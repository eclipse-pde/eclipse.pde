/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.engine.ProfileMetadataRepository;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.touchpoint.eclipse.query.OSGiBundleQuery;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.provisional.*;

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
	 * Cached id/version pairs listing the features that were downloaded to the bundle pool during resolution.  <code>null</code> if not resolved.
	 */
	private NameVersionDescriptor[] fFeatures;

	/**
	 * Repositories to consider, or <code>null</code> if default.
	 */
	private URI[] fRepos;

	/**
	 * Whether this container must have all required IUs of the selected IUs available and included
	 * in the target to resolve successfully.  If this option is true, the planner will be used to resolve
	 * otherwise the slicer is used.  The planner can describe any missing requirements as errors.
	 * <p>
	 * <code>true</code> by default
	 * </p>
	 */
	private boolean fIncludeAllRequired = true;

	/**
	 * Whether this container should download and include environment (platform) specific units for all
	 * available platforms (vs only the current target definition's environment settings).  Only supported 
	 * by the slicer so {@link fIncludeAllRequired} must be turned off for this setting to be used.
	 * <p>
	 * <code>false</code> by default
	 * </p>
	 */
	private boolean fIncludeMultipleEnvironments = false;

	/**
	 * Whether this container should download and include source bundles for the selected units if the associated
	 * source is available in the repository.
	 * <p>
	 * <code>false</code>by default
	 */
	private boolean fIncludeSource = false;

	/**
	 * Constant ID for a root installable unit that is installed into the profile if {@link #fIncludeSource} is set
	 * to <code>true</code>.  The source units found in the repository will be set as required IUs on the root unit.
	 */
	private static final String SOURCE_IU_ID = "org.eclipse.pde.core.target.source.bundles"; //$NON-NLS-1$

	private static final boolean DEBUG_PROFILE;

	static {
		DEBUG_PROFILE = PDECore.getDefault().isDebugging() && "true".equals(Platform.getDebugOption("org.eclipse.pde.core/target/profile")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 * 
	 * @param ids IU identifiers
	 * @param versions IU versions
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> if
	 *   default set
	 */
	IUBundleContainer(String[] ids, String[] versions, URI[] repositories) {
		fIds = ids;
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
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> if
	 *   default set
	 */
	IUBundleContainer(IInstallableUnit[] units, URI[] repositories) {
		fUnits = units;
		fIds = new String[units.length];
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
	 * @see org.eclipse.pde.internal.core.target.AbstractBundleContainer#resolveFeatures(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IFeatureModel[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		if (fFeatures == null || fFeatures.length == 0 || !(definition instanceof TargetDefinition)) {
			return new IFeatureModel[0];
		}

		// Note: By creating a map of the container features, we are limiting the user to only one version of a feature in this container

		// Get all the features in the bundle pool
		IFeatureModel[] allFeatures = ((TargetDefinition) definition).getFeatureModels(getLocation(false), monitor);

		// Create a map of the container features for quick lookups
		HashMap containerFeatures = new HashMap();
		for (int i = 0; i < fFeatures.length; i++) {
			containerFeatures.put(fFeatures[i].getId(), fFeatures[i]);
		}

		List includedFeatures = new ArrayList();
		for (int i = 0; i < allFeatures.length; i++) {
			NameVersionDescriptor candidate = (NameVersionDescriptor) containerFeatures.get(allFeatures[i].getFeature().getId());
			if (candidate != null) {
				if (candidate.getVersion().equals(allFeatures[i].getFeature().getVersion())) {
					includedFeatures.add(allFeatures[i]);
				}
			}
		}
		return (IFeatureModel[]) includedFeatures.toArray(new IFeatureModel[includedFeatures.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		// Resolving may change the included features, clear the cached values
		fFeatures = null;

		SubMonitor subMon = SubMonitor.convert(monitor, 100);

		// Attempt to restore from the profile first, as it is local and faster
		IResolvedBundle[] result = resolveWithProfile(definition, subMon.newChild(25));
		if (result != null) {
			subMon.done();
			return result;
		}

		// Unable to load from profile, resolve normally
		try {
			if (fIncludeAllRequired) {
				result = resolveWithPlanner(definition, subMon.newChild(75));
			} else {
				result = resolveWithSlicer(definition, subMon.newChild(75));
			}
			// If there is a problem generating the profile, delete it so it doesn't get used by #resolveWithProfile()
			if (result == null || result.length == 0 || subMon.isCanceled()) {
				AbstractTargetHandle handle = ((AbstractTargetHandle) definition.getHandle());
				P2TargetUtils.deleteProfile(handle);
			}
			return result;
		} catch (CoreException e) {
			AbstractTargetHandle handle = ((AbstractTargetHandle) definition.getHandle());
			P2TargetUtils.deleteProfile(handle);
			throw e;
		}

	}

	/**
	 * Used to resolve the contents of this container if the container has been resolved and saved to a profile file.  If the
	 * profile contains the correct bundles, there is no need to do a full resolve.  If this method has a problem (missing
	 * file, unable to compute all dependent bundles), this method will return <code>null</code> and the caller should 
	 * use {@link #resolveWithPlanner(ITargetDefinition, IProgressMonitor)} or {@link #resolveWithSlicer(ITargetDefinition, IProgressMonitor)} 
	 * to do a full resolve.
	 * 
	 * @param definition definition being resolved
	 * @param monitor for reporting progress
	 * @return set of bundles included in this container or <code>null</code> if the profile is out of date
	 * @throws CoreException if an unexpected problem occurs trying to read from the profile
	 */
	private IResolvedBundle[] resolveWithProfile(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		if (DEBUG_PROFILE) {
			System.out.println("Target resolution using profile (" + definition.getName() + ")"); //$NON-NLS-1$//$NON-NLS-2$
		}

		IProfile profile = P2TargetUtils.getProfile(definition);
		if (profile == null) {
			if (DEBUG_PROFILE) {
				System.out.println("No profile found"); //$NON-NLS-1$
			}
			return null;
		}

		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.IUBundleContainer_LoadingFromProfileJob, 20);

		// Always create our own units because the slicer will return existing units even though the profile is empty
		if (DEBUG_PROFILE) {
			System.out.print("Required Units: "); //$NON-NLS-1$
			for (int i = 0; i < fIds.length; i++) {
				System.out.print(fIds[i] + ", "); //$NON-NLS-1$
			}
			System.out.println();
		}

		fUnits = new IInstallableUnit[fIds.length];
		for (int i = 0; i < fIds.length; i++) {
			IQuery query = QueryUtil.createIUQuery(fIds[i], fVersions[i]);
			IQueryResult queryResult = profile.query(query, null);
			if (queryResult.isEmpty()) {
				if (DEBUG_PROFILE) {
					System.out.println("Required unit not found in profile: " + fIds[i]); //$NON-NLS-1$
				}
				fUnits = null;
				return null;
			}
			fUnits[i] = (IInstallableUnit) queryResult.iterator().next();
		}
		// check that the include source flag matches what the profile represents
		if (fIncludeSource != (getCurrentSourceIU(profile) != null))
			return null;

		// OK.  Everything looks good.  Now gather the features and bundles and be done.
		ResolvedBundle[] resolvedBundles = updateAfterResolve(profile, definition);

		subMonitor.worked(10);
		subMonitor.done();
		if (DEBUG_PROFILE) {
			if (resolvedBundles == null || resolvedBundles.length == 0)
				System.out.println("No bundles loaded from profile"); //$NON-NLS-1$
			else
				System.out.println("Loading from profile successful. " + resolvedBundles.length + " bundles found"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println();
		}
		return resolvedBundles;
	}

	/**
	 * Used to resolve the contents of this container if the user is including all required software.  The p2 planner is used
	 * to determine the complete set of IUs required to run the selected software.  If all requirements are met, the bundles
	 * are downloaded from the repository into the bundle pool and added to the target definition.
	 * 
	 * @param definition definition being resolved
	 * @param monitor for reporting progress
	 * @return set of bundles included in this container
	 * @throws CoreException if there is a problem with the requirements or there is a problem downloading
	 */
	private IResolvedBundle[] resolveWithPlanner(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(Messages.IUBundleContainer_0, 200);

		// retrieve profile
		IProfile profile = P2TargetUtils.getProfile(definition);
		profile.getTimestamp();
		subMonitor.worked(10);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// resolve IUs
		IInstallableUnit[] units = getInstallableUnits(profile);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// create the provisioning plan
		IPlanner planner = P2TargetUtils.getPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		// first remove everything that was explicitly installed.  Then add it back.  This has the net effect of 
		// removing everything that is no longer needed.
		computeRemovals(profile, request, units);
		request.addAll(Arrays.asList(units));
		for (int i = 0; i < units.length; i++) {
			IInstallableUnit unit = units[i];
			request.setInstallableUnitProfileProperty(unit, P2TargetUtils.PROP_INSTALLED_IU, Boolean.toString(true));
		}

		ProvisioningContext context = new ProvisioningContext(P2TargetUtils.getAgent());
		context.setMetadataRepositories(resolveRepositories());
		context.setArtifactRepositories(resolveArtifactRepositories());

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new SubProgressMonitor(subMonitor, 20));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		IProvisioningPlan installerPlan = plan.getInstallerPlan();
		if (installerPlan != null) {
			// this plan requires an update to the installer first, log the fact and attempt
			// to continue, we don't want to update the running SDK while provisioning a target
			PDECore.log(new Status(IStatus.INFO, PDECore.PLUGIN_ID, Messages.IUBundleContainer_6));
		}
		subMonitor.worked(10);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// execute the provisioning plan
		IPhaseSet phases = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE});
		IEngine engine = P2TargetUtils.getEngine();
		plan.setProfileProperty(P2TargetUtils.PROP_PROVISION_MODE, TargetDefinitionPersistenceHelper.MODE_PLANNER);
		plan.setProfileProperty(P2TargetUtils.PROP_ALL_ENVIRONMENTS, Boolean.toString(false));
		IStatus result = engine.perform(plan, phases, new SubProgressMonitor(subMonitor, 140));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// Now that we have a plan with all the binary and explicit bundles, do a second pass and add 
		// in all the source.
		try {
			planInSourceBundles(profile, context, subMonitor);
		} catch (CoreException e) {
			// XXX Review required: is adding in the source critical or optional?
			// We failed adding in the source so remove the intermediate profile and rethrow
			P2TargetUtils.getProfileRegistry().removeProfile(profile.getProfileId(), profile.getTimestamp());
			throw e;
		}

		ResolvedBundle[] resolvedBundles = updateAfterResolve(profile, definition);
		subMonitor.worked(10);
		subMonitor.done();
		return resolvedBundles;
	}

	/** 
	 * Update the given change request to remove anything that was explicitly installed
	 * including the internal source IU.  
	 */
	private void computeRemovals(IProfile profile, IProfileChangeRequest request, IInstallableUnit[] units) {
		// if include source is off then ensure that the source IU is removed.
		if (!fIncludeSource) {
			IInstallableUnit sourceIU = getCurrentSourceIU(profile);
			if (sourceIU != null)
				request.remove(sourceIU);
		}
		// remove everything that is marked as roots.  The plan will have the new roots added in anyway.
		IQuery query = new IUProfilePropertyQuery(P2TargetUtils.PROP_INSTALLED_IU, Boolean.toString(true));
		IQueryResult installedIUs = profile.query(query, null);
		request.removeAll(installedIUs.toSet());
	}

	// run a second pass of the planner to add in the source bundles for everything that's
	// in the current profile.
	private void planInSourceBundles(IProfile profile, ProvisioningContext context, IProgressMonitor monitor) throws CoreException {
		if (!fIncludeSource)
			return;

		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(Messages.IUBundleContainer_ProvisioningSourceBundles, 200);

		// create an IU that optionally and greedily requires the related source bundles.
		// Completely replace any source IU that may already be in place
		IInstallableUnit currentSourceIU = getCurrentSourceIU(profile);

		// determine the new version number.  start at 1
		Version sourceVersion = Version.createOSGi(1, 0, 0);
		if (currentSourceIU != null) {
			Integer major = (Integer) currentSourceIU.getVersion().getSegment(0);
			sourceVersion = Version.createOSGi(major.intValue() + 1, 0, 0);
		}
		IInstallableUnit sourceIU = createSourceIU(profile, sourceVersion);

		// call the planner again to add in the new source IU and all available source bundles
		IPlanner planner = P2TargetUtils.getPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		if (currentSourceIU != null)
			request.remove(currentSourceIU);
		request.add(sourceIU);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new SubProgressMonitor(subMonitor, 20));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		if (subMonitor.isCanceled()) {
			return;
		}

		long oldTimestamp = profile.getTimestamp();

		// execute the provisioning plan
		IPhaseSet phases = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE, PhaseSetFactory.PHASE_UNINSTALL});
		IEngine engine = P2TargetUtils.getEngine();
		plan.setProfileProperty(P2TargetUtils.PROP_PROVISION_MODE, TargetDefinitionPersistenceHelper.MODE_PLANNER);
		plan.setProfileProperty(P2TargetUtils.PROP_ALL_ENVIRONMENTS, Boolean.toString(false));
		IStatus result = engine.perform(plan, phases, new SubProgressMonitor(subMonitor, 140));

		if (subMonitor.isCanceled()) {
			return;
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// remove the old (intermediate) profile version now we have a new one with source.
		P2TargetUtils.getProfileRegistry().removeProfile(profile.getProfileId(), oldTimestamp);
		subMonitor.worked(10);
		subMonitor.done();
	}

	// Create and return an IU that has optional and greedy requirements on all source bundles
	// related to bundle IUs in the given queryable. 
	/**
	 * Creates and returns an IU that has optional and greedy requirements on all source bundles
	 * related to bundle IUs in the given queryable.
	 * @param queryable location to search for source bundle IUs
	 * @param iuVersion version to set on the returned installable unit
	 * @return a new installable unit with requirements on the available source IUs
	 */
	private IInstallableUnit createSourceIU(IQueryable queryable, Version iuVersion) {
		// compute the set of source bundles we could possibly need for the bundles in the profile
		IRequirement bundleRequirement = MetadataFactory.createRequirement("org.eclipse.equinox.p2.eclipse.type", "bundle", null, null, false, false, false); //$NON-NLS-1$ //$NON-NLS-2$
		IQueryResult profileIUs = queryable.query(QueryUtil.createIUAnyQuery(), null);
		ArrayList requirements = new ArrayList();
		for (Iterator i = profileIUs.iterator(); i.hasNext();) {
			IInstallableUnit profileIU = (IInstallableUnit) i.next();
			if (profileIU.satisfies(bundleRequirement)) {
				String id = profileIU.getId() + ".source"; //$NON-NLS-1$
				Version version = profileIU.getVersion();
				VersionRange range = new VersionRange(version, true, version, true);
				IRequirement sourceRequirement = MetadataFactory.createRequirement("osgi.bundle", id, range, null, true, false, true); //$NON-NLS-1$
				requirements.add(sourceRequirement);
			}
		}

		InstallableUnitDescription sourceDescription = new MetadataFactory.InstallableUnitDescription();
		sourceDescription.setSingleton(true);
		sourceDescription.setId(SOURCE_IU_ID);
		sourceDescription.setVersion(iuVersion);
		sourceDescription.addRequirements(requirements);
		IProvidedCapability capability = MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, SOURCE_IU_ID, iuVersion);
		sourceDescription.setCapabilities(new IProvidedCapability[] {capability});
		return MetadataFactory.createInstallableUnit(sourceDescription);
	}

	/**
	 * Lookup and return the source IU in the given queryable or <code>null</code> if not found.
	 * @param queryable location to look for source IUs
	 * @return the source IU or <code>null</code>
	 */
	private IInstallableUnit getCurrentSourceIU(IQueryable queryable) {
		IQuery query = QueryUtil.createIUQuery(SOURCE_IU_ID);
		IQueryResult list = queryable.query(query, null);
		IInstallableUnit currentSourceIU = null;
		if (!list.isEmpty())
			currentSourceIU = (IInstallableUnit) list.iterator().next();
		return currentSourceIU;
	}

	/**
	 * Used to resolve the contents of this container when the user has chosen to manage the dependencies in the target
	 * themselves.  The selected IUs and any required software that can be found will be retrieved from the repositories 
	 * and added to the target.  Any missing required software will be ignored.
	 * 
	 * @param definition definition being resolved
	 * @param monitor for reporting progress
	 * @return set of resolved bundles included in this container
	 * @throws CoreException if there is a problem interacting with the repositories
	 */
	private IResolvedBundle[] resolveWithSlicer(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 10);
		subMonitor.beginTask(Messages.IUBundleContainer_0, 180);

		// retrieve profile
		IProfile profile = P2TargetUtils.getProfile(definition);
		subMonitor.worked(10);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		// resolve IUs
		IInstallableUnit[] units = getInstallableUnits(profile);

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		URI[] repositories = resolveRepositories();
		int repoCount = repositories.length;
		if (repoCount == 0) {
			return new IResolvedBundle[0];
		}

		IProgressMonitor loadMonitor = new SubProgressMonitor(subMonitor, 10);
		loadMonitor.beginTask(null, repoCount * 10);
		List metadataRepos = new ArrayList(repoCount);
		MultiStatus repoStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.IUBundleContainer_ProblemsLoadingRepositories, null);
		IMetadataRepositoryManager manager = P2TargetUtils.getRepoManager();
		for (int i = 0; i < repoCount; ++i) {
			try {
				IMetadataRepository repo = manager.loadRepository(repositories[i], new SubProgressMonitor(loadMonitor, 10));
				metadataRepos.add(repo);
			} catch (ProvisionException e) {
				repoStatus.add(e.getStatus());
			}
		}
		loadMonitor.done();

		IQueryable allMetadata;
		if (metadataRepos.size() == 0) {
			throw new CoreException(repoStatus);
		} else if (metadataRepos.size() == 1) {
			allMetadata = (IQueryable) metadataRepos.get(0);
		} else {
			allMetadata = QueryUtil.compoundQueryable(metadataRepos);
		}

		// do an initial slice to add everything the user requested
		IQueryResult queryResult = slice(units, allMetadata, definition, subMonitor);
		if (subMonitor.isCanceled() || queryResult == null || queryResult.isEmpty()) {
			return new IResolvedBundle[0];
		}

		// If we are including source then create a source IU to bring in the relevant source
		// bundles and run the slicer again.
		if (fIncludeSource) {
			// Build an IU that represents all the source bundles and slice again to add them in if available
			IInstallableUnit sourceIU = createSourceIU(queryResult, Version.createOSGi(1, 0, 0));
			IInstallableUnit[] units2 = new IInstallableUnit[units.length + 1];
			System.arraycopy(units, 0, units2, 0, units.length);
			units2[units.length] = sourceIU;

			queryResult = slice(units2, allMetadata, definition, subMonitor);
			if (subMonitor.isCanceled() || queryResult == null || queryResult.isEmpty()) {
				return new IResolvedBundle[0];
			}
		}

		IEngine engine = P2TargetUtils.getEngine();
		ProvisioningContext context = new ProvisioningContext(P2TargetUtils.getAgent());
		context.setMetadataRepositories(repositories);
		context.setArtifactRepositories(resolveArtifactRepositories());
		IProvisioningPlan plan = engine.createPlan(profile, context);

		Set newSet = queryResult.toSet();
		Iterator itor = newSet.iterator();
		while (itor.hasNext()) {
			plan.addInstallableUnit((IInstallableUnit) itor.next());
		}
		for (int i = 0; i < units.length; i++) {
			IInstallableUnit unit = units[i];
			plan.setInstallableUnitProfileProperty(unit, P2TargetUtils.PROP_INSTALLED_IU, Boolean.toString(true));
		}

		// remove all units that are in the current profile but not in the new slice
		Set toRemove = profile.query(QueryUtil.ALL_UNITS, null).toSet();
		toRemove.removeAll(newSet);
		for (Iterator i = toRemove.iterator(); i.hasNext();) {
			plan.removeInstallableUnit((IInstallableUnit) i.next());
		}

		plan.setProfileProperty(P2TargetUtils.PROP_PROVISION_MODE, TargetDefinitionPersistenceHelper.MODE_SLICER);
		plan.setProfileProperty(P2TargetUtils.PROP_ALL_ENVIRONMENTS, Boolean.toString(getIncludeAllEnvironments()));

		// execute the provisioning plan
		IPhaseSet phases = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE});
		IStatus result = engine.perform(plan, phases, new SubProgressMonitor(subMonitor, 140));

		if (subMonitor.isCanceled()) {
			return new IResolvedBundle[0];
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		ResolvedBundle[] resolvedBundles = updateAfterResolve(profile, definition);

		subMonitor.worked(10);
		subMonitor.done();
		return resolvedBundles;
	}

	private ResolvedBundle[] updateAfterResolve(IProfile profile, ITargetDefinition definition) throws CoreException {
		// Cache the feature list
		queryForFeatures(profile);

		// query for bundles
		IFileArtifactRepository repository = null;
		try {
			repository = P2TargetUtils.getBundlePool(profile);
		} catch (CoreException e) {
			if (DEBUG_PROFILE) {
				System.out.println("Bundle pool repository could not be loaded"); //$NON-NLS-1$
			}
			return null;
		}

		Map bundles = generateResolvedBundles(profile, repository, true);
		if (bundles == null || bundles.isEmpty()) {
			if (DEBUG_PROFILE) {
				System.out.println("Profile does not contain any bundles or artifacts were missing"); //$NON-NLS-1$
			}
			return null;
		}

		removeDuplicateBundles(definition, bundles);
		if (bundles.isEmpty()) {
			return null;
		}

		return (ResolvedBundle[]) bundles.values().toArray(new ResolvedBundle[bundles.size()]);
	}

	private IQueryResult slice(IInstallableUnit[] units, IQueryable allMetadata, ITargetDefinition definition, SubProgressMonitor subMonitor) throws CoreException {
		// slice IUs and all prerequisites
		PermissiveSlicer slicer = null;
		if (getIncludeAllEnvironments()) {
			slicer = new PermissiveSlicer(allMetadata, new Properties(), true, false, true, true, false);
		} else {
			Properties props = new Properties();
			props.setProperty("osgi.os", definition.getOS() != null ? definition.getOS() : Platform.getOS()); //$NON-NLS-1$
			props.setProperty("osgi.ws", definition.getWS() != null ? definition.getWS() : Platform.getWS()); //$NON-NLS-1$
			props.setProperty("osgi.arch", definition.getArch() != null ? definition.getArch() : Platform.getOSArch()); //$NON-NLS-1$
			props.setProperty("osgi.nl", definition.getNL() != null ? definition.getNL() : Platform.getNL()); //$NON-NLS-1$
			props.setProperty(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
			slicer = new PermissiveSlicer(allMetadata, props, true, false, false, true, false);
		}
		IQueryable slice = slicer.slice(units, new SubProgressMonitor(subMonitor, 10));
		if (!slicer.getStatus().isOK()) {
			throw new CoreException(slicer.getStatus());
		}
		IQueryResult queryResult = null;
		if (slice != null)
			queryResult = slice.query(QueryUtil.createIUAnyQuery(), new SubProgressMonitor(subMonitor, 10));
		return queryResult;
	}

	/**
	 * Queries the given given queryable and finds all feature group IUs.  The feature id/versions of the features
	 * are cached in {@link #fFeatures}.
	 * 
	 * @param queryable profile/slicer/etc. to query for features
	 */
	private void queryForFeatures(IQueryable queryable) {
		// Query for features, cache the result for calls to resolveFeatures()
		// Get any IU with the group property, this will return any feature groups
		IQuery featureQuery = QueryUtil.createMatchQuery("properties[$0] == $1", new Object[] {MetadataFactory.InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString()}); //$NON-NLS-1$
		IQueryResult featureResult = queryable.query(featureQuery, null);
		List features = new ArrayList();
		for (Iterator iterator = featureResult.iterator(); iterator.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			String id = unit.getId();
			if (id.endsWith(FEATURE_ID_SUFFIX)) {
				id = id.substring(0, id.length() - FEATURE_ID_SUFFIX.length());
			}
			String version = unit.getVersion().toString();
			features.add(new NameVersionDescriptor(id, version, NameVersionDescriptor.TYPE_FEATURE));
		}
		fFeatures = (NameVersionDescriptor[]) features.toArray(new NameVersionDescriptor[features.size()]);
	}

	/**
	 * Returns the IU's this container references. Checks in the profile first to avoid
	 * going out to repositories.
	 * 
	 * @param profile profile to check first
	 * @return IU's
	 * @exception CoreException if unable to retrieve IU's
	 */
	public synchronized IInstallableUnit[] getInstallableUnits(IProfile profile) throws CoreException {
		if (fUnits == null) {
			fUnits = new IInstallableUnit[fIds.length];
			for (int i = 0; i < fIds.length; i++) {
				IQuery query = QueryUtil.createIUQuery(fIds[i], fVersions[i]);
				IQueryResult queryResult = profile.query(query, null);
				if (queryResult.isEmpty()) {
					// try repositories
					URI[] repositories = resolveRepositories();
					for (int j = 0; j < repositories.length; j++) {
						try {
							IMetadataRepository repository = P2TargetUtils.getRepository(repositories[j]);
							queryResult = repository.query(query, null);
							if (!queryResult.isEmpty()) {
								break;
							}
						} catch (ProvisionException e) {
							// Ignore and move on to the next site
						}
					}
				}
				if (queryResult.isEmpty()) {
					// not found
					fUnits = null;
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.IUBundleContainer_1, fIds[i])));
				}
				fUnits[i] = (IInstallableUnit) queryResult.iterator().next();
			}
		}
		return fUnits;
	}

	private IMetadataRepository[] getRepos() throws CoreException {
		URI[] repos = resolveRepositories();
		IMetadataRepository[] result = new IMetadataRepository[repos.length];
		for (int i = 0; i < repos.length; i++)
			result[i] = P2TargetUtils.getRepository(repos[i]);
		return result;
	}

	/**
	 * Update the root IUs to the latest available in the repos associated with this container.
	 * 
	 * @param toUpdate the set of IU ids in this container to consider updating.  If null
	 * then update everything
	 * @return a bitmasked int indicating how/if this container changed.  See {@link UpdateTargetJob#DIRTY} and {@link UpdateTargetJob#UPDATED}. 
	 * @exception CoreException if unable to retrieve IU's
	 */
	public synchronized int update(Set toUpdate) throws CoreException {
		IQueryable source = new CompoundQueryable(getRepos());
		int dirty = 0;
		int updated = 0;
		for (int i = 0; i < fIds.length; i++) {
			if (!toUpdate.isEmpty() && !toUpdate.contains(fIds[i]))
				continue;
			IQuery query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(fIds[i]));
			IQueryResult queryResult = source.query(query, null);
			Iterator it = queryResult.iterator();
			// bail if the feature is no longer available.
			if (!it.hasNext())
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.IUBundleContainer_1, fIds[i])));
			IInstallableUnit iu = (IInstallableUnit) it.next();
			fUnits[i] = iu;
			// if the version is different from the spec (up or down), record the change.
			if (!iu.getVersion().equals(fVersions[i])) {
				updated = UpdateTargetJob.UPDATED;
				// if the spec was not specific (e.g., 0.0.0) the target def itself has changed.
				if (!fVersions[i].equals(Version.emptyVersion)) {
					fVersions[i] = iu.getVersion();
					dirty = UpdateTargetJob.DIRTY;
				}
			}
		}
		return dirty | updated;
	}

	/**
	 * Checks all other bundle containers in the given target and removes any bundles provided by them from
	 * the map.  This prevents targets containing more than one IUBundleContainer from having duplicate bundles.
	 * 
	 * @param definition target definition to look for other containers in
	 * @param bundles collection of bundles arranged by mapping BundleInfo to IResolvedBundle
	 */
	private void removeDuplicateBundles(ITargetDefinition definition, Map bundles) {
		// remove all bundles from previous IU containers (so we don't get duplicates from multi-locations
		IBundleContainer[] containers = definition.getBundleContainers();
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			if (container == this) {
				break;
			}
			if (container instanceof IUBundleContainer) {
				IUBundleContainer bc = (IUBundleContainer) container;
				IResolvedBundle[] included = bc.getBundles();
				if (included != null) {
					for (int j = 0; j < included.length; j++) {
						bundles.remove(included[j].getBundleInfo());
					}
				}
			}
		}
	}

	/**
	 * Collects all available installable units from the given source that represent OSGI
	 * bundles.  A IResolvedBundle is created for each and a map containing all results
	 * mapping BundleInfo to IResolvedBundle is returned.
	 * <p>
	 * If there is an artifact missing for a unit it will either be ignored (not added to the returned map),
	 * or <code>null</code> will be returned depending on the ignoreMissingFiles parameter. 
	 * </p>
	 * @param source A queryable profile or slice that the bundle units will be obtained from
	 * @param ignoreMissingFiles if <code>true</code> ius that have missing artifacts will be ignored and not added to the map, if <code>false</code>, <code>null</code> will be returned
	 * @return map of BundleInfo to IResolvedBundle or <code>null</code> if a missing file is found and not ignored
	 * @throws CoreException
	 */
	private Map generateResolvedBundles(IQueryable source, IFileArtifactRepository repo, boolean ignoreMissingFiles) throws CoreException {
		OSGiBundleQuery query = new OSGiBundleQuery();
		IQueryResult queryResult = source.query(query, null);
		Map bundles = new LinkedHashMap();
		for (Iterator iterator = queryResult.iterator(); iterator.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			Collection artifacts = unit.getArtifacts();
			for (Iterator iterator2 = artifacts.iterator(); iterator2.hasNext();) {
				File file = repo.getArtifactFile((IArtifactKey) iterator2.next());
				if (file == null) {
					// Missing file
					if (!ignoreMissingFiles) {
						if (DEBUG_PROFILE) {
							System.out.println("Backing file missing for: " + unit.getId()); //$NON-NLS-1$
						}
						return null;
					}
				} else {
					IResolvedBundle bundle = generateBundle(file);
					if (bundle != null) {
						bundles.put(bundle.getBundleInfo(), bundle);
					}
				}
			}
		}
		return bundles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof IUBundleContainer) {
			IUBundleContainer iuContainer = (IUBundleContainer) container;
			if (iuContainer.getIncludeAllRequired() == getIncludeAllRequired()) {
				// include all targets only matters if include all required is turned off
				if (getIncludeAllRequired() || iuContainer.getIncludeAllEnvironments() == getIncludeAllEnvironments()) {
					return isEqualOrNull(fIds, iuContainer.fIds) && isEqualOrNull(fVersions, iuContainer.fVersions) && isEqualOrNull(fRepos, iuContainer.fRepos);
				}
			}
			if (fIncludeSource != iuContainer.getIncludeSource())
				return false;
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
	 * Returns the repositories to consider when resolving IU's (will return default set of
	 * repositories if current repository settings are <code>null</code>).
	 *  
	 * @return URI's of repositories to use when resolving bundles
	 * @exception CoreException
	 */
	private URI[] resolveRepositories() throws CoreException {
		if (fRepos == null) {
			IMetadataRepositoryManager manager = P2TargetUtils.getRepoManager();
			return manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		}
		return fRepos;
	}

	/**
	 * Returns the artifact repositories to consider when getting artifacts.  Returns a default set of
	 * repositories if current repository settings are <code>null</code>).
	 *  
	 * @return URI's of repositories to use when getting artifacts
	 * @exception CoreException
	 */
	private URI[] resolveArtifactRepositories() throws CoreException {
		URI[] repositories = fRepos;
		if (fRepos == null) {
			IArtifactRepositoryManager manager = P2TargetUtils.getArtifactRepositoryManager();
			repositories = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		}
		if (!useAdditionalLocalArtifacts())
			return repositories;
		Set additionalRepos = new HashSet(Arrays.asList(repositories));
		findProfileRepos(additionalRepos);
		findWorkspaceRepos(additionalRepos);
		return (URI[]) additionalRepos.toArray(new URI[additionalRepos.size()]);
	}

	/**
	 * @return whether to use local artifact repositories when provisioning the target
	 */
	private boolean useAdditionalLocalArtifacts() {
		// TODO consider using a preference here or another strategy if users are able to spec 
		// what local repos are to be considered.
		return true;
	}

	/** 
	 * Add the artifact repos from the PDE target bundle pools from all known repos.  For example, the list 
	 * of "recent workspaces" maintained by the IDE is a good source.
	 * 
	 * @param additionalRepos the set to which additional repos are added.
	 */
	private void findWorkspaceRepos(Set additionalRepos) {
		IPreferencesService prefs = P2TargetUtils.getPreferences();
		if (prefs == null)
			return;
		String recent = prefs.getString("org.eclipse.ui.ide", "RECENT_WORKSPACES", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		if (recent == null)
			return;
		String[] recents = recent.split("\n"); //$NON-NLS-1$
		for (int i = 0; i < recents.length; i++) {
			String bundlePool = recents[i] + "/.metadata/.plugins/org.eclipse.pde.core/.bundle_pool"; //$NON-NLS-1$
			if (new File(bundlePool).exists()) {
				URI uri;
				try {
					uri = new URI("file", bundlePool, null); //$NON-NLS-1$
					additionalRepos.add(uri);
				} catch (URISyntaxException e) {
					// should never happen
				}
			}
		}
	}

	/** 
	 * Look through the current p2 profile (_SELF_) and add the artifact repos that make up its
	 * bundle pool, dropins location, ...  This helps in the cases that you are targeting stuff that 
	 * makes up your current IDE.
	 * 
	 * @param additionalRepos the set to which additional repos are added.
	 */
	private void findProfileRepos(Set additionalRepos) {
		try {
			// NOTE: be sure to use the global p2 agent here as we are looking for SELF.
			IProfileRegistry profileRegistry = (IProfileRegistry) P2TargetUtils.getGlobalAgent().getService(IProfileRegistry.SERVICE_NAME);
			if (profileRegistry == null)
				return;
			IProfile self = profileRegistry.getProfile(IProfileRegistry.SELF);
			if (self == null)
				return;

			IAgentLocation location = (IAgentLocation) P2TargetUtils.getGlobalAgent().getService(IAgentLocation.SERVICE_NAME);
			URI dataArea = location.getDataArea("org.eclipse.equinox.p2.engine"); //$NON-NLS-1$
			dataArea = URIUtil.append(dataArea, "profileRegistry/" + self.getProfileId() + ".profile"); //$NON-NLS-1$//$NON-NLS-2$
			ProfileMetadataRepository profileRepo = new ProfileMetadataRepository(P2TargetUtils.getGlobalAgent(), dataArea, null);
			Collection repos = profileRepo.getReferences();
			for (Iterator i = repos.iterator(); i.hasNext();) {
				Object element = i.next();
				if (element instanceof IRepositoryReference) {
					IRepositoryReference reference = (IRepositoryReference) element;
					if (reference.getType() == IRepository.TYPE_ARTIFACT && reference.getLocation() != null)
						additionalRepos.add(reference.getLocation());
				}
			}
		} catch (CoreException e) {
			// if there is a problem, move on.  Could log something here 
			return;
		}
	}

	/**
	 * Sets whether all required units must be available to resolve this container.  When <code>true</code>
	 * the resolve operation will use the planner to determine the complete set of IUs required to
	 * make the selected IUs runnable.  If any dependencies are missing, the resolve operation will return an
	 * error explaining what problems exist.  When <code>false</code> the resolve operation will use the slicer
	 * to determine what units to include.  Any required units that are not available in the repositories will
	 * be ignored.
	 * <p>
	 * Since there is only one profile per target and the planner and slicer resolve methods are incompatible
	 * it is highly recommended that the parent target be passed to this method so all other IUBundleContainers
	 * in the target can be updated with the new setting. 
	 * </p>
	 * @param include whether all required units must be available to resolve this container
	 * @param definition parent target, used to update other IUBundleContainers with this setting, can be <code>null</code>
	 */
	public void setIncludeAllRequired(boolean include, ITargetDefinition definition) {
		fIncludeAllRequired = include;
		if (definition != null) {
			IBundleContainer[] containers = definition.getBundleContainers();
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i] instanceof IUBundleContainer && containers[i] != this) {
						((IUBundleContainer) containers[i]).setIncludeAllRequired(include, null);
					}
				}
			}
		}
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
		return fIncludeAllRequired;
	}

	/**
	 * Sets whether all environment (platform) specific installable units should
	 * be included in this container when it is resolved.  This feature is not supported
	 * by the planner so will only have an effect if the include all required setting
	 * is turned off ({@link #getIncludeAllRequired()}).
	 * <p>
	 * There is only one profile per target and this setting can only be set for the
	 * entire target definition.  It is highly recommended that the parent target be passed
	 * to this method so all other IUBundleContainers in the target can be updated with the 
	 * new setting. 
	 * </p>
	 * @param include whether environment specific units should be included
	 * @param definition parent target, used to update other IUBundleContainers with this setting, can be <code>null</code>
	 */
	public void setIncludeAllEnvironments(boolean include, ITargetDefinition definition) {
		fIncludeMultipleEnvironments = include;
		if (definition != null) {
			IBundleContainer[] containers = definition.getBundleContainers();
			if (containers != null) {
				for (int i = 0; i < containers.length; i++) {
					if (containers[i] instanceof IUBundleContainer && containers[i] != this) {
						((IUBundleContainer) containers[i]).setIncludeAllEnvironments(include, null);
					}
				}
			}
		}
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
		return fIncludeMultipleEnvironments;
	}

	/**
	 * Set whether or not the source bundles corresponding to any binary bundles should
	 * be automatically included in the target.
	 * 
	 * @param value whether or not to include source
	 */
	public void setIncludeSource(boolean value) {
		fIncludeSource = value;
	}

	/**
	 * Returns whether or not source bundles corresponding to selected binary bundles 
	 * are automatically included in the target.
	 * 
	 * @return whether or not source is included automatically
	 */
	public boolean getIncludeSource() {
		return fIncludeSource;
	}

	/**
	 * Removes an installable unit from this container.  The container will no longer be resolved.
	 *  
	 * @param unit unit to remove from the list of root IUs
	 */
	public void removeInstallableUnit(IInstallableUnit unit) {
		List newUnits = new ArrayList(fUnits.length);
		for (int i = 0; i < fUnits.length; i++) {
			if (!fUnits[i].equals(unit)) {
				newUnits.add(fUnits[i]);
			}
		}
		fUnits = (IInstallableUnit[]) newUnits.toArray(new IInstallableUnit[newUnits.size()]);
		fIds = new String[fUnits.length];
		fVersions = new Version[fUnits.length];
		for (int i = 0; i < fUnits.length; i++) {
			fIds[i] = fUnits[i].getId();
			fVersions[i] = fUnits[i].getVersion();
		}
		// Need to mark the container as unresolved
		clearResolutionStatus();
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

}
