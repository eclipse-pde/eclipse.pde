/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     EclipseSource, Inc. - ongoing development
 *     Manumitting Technologies Inc - bug 437726: wrong error messages opening target definition
 *     Alexander Fedorov (ArSysOp) - Bug 542425, Bug 574629
 *     Christoph Läubrich - Bug 568865 - [target] add advanced editing capabilities for custom target platforms
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.touchpoint.eclipse.query.OSGiBundleQuery;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A bundle container that references IU's in one or more repositories.
 *
 * @since 3.5
 */
public class IUBundleContainer extends AbstractBundleContainer {

	record UnitDescription(String id, VersionRange version) {

		static UnitDescription parse(String id, String version) {
			VersionRange range;
			if (Version.emptyVersion.toString().equals(version)) {
				range = VersionRange.emptyRange;
			} else if (version.contains(",")) { //$NON-NLS-1$
				range = VersionRange.create(version);
			} else {
				range = VersionRange.create('[' + version + ',' + version + ']');
			}
			return new UnitDescription(id, range);
		}

		static UnitDescription create(String id, Version version) {
			VersionRange range = new VersionRange(version, true, version, true);
			return new UnitDescription(id, range);
		}

		UnitDescription {
			Objects.requireNonNull(id);
			Objects.requireNonNull(version);
		}

		private boolean isSingleVersion() {
			// TODO: also check bounds? if not both open, then it would be empty
			// anyways...
			return version.equals(VersionRange.emptyRange) || version.getMaximum().equals(version.getMinimum());
		}

		Optional<Version> getSingleVersion() {
			return isSingleVersion() ? Optional.of(version.getMinimum()) : Optional.empty();
		}

		IQuery<IInstallableUnit> createIUQuery() {
			return isSingleVersion() //
					? QueryUtil.createIUQuery(id, version.getMinimum())
					: QueryUtil.createIUQuery(id, version);
		}

		String versionString() {
			return getSingleVersion().map(Version::toString).orElseGet(version()::toString);
		}

		@Override
		public String toString() {
			return VersionRange.emptyRange.equals(version) ? id : id + '/' + versionString();
		}
	}

	/**
	 * Constant describing the type of bundle container
	 */
	public static final String TYPE = "InstallableUnit"; //$NON-NLS-1$

	/**
	 * Constant for the string that is appended to feature installable unit ids
	 */
	private static final String FEATURE_ID_SUFFIX = ".feature.group"; //$NON-NLS-1$

	/**
	 * Whether this container must have all required IUs of the selected IUs available and included
	 * in the target to resolve successfully.  If this option is true, the planner will be used to resolve
	 * otherwise the slicer is used.  The planner can describe any missing requirements as errors.
	 */
	public static final int INCLUDE_REQUIRED = 1 << 0;

	/**
	 * Whether this container should download and include environment (platform) specific units for all
	 * available platforms (vs only the current target definition's environment settings).  Only supported
	 * by the slicer so {@link #INCLUDE_REQUIRED} must be turned off for this setting to be used.
	 */
	public static final int INCLUDE_ALL_ENVIRONMENTS = 1 << 1;

	/**
	 * Whether this container should download and include source bundles for the selected units if the associated
	 * source is available in the repository.
	 */
	public static final int INCLUDE_SOURCE = 1 << 2;

	/**
	 * Whether this container should execute the configure phase after fetching the selected units
	 */
	public static final int INCLUDE_CONFIGURE_PHASE = 1 << 3;

	/** The list of id+version of all root units declared in this container. */
	private List<UnitDescription> fIUs;

	/**
	 * Cached IU's referenced by this bundle container, or <code>null</code> if not
	 * resolved.
	 */
	private List<IInstallableUnit> fUnits = List.of();

	/**
	 * Repositories to consider, or <code>null</code> if default.
	 */
	private final List<URI> fRepos;

