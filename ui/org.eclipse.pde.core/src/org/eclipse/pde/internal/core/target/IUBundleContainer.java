/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     EclipseSource, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.touchpoint.eclipse.query.OSGiBundleQuery;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A bundle container that references IU's in one or more repositories.
 * 
 * @since 3.5
 */
public class IUBundleContainer extends AbstractBundleContainer {

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
	 * by the slicer so {@link fIncludeAllRequired} must be turned off for this setting to be used.
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

	/**
	 * IU identifiers.
	 */
	private String[] fIds;

	/**
	 * IU versions
	 */
	private Version[] fVersions;

	/**
	 * Cached IU's referenced by this bundle container, or <code>null</code> if not
	 * resolved.
	 */
	private IInstallableUnit[] fUnits;

	/**
	 * Repositories to consider, or <code>null</code> if default.
	 */
	private URI[] fRepos;

	/**
	 * A set of bitmask flags that indicate how this container gets elements from its
	 * associated p2 repository.
	 */
	private int fFlags;

	/**
	 *  The p2 synchronizer to use in managing this container.
	 */
	private P2TargetUtils fSynchronizer;

	/**
	 * The target definition that this bundle container was last resolved in
	 * or <code>null</code> if not resolved.
	 */
	private ITargetDefinition fTarget;

	private static final boolean DEBUG_PROFILE;