	/**
	 * A set of bitmask flags that indicate how this container gets elements from its
	 * associated p2 repository.
	 */
	private final int fFlags;

	/**
	 *  The p2 synchronizer to use in managing this container.
	 */
	private P2TargetUtils fSynchronizer;

	/**
	 * The target definition that this bundle container was last resolved in
	 * or <code>null</code> if not resolved.
	 */
	private ITargetDefinition fTarget;

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 *
	 * @param units the units composed of their identifier and version
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> for default set
	 * @param resolutionFlags bitmask of flags to control IU resolution, possible flags are {@link IUBundleContainer#INCLUDE_ALL_ENVIRONMENTS}, {@link IUBundleContainer#INCLUDE_REQUIRED}, {@link IUBundleContainer#INCLUDE_SOURCE}, {@link IUBundleContainer#INCLUDE_CONFIGURE_PHASE}
	 */
	IUBundleContainer(Collection<UnitDescription> units, List<URI> repositories, int resolutionFlags) {
		fIUs = new ArrayList<>(units);
		fFlags = resolutionFlags;
		fRepos = List.copyOf(repositories);
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		return P2TargetUtils.BUNDLE_POOL.toOSString();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		fTarget = definition;

		if (!isResolved()) {
			fSynchronizer.synchronize(definition, monitor);
		}
		return fFeatures;
	}

	/**
	 * Returns the target definition that was used to resolve this bundle container or <code>null</code>
	 * if this container is not resolved.
	 *
	 * @return target definition or <code>null</code>
	 */
	public ITargetDefinition getTarget() {
		return isResolved() ? fTarget : null;
	}

	/**
	 * Update this container's cache of feature objects based on the given profile.
	 * NOTE: this method expects the synchronizer to be synchronized and is called
	 * as a result of a synchronization operation.
	 */
	private void cacheFeatures(ITargetDefinition target) throws CoreException {
		// Ideally we would compute the list of features specific to this container but that
		// would require running the slicer again to follow the dependencies from this
		// container's roots.  Instead, here we find all features in the shared profile.  This means
		// that all IU containers will return the same thing for getFeatures.  In practice this is
		// ok because we remove duplicates in TargetDefinition#getAllFeatures.

		Set<NameVersionDescriptor> features = new HashSet<>();
		IQueryResult<IInstallableUnit> queryResult = fSynchronizer.getProfile().query(QueryUtil.createIUAnyQuery(), null);
		if (queryResult.isEmpty()) {
			return;
		}

		for (IInstallableUnit unit : queryResult) {
			String id = unit.getId();
			// if the IU naming convention says it is a feature, then add it.
			// This is less than optimal but there is no clear way of identifying an IU as a feature.
			if (id.endsWith(FEATURE_ID_SUFFIX)) {
				id = id.substring(0, id.length() - FEATURE_ID_SUFFIX.length());
				String version = unit.getVersion().toString();
				features.add(new NameVersionDescriptor(id, version, NameVersionDescriptor.TYPE_FEATURE));
			}
		}
		if (features.isEmpty()) {
			return;
		}

		// Now get features for all known features
		TargetFeature[] allFeatures = ((TargetDefinition) target).resolveFeatures(getLocation(false), new NullProgressMonitor());

		// Build a final set of the models for the features in the profile.
		List<TargetFeature> result = new ArrayList<>();
		for (TargetFeature allFeature : allFeatures) {
			NameVersionDescriptor candidate = new NameVersionDescriptor(allFeature.getId(), allFeature.getVersion(), NameVersionDescriptor.TYPE_FEATURE);
			if (features.contains(candidate)) {
				result.add(allFeature);
			}
		}
		fFeatures = result.toArray(TargetFeature[]::new);
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		fTarget = definition;
		if (!isResolved()) {
			fSynchronizer.synchronize(definition, monitor);
		}
		return fBundles;
	}

	/**
	 * Update this container's cache of top level IUs based on the given profile.
	 * NOTE: this method expects the synchronizer to be synchronized and is called
	 * as a result of a synchronization operation.
	 */
	private void cacheIUs() throws CoreException {
		IProfile profile = fSynchronizer.getProfile();
		List<IInstallableUnit> result = new ArrayList<>();
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.IUBundleContainer_ProblemsLoadingRepositories, null);
		for (UnitDescription unit : fIUs) {
			IQuery<IInstallableUnit> query = unit.createIUQuery();
			addElement(profile, query, unit, result, status);
		}
		if (!status.isOK()) {
			fResolutionStatus = status;
			throw new CoreException(status);
		}
		fUnits = List.copyOf(result);
	}

	private void addElement(IQueryable<IInstallableUnit> queryable, IQuery<IInstallableUnit> query,
			UnitDescription iu, List<IInstallableUnit> result, MultiStatus status) {
		Optional<IInstallableUnit> queryResult = queryFirst(queryable, query, null);
		if (queryResult.isEmpty()) {
			status.add(Status.error(NLS.bind(Messages.IUBundleContainer_1, iu)));
		} else {
			result.add(queryResult.get());
		}
	}

	private static <T> Optional<T> queryFirst(IQueryable<T> queryable, IQuery<T> query, IProgressMonitor monitor) {
		// FIXME: check how efficient this is (or can be done more efficient and
		// adjust P2 impl accordingly)
		IQueryResult<T> queryResult = queryable.query(query, monitor);
		return queryResult.stream().findFirst();
	}

	/**
	 * Update this container's cache of bundle objects based on the given profile.
	 * NOTE: this method expects the synchronizer to be synchronized and is called
	 * as a result of a synchronization operation.
	 */
	private void cacheBundles(ITargetDefinition target) throws CoreException {
		// slice the profile to find the bundles attributed to this container.
		// Look only for strict dependencies if we are using the slicer.
		// We can always consider all platforms since the profile wouldn't contain it if it was not interesting
		boolean onlyStrict = !fSynchronizer.getIncludeAllRequired();
		IProfile metadata = fSynchronizer.getProfile();
		PermissiveSlicer slicer = new PermissiveSlicer(metadata, new HashMap<>(), true, false, true, onlyStrict, false);
		IQueryable<IInstallableUnit> slice = slicer.slice(fUnits, new NullProgressMonitor());

		if (slicer.getStatus().getSeverity() == IStatus.ERROR) {
			// If the slicer has an error, report it instead of returning an empty set
			throw new CoreException(slicer.getStatus());
		}

		// query for bundles
		IFileArtifactRepository artifacts = null;
		try {
			if (P2TargetUtils.fgTargetArtifactRepo.containsKey(target)) {
				artifacts = P2TargetUtils.fgTargetArtifactRepo.get(target);
			} else {
				artifacts = P2TargetUtils.getBundlePool();
				P2TargetUtils.fgTargetArtifactRepo.put(target, artifacts);
			}
		} catch (CoreException e) {
			if (PDECore.DEBUG_TARGET_PROFILE) {
				System.out.println("Bundle pool repository could not be loaded"); //$NON-NLS-1$
			}
			fBundles = null;
			return;
		}

		Map<BundleInfo, TargetBundle> bundles = generateResolvedBundles(slice, metadata, artifacts);
		if (bundles.isEmpty()) {
			if (PDECore.DEBUG_TARGET_PROFILE) {
				System.out.println("Profile does not contain any bundles or artifacts were missing"); //$NON-NLS-1$
			}
			if (slicer.getStatus().getSeverity() == IStatus.WARNING) {
				// If the slicer has warnings, they probably caused there to be no bundles available
				throw new CoreException(slicer.getStatus());
			}
			fBundles = null;
			return;
		}
		fBundles = bundles.values().toArray(TargetBundle[]::new);
	}