	static {
		DEBUG_PROFILE = PDECore.getDefault().isDebugging() && "true".equals(Platform.getDebugOption("org.eclipse.pde.core/target/profile")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 * 
	 * @param ids IU identifiers
	 * @param versions IU versions
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> for default set
	 * @param resolutionFlags bitmask of flags to control IU resolution, possible flags are {@link IUBundleContainer#INCLUDE_ALL_ENVIRONMENTS}, {@link IUBundleContainer#INCLUDE_REQUIRED}, {@link IUBundleContainer#INCLUDE_SOURCE}, {@link IUBundleContainer#INCLUDE_CONFIGURE_PHASE}
	 */
	IUBundleContainer(String[] ids, String[] versions, URI[] repositories, int resolutionFlags) {
		fIds = ids;
		fFlags = resolutionFlags;
		fVersions = new Version[versions.length];
		for (int i = 0; i < versions.length; i++) {
			fVersions[i] = Version.create(versions[i]);

		}
		if (repositories == null || repositories.length == 0) {
			fRepos = null;
		} else {
			fRepos = repositories;
		}
	}

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 * 
	 * @param units IU's
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> for default set
	 * @param resolutionFlags bitmask of flags to control IU resolution, possible flags are {@link IUBundleContainer#INCLUDE_ALL_ENVIRONMENTS}, {@link IUBundleContainer#INCLUDE_REQUIRED}, {@link IUBundleContainer#INCLUDE_SOURCE}, {@link IUBundleContainer#INCLUDE_CONFIGURE_PHASE}
	 */
	IUBundleContainer(IInstallableUnit[] units, URI[] repositories, int resolutionFlags) {
		fIds = new String[units.length];
		fFlags = resolutionFlags;
		fVersions = new Version[units.length];
		for (int i = 0; i < units.length; i++) {
			fIds[i] = units[i].getId();
			fVersions[i] = units[i].getVersion();
		}
		if (repositories == null || repositories.length == 0) {
			fRepos = null;
		} else {
			fRepos = repositories;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		return P2TargetUtils.BUNDLE_POOL.toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractBundleContainer#resolveFeatures(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		fTarget = definition;
		fSynchronizer.synchronize(definition, monitor);
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
	TargetFeature[] cacheFeatures(ITargetDefinition target) throws CoreException {
		// Ideally we would compute the list of features specific to this container but that 
		// would require running the slicer again to follow the dependencies from this 
		// container's roots.  Instead, here we find all features in the shared profile.  This means
		// that all IU containers will return the same thing for getFeatures.  In practice this is 
		// ok because we remove duplicates in TargetDefinition#getAllFeatures.

		Set features = new HashSet();
		IQueryResult queryResult = fSynchronizer.getProfile().query(QueryUtil.createIUAnyQuery(), null);
		if (queryResult.isEmpty()) {
			return new TargetFeature[0];
		}

		for (Iterator i = queryResult.iterator(); i.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) i.next();
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
			return new TargetFeature[0];
		}

		// Now get features for all known features
		TargetFeature[] allFeatures = ((TargetDefinition) target).resolveFeatures(getLocation(false), new NullProgressMonitor());

		// Build a final set of the models for the features in the profile.
		List result = new ArrayList();
		for (int i = 0; i < allFeatures.length; i++) {
			NameVersionDescriptor candidate = new NameVersionDescriptor(allFeatures[i].getId(), allFeatures[i].getVersion(), NameVersionDescriptor.TYPE_FEATURE);
			if (features.contains(candidate)) {
				result.add(allFeatures[i]);
			}
		}
		fFeatures = (TargetFeature[]) result.toArray(new TargetFeature[result.size()]);
		return fFeatures;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		fTarget = definition;
		fSynchronizer.synchronize(definition, monitor);
		return fBundles;
	}

	/**
	 * Update this container's cache of top level IUs based on the given profile.
	 * NOTE: this method expects the synchronizer to be synchronized and is called
	 * as a result of a synchronization operation.
	 */
	IInstallableUnit[] cacheIUs(ITargetDefinition target) throws CoreException {
		IProfile profile = fSynchronizer.getProfile();
		ArrayList result = new ArrayList();
		for (int i = 0; i < fIds.length; i++) {
			IQuery query = QueryUtil.createIUQuery(fIds[i], fVersions[i]);
			IQueryResult queryResult = profile.query(query, null);
			if (queryResult.isEmpty())
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.IUBundleContainer_1, fIds[i])));
			result.add(queryResult.iterator().next());
		}
		fUnits = (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
		return fUnits;
	}

	/**
	 * Update this container's cache of bundle objects based on the given profile.
	 * NOTE: this method expects the synchronizer to be synchronized and is called
	 * as a result of a synchronization operation.
	 */
	TargetBundle[] cacheBundles(ITargetDefinition target) throws CoreException {
		// slice the profile to find the bundles attributed to this container.
		// Look only for strict dependencies if we are using the slicer.
		// We can always consider all platforms since the profile wouldn't contain it if it was not interesting
		boolean onlyStrict = !fSynchronizer.getIncludeAllRequired();
		IProfile metadata = fSynchronizer.getProfile();
		PermissiveSlicer slicer = new PermissiveSlicer(metadata, new HashMap(), true, false, true, onlyStrict, false);
		IQueryable slice = slicer.slice(fUnits, new NullProgressMonitor());

		if (slicer.getStatus().getSeverity() == IStatus.ERROR) {
			// If the slicer has an error, report it instead of returning an empty set
			throw new CoreException(slicer.getStatus());
		}

		// query for bundles
		IFileArtifactRepository artifacts = null;
		try {
			artifacts = P2TargetUtils.getBundlePool();
		} catch (CoreException e) {
			if (DEBUG_PROFILE) {
				System.out.println("Bundle pool repository could not be loaded"); //$NON-NLS-1$
			}
			return fBundles = null;
		}

		Map bundles = generateResolvedBundles(slice, metadata, artifacts);
		if (bundles.isEmpty()) {
			if (DEBUG_PROFILE) {
				System.out.println("Profile does not contain any bundles or artifacts were missing"); //$NON-NLS-1$
			}
			if (slicer.getStatus().getSeverity() == IStatus.WARNING) {
				// If the slicer has warnings, they probably caused there to be no bundles available
				throw new CoreException(slicer.getStatus());
			}

			return fBundles = null;
		}

		fBundles = (TargetBundle[]) bundles.values().toArray(new TargetBundle[bundles.size()]);
		return fBundles;
	}

	/*
	 * Respond to the notification that the synchronizer associated with this container has changed
	 * This is a callback method used by the synchronizer.
	 * It should NOT be called any other time.
	 */
	void synchronizerChanged(ITargetDefinition target) throws CoreException {
		// cache the IUs first as they are used to slice the profile for the other caches.
		cacheIUs(target);
		cacheBundles(target);
		cacheFeatures(target);
	}

	/**
	 * Update the root IUs to the latest available in the repos associated with this container.
	 * 
	 * @param toUpdate the set of IU ids in this container to consider updating.  If empty
	 * then update everything
	 * @param monitor progress monitor or <code>null</code>
	 * @return whether this container was changed as part of this update and must be resolved 
	 * @exception CoreException if unable to retrieve IU's
	 */
	public synchronized boolean update(Set/*<type>*/toUpdate, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		IQueryable source = P2TargetUtils.getQueryableMetadata(fRepos, progress.newChild(30));
		boolean updated = false;
		SubMonitor loopProgress = progress.newChild(70).setWorkRemaining(fIds.length);
		for (int i = 0; i < fIds.length; i++) {
			if (!toUpdate.isEmpty() && !toUpdate.contains(fIds[i]))
				continue;
			IQuery query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(fIds[i]));
			IQueryResult queryResult = source.query(query, loopProgress.newChild(1));
			Iterator it = queryResult.iterator();
			// bail if the feature is no longer available.
			if (!it.hasNext())
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.IUBundleContainer_1, fIds[i])));
			IInstallableUnit iu = (IInstallableUnit) it.next();
			// if the version is different from the spec (up or down), record the change.
			if (!iu.getVersion().equals(fVersions[i])) {
				updated = true;
				// if the spec was not specific (e.g., 0.0.0) the target def itself has changed.
				if (!fVersions[i].equals(Version.emptyVersion)) {
					fVersions[i] = iu.getVersion();
				}
			}
		}
		if (!updated) {
			// Things have changed so mark the container as unresolved
			clearResolutionStatus();
		}
		return updated;
	}

	protected void clearResolutionStatus() {
		super.clearResolutionStatus();
		fSynchronizer.markDirty();
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
	 * @throws CoreException
	 */
	private Map generateResolvedBundles(IQueryable source, IQueryable metadata, IFileArtifactRepository artifacts) throws CoreException {
		OSGiBundleQuery query = new OSGiBundleQuery();
		IQueryResult queryResult = source.query(query, null);
		Map bundles = new LinkedHashMap();
		for (Iterator i = queryResult.iterator(); i.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) i.next();
			generateBundle(unit, artifacts, bundles);
			if (getIncludeSource()) {
				// bit of a hack using the bundle naming convention for finding source bundles
				// but this matches what we do when adding source to the profile so...
				IQuery sourceQuery = QueryUtil.createIUQuery(unit.getId() + ".source", unit.getVersion()); //$NON-NLS-1$
				IQueryResult result = metadata.query(sourceQuery, null);
				if (!result.isEmpty()) {
					generateBundle((IInstallableUnit) result.iterator().next(), artifacts, bundles);
				}
			}
		}
		return bundles;
	}

	private void generateBundle(IInstallableUnit unit, IFileArtifactRepository repo, Map bundles) throws CoreException {
		Collection artifacts = unit.getArtifacts();
		for (Iterator iterator2 = artifacts.iterator(); iterator2.hasNext();) {
			File file = repo.getArtifactFile((IArtifactKey) iterator2.next());
			if (file != null) {
				TargetBundle bundle = new TargetBundle(file);
				if (bundle != null) {
					bundles.put(bundle.getBundleInfo(), bundle);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof IUBundleContainer) {
			IUBundleContainer iuContainer = (IUBundleContainer) container;
			boolean result = true;
			result &= iuContainer.getIncludeAllRequired() == getIncludeAllRequired();
			result &= iuContainer.getIncludeAllEnvironments() == getIncludeAllEnvironments();
			result &= iuContainer.getIncludeSource() == getIncludeSource();
			result &= iuContainer.getIncludeConfigurePhase() == getIncludeConfigurePhase();
			return result && isEqualOrNull(fIds, iuContainer.fIds) && isEqualOrNull(fVersions, iuContainer.fVersions) && isEqualOrNull(fRepos, iuContainer.fRepos);
		}
		return false;
	}

	/**
	 * Returns whether the arrays have equal contents or are both <code>null</code>.
	 * 
	 * @param objects1
	 * @param objects2
	 * @return whether the arrays have equal contents or are both <code>null</code>
	 */
	private boolean isEqualOrNull(Object[] objects1, Object[] objects2) {
		if (objects1 == null) {
			return objects2 == null;
		}
		if (objects2 == null) {
			return false;
		}
		if (objects1.length == objects2.length) {
			for (int i = 0; i < objects1.length; i++) {
				if (!objects1[i].equals(objects2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the URI's identifying the metadata repositories to consider when resolving
	 * IU's or <code>null</code> if the default set should be used.
	 * 
	 * @return metadata repository URI's or <code>null</code>
	 */
	public URI[] getRepositories() {
		return fRepos;
	}

	/**
	 * Removes an installable unit from this container.  The container will no longer be resolved.
	 *  
	 * @param unit unit to remove from the list of root IUs
	 */
	public synchronized void removeInstallableUnit(IInstallableUnit unit) {
		List newIds = new ArrayList(fIds.length);
		List newVersions = new ArrayList(fIds.length);
		for (int i = 0; i < fIds.length; i++) {
			if (!fIds[i].equals(unit.getId()) || !fVersions[i].equals(unit.getVersion())) {
				newIds.add(fIds[i]);
				newVersions.add(fVersions[i]);
			}
		}
		fIds = (String[]) newIds.toArray(new String[newIds.size()]);
		fVersions = (Version[]) newVersions.toArray(new Version[newVersions.size()]);

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
		if (fSynchronizer == null)
			return (fFlags & INCLUDE_REQUIRED) == INCLUDE_REQUIRED;
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
		if (fSynchronizer == null)
			return (fFlags & INCLUDE_ALL_ENVIRONMENTS) == INCLUDE_ALL_ENVIRONMENTS;
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
		if (fSynchronizer == null)
			return (fFlags & INCLUDE_SOURCE) == INCLUDE_SOURCE;
		return fSynchronizer.getIncludeSource();
	}

	/**
	 * Returns whether or not the configuration phase should be executed while installing the IUS
	 * 
	 * @return whether or not the configuration phase should be executed
	 */
	public boolean getIncludeConfigurePhase() {
		// if this container has not been associated with a container, return its own value
		if (fSynchronizer == null)
			return (fFlags & INCLUDE_CONFIGURE_PHASE) == INCLUDE_CONFIGURE_PHASE;
		return fSynchronizer.getIncludeConfigurePhase();
	}

	/**
	 * Returns the installable units defined by this container
	 * 
	 * @return the discovered IUs
	 * @exception CoreException if unable to retrieve IU's
	 */
	public IInstallableUnit[] getInstallableUnits() throws CoreException {
		if (fUnits == null)
			return new IInstallableUnit[0];
		return fUnits;
	}

	/**
	 * Returns installable unit identifiers.
	 * 
	 * @return IU id's
	 */
	String[] getIds() {
		return fIds;
	}

	/**
	 * Returns installable unit versions.
	 * 
	 * @return IU versions
	 */
	Version[] getVersions() {
		return fVersions;
	}

	/**
	 * Return the synchronizer for this container.  If there isn't one and a target definition is 
	 * supplied, then get/create the one used by the target and the other containers.
	 */
	P2TargetUtils getSynchronizer(ITargetDefinition definition) {
		if (fSynchronizer != null) {
			return fSynchronizer;
		}
		if (definition == null)
			return null;
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
	protected void associateWithTarget(ITargetDefinition target) {
		super.associateWithTarget(target);
		fSynchronizer = getSynchronizer(target);
		// The synchronizer is being made dirty because this IU container is being associated with it
		fSynchronizer.markDirty();
		fSynchronizer.setIncludeAllRequired((fFlags & INCLUDE_REQUIRED) == INCLUDE_REQUIRED);
		fSynchronizer.setIncludeAllEnvironments((fFlags & INCLUDE_ALL_ENVIRONMENTS) == INCLUDE_ALL_ENVIRONMENTS);
		fSynchronizer.setIncludeSource((fFlags & INCLUDE_SOURCE) == INCLUDE_SOURCE);
		fSynchronizer.setIncludeConfigurePhase((fFlags & INCLUDE_CONFIGURE_PHASE) == INCLUDE_CONFIGURE_PHASE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractBundleContainer#serialize()
	 */
	public String serialize() {
		Element containerElement;
		Document document;
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = docBuilder.newDocument();
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
		String[] ids = getIds();
		Version[] versions = getVersions();
		for (int i = 0; i < ids.length; i++) {
			Element unit = document.createElement(TargetDefinitionPersistenceHelper.INSTALLABLE_UNIT);
			unit.setAttribute(TargetDefinitionPersistenceHelper.ATTR_ID, ids[i]);
			unit.setAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION, versions[i].toString());
			containerElement.appendChild(unit);
		}
		URI[] repositories = getRepositories();
		if (repositories != null) {
			for (int i = 0; i < repositories.length; i++) {
				Element repo = document.createElement(TargetDefinitionPersistenceHelper.REPOSITORY);
				repo.setAttribute(TargetDefinitionPersistenceHelper.LOCATION, repositories[i].toASCIIString());
				containerElement.appendChild(repo);
			}
		}

		try {
			document.appendChild(containerElement);
			StreamResult result = new StreamResult(new StringWriter());
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
			transformer.transform(new DOMSource(document), result);
			return result.getWriter().toString();
		} catch (TransformerException ex) {
			return null;
		}
	}
}