	/*
	 * Respond to the notification that the synchronizer associated with this container has changed
	 * This is a callback method used by the synchronizer.
	 * It should NOT be called any other time.
	 */
	void synchronizerChanged(ITargetDefinition target) {
		try {
			// cache the IUs first as they are used to slice the profile for the other caches.
			cacheIUs();
			cacheBundles(target);
			cacheFeatures(target);
		} catch (CoreException e) {
			PDECore.logException(e, e.getMessage());
			fBundles = new TargetBundle[0];
			fFeatures = new TargetFeature[0];
			fResolutionStatus = e.getStatus();
		}
	}

	/**
	 * Update the root IUs to the latest available in the repos associated with
	 * this container.
	 *
	 * @param toUpdate
	 *            the set of IU ids in this container to consider updating. If
	 *            empty then update everything
	 * @param monitor
	 *            progress monitor or <code>null</code>
	 * @return a new instance with updated versions or <code>null</code> if
	 *         nothing was changed as result of the update
	 * @exception CoreException
	 *                if unable to retrieve IU's
	 */
	public synchronized IUBundleContainer update(Set<String> toUpdate, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		IQueryable<IInstallableUnit> source = P2TargetUtils.getQueryableMetadata(fRepos, progress.split(30));
		boolean updated = false;
		List<UnitDescription> updatedUnits = new ArrayList<>(fIUs);
		SubMonitor loopProgress = progress.split(70).setWorkRemaining(updatedUnits.size());
		for (int i = 0; i < updatedUnits.size(); i++) {
			UnitDescription unit = updatedUnits.get(i);
			if (!toUpdate.isEmpty() && !toUpdate.contains(unit.id())) {
				continue;
			}
			// TODO: why is createLatestQuery() used here but not in the other
			// caller ?
			IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(unit.id()));
			Optional<IInstallableUnit> queryResult = queryFirst(source, query, loopProgress.split(1));
			Version updatedVersion = queryResult.map(IInstallableUnit::getVersion)
					// bail if the feature is no longer available.
					.orElseThrow(() -> new CoreException(Status.error(NLS.bind(Messages.IUBundleContainer_1, unit))));
			// if the version is different from the spec (up or down), record the change.
			// FIXME: check the new logic here again?!
			Optional<Version> singleVersion = unit.getSingleVersion();

			if (singleVersion.isPresent() && !updatedVersion.equals(singleVersion.get())) {
				updated = true;
				// if the spec was not specific (e.g., 0.0.0) the target def itself has changed.
				if (!singleVersion.get().equals(Version.emptyVersion)) {
					updatedUnits.set(i, UnitDescription.create(unit.id(), updatedVersion));
				}
			} else {
				// TODO: how to update version ranges properly. There are
				// different strategies possible.
				// E.g. compute its 'span' and apply it to the updated version
				// as lower bound
				throw new UnsupportedOperationException();
			}
		}
		if (updated) {
			return new IUBundleContainer(updatedUnits, fRepos, fFlags);
		}
		return null;
	}

	/**
	 * Collects all available installable units from the given source that represent OSGI
	 * bundles.  A IResolvedBundle is created for each and a map containing all results
	 * mapping BundleInfo to IResolvedBundle is returned.
	 * <p>
	 * If there is an artifact missing for a unit it will be ignored (not added to the returned map).
	 * If this container is setup to automatically include source, an corresponding source bundles
	 * found in the given profile will also be added as resolved bundles.
	 * </p>
	 * @param source the bundle units to be converted
	 * @param metadata the metadata backing the conversion
	 * @param artifacts the underlying artifact repo against which the bundles are validated
	 * @return map of BundleInfo to IResolvedBundle
	 */
	private Map<BundleInfo, TargetBundle> generateResolvedBundles(IQueryable<IInstallableUnit> source, IQueryable<IInstallableUnit> metadata, IFileArtifactRepository artifacts) throws CoreException {
		OSGiBundleQuery query = new OSGiBundleQuery();
		IQueryResult<IInstallableUnit> queryResult = source.query(query, null);
		Map<BundleInfo, TargetBundle> bundles = new LinkedHashMap<>();
		for (IInstallableUnit unit : queryResult) {
			generateBundle(unit, artifacts, bundles);
			if (getIncludeSource()) {
				// bit of a hack using the bundle naming convention for finding source bundles
				// but this matches what we do when adding source to the profile so...
				IQuery<IInstallableUnit> sourceQuery = QueryUtil.createIUQuery(unit.getId() + ".source", unit.getVersion()); //$NON-NLS-1$
				Optional<IInstallableUnit> resultingUnit = queryFirst(metadata, sourceQuery, null);
				if (resultingUnit.isPresent()) {
					generateBundle(resultingUnit.get(), artifacts, bundles);
				}
			}
		}
		return bundles;
	}

	private void generateBundle(IInstallableUnit unit, IFileArtifactRepository repo, Map<BundleInfo, TargetBundle> bundles) throws CoreException {
		Collection<IArtifactKey> artifacts = unit.getArtifacts();
		for (IArtifactKey artifactKey : artifacts) {
			File file = null;

			Map<IFileArtifactRepository, File> mapRepoFile = P2TargetUtils.fgArtifactKeyRepoFile.get(artifactKey);
			if (mapRepoFile != null){
				file = mapRepoFile.get(repo);
			}

			if( file == null){
				file = repo.getArtifactFile(artifactKey);
				if (file != null) {
					Map<IFileArtifactRepository, File> repoFile = new ConcurrentHashMap<>();
					repoFile.put(repo, file);
					P2TargetUtils.fgArtifactKeyRepoFile.putIfAbsent(artifactKey, repoFile);
				}
			}
			if (file != null) {
				TargetBundle bundle = new TargetBundle(file);
				bundles.put(bundle.getBundleInfo(), bundle);
			}
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(getIncludeAllRequired(), getIncludeAllEnvironments(), getIncludeSource(),
				getIncludeConfigurePhase(), fIUs, fRepos);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof IUBundleContainer other //
				&& getIncludeAllRequired() == other.getIncludeAllRequired()
				&& getIncludeAllEnvironments() == other.getIncludeAllEnvironments()
				&& getIncludeSource() == other.getIncludeSource()
				&& getIncludeConfigurePhase() == other.getIncludeConfigurePhase() //
				&& fIUs.equals(other.fIUs) //
				&& fRepos.equals(other.fRepos);
	}

	/**
	 * Returns the URI's identifying the metadata repositories to consider when resolving
	 * IU's or <code>null</code> if the default set should be used.
	 *
	 * @return metadata repository URI's or <code>null</code>
	 */
	public List<URI> getRepositories() {
		return fRepos;
	}

	/**
	 * Removes an installable unit from this container.  The container will no longer be resolved.
	 *
	 * @param unit unit to remove from the list of root IUs
	 */
	public synchronized void removeInstallableUnit(IInstallableUnit unit) {
		// TODO: TODO: maybe the units list can be converted to a map and map
		// each unit to the declaring IU (if its not a dependency). Then we get
		// an exact matched version for the range. Also consider this for the
		// update method.
		Optional<UnitDescription> toRemove = fIUs.stream()
				.filter(iu -> iu.id().equals(unit.getId()) && iu.version().isIncluded(unit.getVersion())).findFirst();
		toRemove.ifPresent(fIUs::remove);
		// fIUs.remove(new VersionedId(unit.getId(), unit.getVersion()));
		// Need to mark the container as unresolved
		clearResolutionStatus();
	}

	/**
	 * Returns whether all required units must be available to resolve this container.  When <code>true</code>
	 * the resolve operation will use the planner to determine the complete set of IUs required to
	 * make the selected IUs runnable.  If any dependencies are missing, the resolve operation will return an
	 * error explaining what problems exist.  When <code>false</code> the resolve operation will use the slicer
	 * to determine what units to include.  Any required units that are not available in the repositories will
	 * be ignored.
	 *
	 * @return whether all required units must be available to resolve this container
	 */
	public boolean getIncludeAllRequired() {
		// if this container has not been associated with a container, return its own value
		if (fSynchronizer == null) {
			return (fFlags & INCLUDE_REQUIRED) == INCLUDE_REQUIRED;
		}
		return fSynchronizer.getIncludeAllRequired();
	}

	/**
	 * Returns whether all environment (platform) specific installable units should
	 * be included in this container when it is resolved.  This feature is not supported
	 * by the planner so will only have an effect if the include all required setting
	 * is turned off ({@link #getIncludeAllRequired()}).
	 *
	 * @return whether environment specific units should be included
	 */
	public boolean getIncludeAllEnvironments() {
		// if this container has not been associated with a container, return its own value
		if (fSynchronizer == null) {
			return (fFlags & INCLUDE_ALL_ENVIRONMENTS) == INCLUDE_ALL_ENVIRONMENTS;
		}
		return fSynchronizer.getIncludeAllEnvironments();
	}

	/**
	 * Returns whether or not source bundles corresponding to selected binary bundles
	 * are automatically included in the target.
	 *
	 * @return whether or not source is included automatically
	 */
	public boolean getIncludeSource() {
		// if this container has not been associated with a container, return its own value
		if (fSynchronizer == null) {
			return (fFlags & INCLUDE_SOURCE) == INCLUDE_SOURCE;
		}
		return fSynchronizer.getIncludeSource();
	}

	/**
	 * Returns whether or not the configuration phase should be executed while installing the IUS
	 *
	 * @return whether or not the configuration phase should be executed
	 */
	public boolean getIncludeConfigurePhase() {
		// if this container has not been associated with a container, return its own value
		if (fSynchronizer == null) {
			return (fFlags & INCLUDE_CONFIGURE_PHASE) == INCLUDE_CONFIGURE_PHASE;
		}
		return fSynchronizer.getIncludeConfigurePhase();
	}

	/**
	 * Returns the installable units defined by this container
	 *
	 * @return the discovered IUs
	 * @exception CoreException if unable to retrieve IU's
	 */
	public List<IInstallableUnit> getInstallableUnits() throws CoreException {
		return fUnits;
	}

	/** Returns installable unit identifiers and versions. */
	List<UnitDescription> getUnits() {
		return Collections.unmodifiableList(fIUs);
	}

	/**
	 * Return the synchronizer for this container.  If there isn't one and a target definition is
	 * supplied, then get/create the one used by the target and the other containers.
	 */
	P2TargetUtils getSynchronizer(ITargetDefinition definition) {
		if (fSynchronizer != null) {
			return fSynchronizer;
		}
		if (definition == null) {
			return null;
		}
		return fSynchronizer = P2TargetUtils.getSynchronizer(definition);
	}

	/**
	 * Callback method used by the synchronizer to associate containers with
	 * synchronizers.
	 */
	void setSynchronizer(P2TargetUtils value) {
		fSynchronizer = value;
	}

	/**
	 * Associate this container with the given target.  The include settings for this container
	 * override the settings for all other IU containers in the target.  Last one wins.
	 */
	@Override
	protected void associateWithTarget(ITargetDefinition target) {
		super.associateWithTarget(target);
		fSynchronizer = getSynchronizer(target);
		fSynchronizer.setIncludeAllRequired((fFlags & INCLUDE_REQUIRED) == INCLUDE_REQUIRED);
		fSynchronizer.setIncludeAllEnvironments((fFlags & INCLUDE_ALL_ENVIRONMENTS) == INCLUDE_ALL_ENVIRONMENTS);
		fSynchronizer.setIncludeSource((fFlags & INCLUDE_SOURCE) == INCLUDE_SOURCE);
		fSynchronizer.setIncludeConfigurePhase((fFlags & INCLUDE_CONFIGURE_PHASE) == INCLUDE_CONFIGURE_PHASE);
	}

	private static final Comparator<UnitDescription> ID_FIRST_VERSION_SECOND = Comparator //
			.comparing(UnitDescription::id) //
			.thenComparing(unit -> {
				Optional<Version> singleVersion = unit.getSingleVersion();
				if (singleVersion.isEmpty()) {
					return singleVersion.get();
				} else   {
					// TODO: better solution? E.g. prefer version over ranges?
					return unit.version().getMinimum();
				}
			});

	@Override
	public String serialize() {
		Element containerElement;
		Document document;
		try {
			@SuppressWarnings("restriction")
			Document d = org.eclipse.core.internal.runtime.XmlProcessorFactory.newDocumentWithErrorOnDOCTYPE();
			document = d;
		} catch (Exception e) {
			PDECore.log(e);
			return null;
		}

		containerElement = document.createElement(TargetDefinitionPersistenceHelper.LOCATION);
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE, getType());
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_MODE, getIncludeAllRequired() ? TargetDefinitionPersistenceHelper.MODE_PLANNER : TargetDefinitionPersistenceHelper.MODE_SLICER);
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_ALL_PLATFORMS, Boolean.toString(getIncludeAllEnvironments()));
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_SOURCE, Boolean.toString(getIncludeSource()));
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_CONFIGURE_PHASE, Boolean.toString(getIncludeConfigurePhase()));
		for (URI repository : fRepos) {
			Element repo = document.createElement(TargetDefinitionPersistenceHelper.REPOSITORY);
			repo.setAttribute(TargetDefinitionPersistenceHelper.LOCATION, repository.toASCIIString());
			containerElement.appendChild(repo);
		}
		// Generate a predictable order of the elements
		fIUs.stream().sorted(ID_FIRST_VERSION_SECOND).forEach(iu -> {
			Element unit = document.createElement(TargetDefinitionPersistenceHelper.INSTALLABLE_UNIT);
			unit.setAttribute(TargetDefinitionPersistenceHelper.ATTR_ID, iu.id());
			unit.setAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION, iu.versionString());
			containerElement.appendChild(unit);
		});
		try {
			document.appendChild(containerElement);
			StreamResult result = new StreamResult(new StringWriter());
			@SuppressWarnings("restriction")
			Transformer transformer = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createTransformerFactoryWithErrorOnDOCTYPE().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
			transformer.transform(new DOMSource(document), result);
			return result.getWriter().toString();
		} catch (TransformerException ex) {
			return null;
		}
	}


	IInstallableUnit[] getRootIUs(IProgressMonitor monitor) throws CoreException {
		IQueryable<IInstallableUnit> repos = P2TargetUtils.getQueryableMetadata(getRepositories(), monitor);
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.IUBundleContainer_ProblemsLoadingRepositories, null);
		List<IInstallableUnit> result = new ArrayList<>();
		for (UnitDescription iu : fIUs) {
			// For versions such as 0.0.0, the IU query may return multiple IUs, so we check which is the latest version
			IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(iu.createIUQuery());
			addElement(repos, query, iu, result, status);
		}
		if (!status.isOK()) {
			fResolutionStatus = status;
			throw new CoreException(status);
		}
		return result.toArray(new IInstallableUnit[0]);
	}

	// // FIXME: check discrepancy?! Is this even necessary?! We have a
	// // specific version why the latest? In case the version is 0.0.0! But
	// // isn't that also interesting fpr the other caller?!
	// IQuery<IInstallableUnit> query = QueryUtil.createLatestQuery(iuQuery);

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.isInstance(fSynchronizer)) {
			return adapter.cast(fSynchronizer);
		}
		return super.getAdapter(adapter);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + fRepos;
	}

}
