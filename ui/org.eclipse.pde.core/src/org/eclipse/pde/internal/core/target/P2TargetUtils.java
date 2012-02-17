/*******************************************************************************
 * Copyright (c) 2010, 2012 EclipseSource Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.engine.phases.*;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.framework.*;

public class P2TargetUtils {

	private static final String SOURCE_IU_ID = "org.eclipse.pde.core.target.source.bundles"; //$NON-NLS-1$

	/**
	 * URI to the local directory where the p2 agent keeps its information.
	 */
	public static URI AGENT_LOCATION;
	static {
		try {
			AGENT_LOCATION = URIUtil.fromString("file:" + PDECore.getDefault().getStateLocation().append(".p2")); //$NON-NLS-1$//$NON-NLS-2$
		} catch (Exception e) {
			// should never happen
		}
	}

	/**
	 * Path to the local directory where the local bundle pool is stored for p2 profile
	 * based targets.
	 */
	public static final IPath BUNDLE_POOL = PDECore.getDefault().getStateLocation().append(".bundle_pool"); //$NON-NLS-1$

	/**
	 * Path to the local directory where install folders are created for p2 profile
	 * based targets.
	 */
	static final IPath INSTALL_FOLDERS = PDECore.getDefault().getStateLocation().append(".install_folders"); //$NON-NLS-1$	

	/**
	 * Prefix for all profiles ID's associated with target definitions
	 */
	static final String PROFILE_ID_PREFIX = "TARGET_DEFINITION:"; //$NON-NLS-1$

	/**
	 * Installable unit property to mark IU's that have been installed in a profile by
	 * a bundle container (rather than as a secondary/required IU).
	 */
	static final String PROP_INSTALLED_IU = PDECore.PLUGIN_ID + ".installed_iu"; //$NON-NLS-1$

	/**
	 * Profile property that keeps track of provisioning mode for the target
	 * (slice versus plan).
	 */
	static final String PROP_PROVISION_MODE = PDECore.PLUGIN_ID + ".provision_mode"; //$NON-NLS-1$

	/**
	 * Profile property that keeps track of provisioning mode for the target
	 * (all environments/true versus false).
	 */
	static final String PROP_ALL_ENVIRONMENTS = PDECore.PLUGIN_ID + ".all_environments"; //$NON-NLS-1$	

	/**
	 * Profile property that keeps track of the target sequence number
	 */
	static final String PROP_SEQUENCE_NUMBER = PDECore.PLUGIN_ID + ".sequence"; //$NON-NLS-1$	

	/**
	 * Profile property that tracks whether or not source to be auto-included
	 */
	static final String PROP_AUTO_INCLUDE_SOURCE = PDECore.PLUGIN_ID + ".autoIncludeSource"; //$NON-NLS-1$	

	/**
	 * Profile property that tracks whether or not the configuration phase should be executed when installing
	 */
	static final String PROP_INCLUDE_CONFIGURE_PHASE = PDECore.PLUGIN_ID + ".includeConfigure"; //$NON-NLS-1$

	/**
	 * Table mapping {@link ITargetDefinition} to synchronizer (P2TargetUtils) instance.
	 */
	private static Map synchronizers = new HashMap();

	/** 
	 * The profile to be synchronized
	 */
	private IProfile fProfile;

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
	 * <code>false</code> by default
	 * </p>
	 */
	private boolean fIncludeSource = false;

	/**
	 * Whether this container should execute the configure phase when installing the IUs
	 * <p>
	 * <code>false</code> by default
	 * </p>
	 */
	private boolean fIncludeConfigurePhase = false;

	/**
	 * Whether or not this synchronizer is dirty by means other than target tweaks etc.
	 */
	private boolean fDirty = false;

	/**
	 * Deletes any profiles associated with target definitions that no longer exist
	 * and returns a list of profile identifiers that were deleted.
	 */
	public static List cleanOrphanedTargetDefinitionProfiles() throws CoreException {
		List list = new ArrayList();
		IProfileRegistry registry = getProfileRegistry();
		ITargetPlatformService tps = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
		if (registry != null && tps != null) {
			IProfile[] profiles = registry.getProfiles();
			for (int i = 0; i < profiles.length; i++) {
				IProfile profile = profiles[i];
				String id = profile.getProfileId();
				if (id.startsWith(PROFILE_ID_PREFIX)) {
					String memento = id.substring(PROFILE_ID_PREFIX.length());
					ITargetHandle handle = tps.getTarget(memento);
					if (!handle.exists()) {
						deleteProfile(handle);
						list.add(id);
					}
				}
			}
		}
		return list;
	}

	/**
	 * Recursively deletes folder and files.
	 * 
	 * @param folder
	 */
	private static void delete(File folder) {
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				delete(file);
			}
			file.delete();
		}
		folder.delete();
	}

	/**
	 * Deletes the profile associated with this target handle, if any. Returns
	 * <code>true</code> if a profile existed and was deleted, otherwise <code>false</code>.
	 * 
	 * @throws CoreException if unable to delete the profile
	 */
	public static void deleteProfile(ITargetHandle handle) throws CoreException {
		IProfileRegistry registry = getProfileRegistry();
		if (registry != null) {
			IProfile profile = registry.getProfile(getProfileId(handle));
			if (profile != null) {
				String location = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
				registry.removeProfile(getProfileId(handle));
				if (location != null && location.length() > 0) {
					File folder = new File(location);
					if (folder.exists()) {
						delete(folder);
					}
				}
			}
		}
	}

	/**
	 * Performs garbage collection based on remaining profiles. Should be called to avoid
	 * having PDE's bundle pool area grow unbounded.
	 */
	public static void garbageCollect() {
		try {
			IProfile[] profiles = getProfileRegistry().getProfiles();
			for (int i = 0; i < profiles.length; i++) {
				if (profiles[i].getProfileId().startsWith(PROFILE_ID_PREFIX)) {
					getGarbageCollector().runGC(profiles[i]);
				}
			}
		} catch (CoreException e) {
			// XXX likely should log something here.
			return;
		}
	}

	/**
	 * Generates the environment properties string for this target definition's p2 profile.
	 * 
	 * @return environment properties
	 */
	private String generateEnvironmentProperties(ITargetDefinition target) {
		// TODO: are there constants for these keys?
		StringBuffer env = new StringBuffer();
		String ws = target.getWS();
		if (ws == null) {
			ws = Platform.getWS();
		}
		env.append("osgi.ws="); //$NON-NLS-1$
		env.append(ws);
		env.append(","); //$NON-NLS-1$
		String os = target.getOS();
		if (os == null) {
			os = Platform.getOS();
		}
		env.append("osgi.os="); //$NON-NLS-1$
		env.append(os);
		env.append(","); //$NON-NLS-1$
		String arch = target.getArch();
		if (arch == null) {
			arch = Platform.getOSArch();
		}
		env.append("osgi.arch="); //$NON-NLS-1$
		env.append(arch);
		return env.toString();
	}

	/**
	 * Generates the NL property for this target definition's p2 profile.
	 * 
	 * @return NL profile property
	 */
	private String generateNLProperty(ITargetDefinition target) {
		String nl = target.getNL();
		if (nl == null) {
			nl = Platform.getNL();
		}
		return nl;
	}

	public static IProvisioningAgent getAgent() throws CoreException {
		//Is there already an agent for this location?
		String filter = "(locationURI=" + String.valueOf(AGENT_LOCATION) + ")"; //$NON-NLS-1$//$NON-NLS-2$
		ServiceReference[] serviceReferences = null;
		BundleContext context = PDECore.getDefault().getBundleContext();
		try {
			serviceReferences = context.getServiceReferences(IProvisioningAgent.SERVICE_NAME, filter);
			if (serviceReferences != null) {
				return (IProvisioningAgent) context.getService(serviceReferences[0]);
			}
		} catch (InvalidSyntaxException e) {
			// ignore
		} finally {
			if (serviceReferences != null)
				context.ungetService(serviceReferences[0]);
		}

		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) PDECore.getDefault().acquireService(IProvisioningAgentProvider.SERVICE_NAME);
		try {
			if (provider == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_7));
			}
			IProvisioningAgent agent = provider.createAgent(AGENT_LOCATION);
			if (agent == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_7));
			}
			// turn off the garbage collector for the PDE agent.  GC is managed on a coarser grain
			GarbageCollector garbageCollector = (GarbageCollector) agent.getService(GarbageCollector.class.getName());
			if (garbageCollector != null) {
				garbageCollector.stop();
			}
			return agent;
		} catch (ProvisionException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_7, e));
		}
	}

	/**
	 * Returns the global p2 provisioning agent.  This is useful when looking to inherit or use
	 * some settings from the global p2 world.
	 * 
	 * @return the global p2 provisioning agent
	 * @throws CoreException
	 */
	public static IProvisioningAgent getGlobalAgent() throws CoreException {
		IProvisioningAgent agent = (IProvisioningAgent) PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
		if (agent == null)
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_11));
		return agent;
	}

	/**
	 * Returns the provisioning agent location service.
	 * 
	 * @return provisioning agent location service
	 * @throws CoreException if none
	 */
	public static IAgentLocation getAgentLocation() throws CoreException {
		IAgentLocation result = (IAgentLocation) getAgent().getService(IAgentLocation.SERVICE_NAME);
		if (result == null)
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_10));
		return result;
	}

	/**
	 * Returns the provisioning engine service.
	 * 
	 * @return provisioning engine
	 * @throws CoreException if none
	 */
	public static IArtifactRepositoryManager getArtifactRepositoryManager() throws CoreException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_3));
		}
		return manager;
	}

	/**
	 * Returns the local bundle pool (repository) where bundles are stored
	 * 
	 * @return local file artifact repository
	 * @throws CoreException
	 */
	public static IFileArtifactRepository getBundlePool() throws CoreException {
		URI uri = BUNDLE_POOL.toFile().toURI();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		try {
			if (manager.contains(uri))
				return (IFileArtifactRepository) manager.loadRepository(uri, null);
		} catch (CoreException e) {
			// could not load or there wasn't one, fall through to create 
		}
		String repoName = "PDE Target Bundle Pool"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(uri, repoName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		return (IFileArtifactRepository) result;
	}

	/**
	 * Returns the provisioning engine service.
	 * 
	 * @return provisioning engine
	 * @throws CoreException if none
	 */
	public static IEngine getEngine() throws CoreException {
		IEngine engine = (IEngine) getAgent().getService(IEngine.class.getName());
		if (engine == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_4));
		}
		return engine;
	}

	/**
	 * Returns the p2 garbage collector
	 * 
	 * @return p2 garbage collector
	 * @throws CoreException if none
	 */
	public static GarbageCollector getGarbageCollector() throws CoreException {
		GarbageCollector engine = (GarbageCollector) getAgent().getService(GarbageCollector.class.getName());
		if (engine == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_9));
		}
		return engine;
	}

	/**
	 * Returns the provisioning planner.
	 * 
	 * @return provisioning planner
	 * @throws CoreException if none
	 */
	public static IPlanner getPlanner() throws CoreException {
		IPlanner planner = (IPlanner) getAgent().getService(IPlanner.class.getName());
		if (planner == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_5));
		}
		return planner;
	}

	/**
	 * Returns the preferences service.
	 * 
	 * @return preferences service or null if none
	 */
	public static IPreferencesService getPreferences() {
		return (IPreferencesService) PDECore.getDefault().acquireService(IPreferencesService.class.getName());
	}

	/**
	 * Returns whether the contents of the profile matches the expected contents of the target definition
	 * 
	 * @return whether or not the profile and target definitions match
	 * @throws CoreException in unable to retrieve profile
	 */
	private boolean checkProfile(ITargetDefinition target) throws CoreException {
		// make sure we have a profile to validate
		if (fProfile == null) {
			return false;
		}

		if (fDirty)
			return false;
		// check that the target and profiles are in sync. If they are then life is good.
		// If they are not equal, there is still a chance that everything is ok.
		String profileNumber = fProfile.getProperty(PROP_SEQUENCE_NUMBER);
		if (Integer.toString(((TargetDefinition) target).getSequenceNumber()).equals(profileNumber)) {
			return true;
		}

		// check if all environments setting is the same
		boolean all = false;
		String value = fProfile.getProperty(PROP_ALL_ENVIRONMENTS);
		if (value != null) {
			all = Boolean.valueOf(value).booleanValue();
			if (!Boolean.toString(getIncludeAllEnvironments()).equals(value)) {
				return false;
			}
		}

		// ensure environment & NL settings are still the same (else we need a new profile)
		String property = null;
		if (!all) {
			property = generateEnvironmentProperties(target);
			value = fProfile.getProperty(IProfile.PROP_ENVIRONMENTS);
			if (!property.equals(value)) {
				return false;
			}
		}
		property = generateNLProperty(target);
		value = fProfile.getProperty(IProfile.PROP_NL);
		if (!property.equals(value)) {
			return false;
		}

		// check provisioning mode: slice versus plan
		if (!getProvisionMode(target).equals(fProfile.getProperty(PROP_PROVISION_MODE))) {
			return false;
		}

		// check that the include source flag matches what the profile represents
		if (getIncludeSource() != Boolean.valueOf(fProfile.getProperty(PROP_AUTO_INCLUDE_SOURCE)).booleanValue()) {
			return false;
		}

		if (getIncludeConfigurePhase() != Boolean.valueOf(fProfile.getProperty(PROP_INCLUDE_CONFIGURE_PHASE)).booleanValue()) {
			return false;
		}

		// check top level IU's. If any have been removed from the containers that are
		// still in the profile, we need to recreate (rather than uninstall)
		IUProfilePropertyQuery propertyQuery = new IUProfilePropertyQuery(PROP_INSTALLED_IU, Boolean.toString(true));
		IQueryResult queryResult = fProfile.query(propertyQuery, null);
		Iterator iterator = queryResult.iterator();
		Set installedIUs = new HashSet();
		while (iterator.hasNext()) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			installedIUs.add(new NameVersionDescriptor(unit.getId(), unit.getVersion().toString()));
		}
		ITargetLocation[] containers = target.getTargetLocations();
		if (containers == null) {
			return installedIUs.isEmpty();
		}
		for (int i = 0; i < containers.length; i++) {
			if (containers[i] instanceof IUBundleContainer) {
				IUBundleContainer bc = (IUBundleContainer) containers[i];
				String[] ids = bc.getIds();
				Version[] versions = bc.getVersions();
				for (int j = 0; j < versions.length; j++) {
					// if there is something in a container but not in the profile, recreate
					if (!installedIUs.remove(new NameVersionDescriptor(ids[j], versions[j].toString()))) {
						return false;
					}
				}
			}
		}
		if (!installedIUs.isEmpty()) {
			return false;
		}

		// Phew! seems like the profile checks out.  
		return true;
	}

	/**
	 * Sets whether all required units must be available to resolve this container.  When <code>true</code>
	 * the resolve operation will use the planner to determine the complete set of IUs required to
	 * make the selected IUs runnable.  If any dependencies are missing, the resolve operation will return an
	 * error explaining what problems exist.  When <code>false</code> the resolve operation will use the slicer
	 * to determine what units to include.  Any required units that are not available in the repositories will
	 * be ignored.
	 * <p>
	 * Since there is only one profile per target and the planner and slicer resolve methods are incompatible.
	 * </p>
	 * @param value whether all required units must be available to resolve this container
	 */
	public void setIncludeAllRequired(boolean value) {
		fIncludeAllRequired = value;
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
	 * entire target definition.  
	 * </p>
	 * @param value whether environment specific units should be included
	 */
	public void setIncludeAllEnvironments(boolean value) {
		fIncludeMultipleEnvironments = value;
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
	 * Set whether or not the configuration phase should be executed when installing the IUs
	 * 
	 * @param value whether or not to execute configuration phase
	 */
	public void setIncludeConfigurePhase(boolean value) {
		fIncludeConfigurePhase = value;
	}

	/**
	 * Returns whether or not the configuration phase should be executed when installing the IUs
	 * 
	 * @return whether or not to execute configuration phase
	 */
	public boolean getIncludeConfigurePhase() {
		return fIncludeConfigurePhase;
	}

	/**
	 * Return whether or not the given target has a matching profile that is in sync
	 * @param target the target to check
	 * @return whether or not the target has been resolved at the p2 level
	 */
	public static boolean isResolved(ITargetDefinition target) {
		P2TargetUtils synchronizer = getSynchronizer(target);
		if (synchronizer == null)
			return false;
		try {
			return synchronizer.checkProfile(target);
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Get the synchronizer to use for the given target.  If there is already one on a
	 * container in the target, use that one.  Otherwise, create a new one.  Either way, 
	 * ensure that all other IU containers in the target are using the same synchronizer.
	 * <p>
	 * The synchronizer is an instance of {@link P2TargetUtils} that has access to the 
	 * profile and other p2 bits for the target.
	 * </p>
	 * 
	 * @param target the target for which we are getting the synchronizer
	 * @return the discovered or created synchronizer
	 */
	static synchronized P2TargetUtils getSynchronizer(ITargetDefinition target) {
		P2TargetUtils result = (P2TargetUtils) synchronizers.get(target);
		if (result != null)
			return result;

		result = new P2TargetUtils();
		synchronizers.put(target, result);
		return result;
	}

	/**
	 * Return the set of IUs in all IU containers associated with this synchronizer.
	 * This is a helper method so we don't have to expose the profile itself.
	 * 
	 * @param target the target definition to query
	 * @param monitor the progress monitor to use
	 * @return the set of associated IUs
	 * @throws CoreException if there is a problem discovering the IUs
	 */
	public static IQueryResult getIUs(ITargetDefinition target, IProgressMonitor monitor) throws CoreException {
		P2TargetUtils synchronizer = getSynchronizer(target);
		if (synchronizer == null)
			return null;
		synchronizer.synchronize(target, monitor);
		return synchronizer.getProfile().query(QueryUtil.createIUAnyQuery(), null);
	}

	/**
	 * Synchronize the profile and the target definition managed by this synchronizer.  On return the profile will 
	 * be resolved and correctly match the given target.  The IUBundleContainers associated with 
	 * the target will be notified of any changes in the underlying p2 profile and given an 
	 * opportunity to update themselves accordingly.
	 * 
	 * NOTE: this is a potentially *very* heavyweight operation.
	 * 
	 * NOTE: this method is synchronized as it is effectively a "test and set" caching method. Two
	 * threads getting the profile at the same time should not execute concurrently or the profiles
	 * will get out of sync.
	 * 
	 * @throws CoreException if there was a problem synchronizing
	 */
	public synchronized void synchronize(ITargetDefinition target, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 100);

		// Happiness if we have a profile and it checks out or if we can load one and it checks out.
		if (fProfile == null)
			fProfile = getProfileRegistry().getProfile(getProfileId(target));
		if (fProfile != null && checkProfile(target)) {
			// always push the changes to the target because there can be many target objects
			// for the same synchronizer (doh!)
			notify(target, progress.newChild(25));
			return;
		}

		// Either no profile was found or it was stale.  Delete the current profile and recreate.  
		// This keeps the internal agent data clean and does not cost us much.
		deleteProfile(target.getHandle());
		createProfile(target);

		if (progress.isCanceled())
			return;
		progress.setWorkRemaining(75);

		// Now resolve the profile and refresh the relate IU containers
		if (getIncludeAllRequired())
			resolveWithPlanner(target, progress.newChild(60));
		else
			resolveWithSlicer(target, progress.newChild(60));

		// If we are updating a profile then delete the old snapshot on success.
		notify(target, progress.newChild(15));
		fDirty = false;
	}

	private void createProfile(ITargetDefinition target) throws CoreException, ProvisionException {
		// create a new profile
		IProfileRegistry registry = getProfileRegistry();
		if (registry == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AbstractTargetHandle_0));
		}
		Map properties = new HashMap();
		properties.put(IProfile.PROP_INSTALL_FOLDER, INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
		properties.put(IProfile.PROP_CACHE, BUNDLE_POOL.toOSString());
		properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
		properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties(target));
		properties.put(IProfile.PROP_NL, generateNLProperty(target));
		properties.put(PROP_SEQUENCE_NUMBER, Integer.toString(((TargetDefinition) target).getSequenceNumber()));
		properties.put(PROP_PROVISION_MODE, getProvisionMode(target));
		properties.put(PROP_ALL_ENVIRONMENTS, Boolean.toString(getIncludeAllEnvironments()));
		properties.put(PROP_AUTO_INCLUDE_SOURCE, Boolean.toString(getIncludeSource()));
		properties.put(PROP_INCLUDE_CONFIGURE_PHASE, Boolean.toString(getIncludeConfigurePhase()));
		fProfile = registry.addProfile(getProfileId(target), properties);
	}

	/**
	 * Signal the relevant bundle containers that the given profile has changed.
	 */
	private void notify(ITargetDefinition target, IProgressMonitor monitor) throws CoreException {
		// flush the target caches first since some of them are used in rebuilding
		// the container caches (e.g., featureModels)
		((TargetDefinition) target).flushCaches(P2TargetUtils.BUNDLE_POOL.toOSString());
		// Now proactively recompute all the related container caches.
		ITargetLocation[] containers = target.getTargetLocations();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				ITargetLocation container = containers[i];
				if (container instanceof IUBundleContainer) {
					((IUBundleContainer) container).synchronizerChanged(target);
				}
			}
		}
	}

	/**
	 * Returns the profile identifier for this target handle. There is one profile
	 * per target definition.
	 * 
	 * @return profile identifier
	 * @throws CoreException in unable to generate identifier
	 */
	private static String getProfileId(ITargetHandle handle) throws CoreException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(PROFILE_ID_PREFIX);
		buffer.append(handle.getMemento());
		return buffer.toString();
	}

	/**
	 * Returns the profile identifier for this target handle. There is one profile
	 * per target definition.
	 * 
	 * @return definition the target to lookup
	 * @throws CoreException in unable to generate identifier
	 */
	public static String getProfileId(ITargetDefinition definition) {
		try {
			return getProfileId(definition.getHandle());
		} catch (CoreException e) {
			// gotta make sure that this never happens.  all we're doing here is computing a string.
			return null;
		}
	}

	/**
	 * Returns the profile registry or <code>null</code>
	 * 
	 * @return profile registry or <code>null</code>
	 * @throws CoreException 
	 */
	public static IProfileRegistry getProfileRegistry() throws CoreException {
		IProfileRegistry result = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
		if (result == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_8));
		}
		return result;
	}

	/**
	 * Returns the mode used to provision this target - slice versus plan or <code>null</code> if
	 * this target has no software sites.
	 * 
	 * @return provisioning mode or <code>null</code>
	 */
	private String getProvisionMode(ITargetDefinition target) {
		return getIncludeAllRequired() ? TargetDefinitionPersistenceHelper.MODE_PLANNER : TargetDefinitionPersistenceHelper.MODE_SLICER;
	}

	/**
	 * Returns the metadata repository manager.
	 * 
	 * @return metadata repository manager
	 * @throws CoreException if none
	 */
	public static IMetadataRepositoryManager getRepoManager() throws CoreException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_2));
		}
		return manager;
	}

	/**
	 * Return a queryable on the metadata defined in the given repo locations
	 * 
	 * @param repos the repos to lookup
	 * @param monitor the progress monitor
	 * @return the set of metadata repositories found
	 * @throws CoreException if there is a problem getting the repositories
	 */
	static IQueryable getQueryableMetadata(URI[] repos, IProgressMonitor monitor) throws CoreException {
		IMetadataRepositoryManager manager = getRepoManager();
		if (repos == null) {
			repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		}

		IProgressMonitor loadMonitor = new SubProgressMonitor(monitor, 10);
		int repoCount = repos.length;
		loadMonitor.beginTask(null, repoCount * 10);
		List result = new ArrayList(repoCount);
		MultiStatus repoStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.IUBundleContainer_ProblemsLoadingRepositories, null);
		for (int i = 0; i < repoCount; ++i) {
			try {
				result.add(manager.loadRepository(repos[i], new SubProgressMonitor(loadMonitor, 10)));
			} catch (ProvisionException e) {
				repoStatus.add(e.getStatus());
			}
		}
		loadMonitor.done();

		if (result.size() != repos.length) {
			throw new CoreException(repoStatus);
		}
		if (result.size() == 1) {
			return (IQueryable) result.get(0);
		}
		return QueryUtil.compoundQueryable(result);
	}

	/**
	 * Used to resolve the contents of this container if the user is including all required software.  The p2 planner is used
	 * to determine the complete set of IUs required to run the selected software.  If all requirements are met, the bundles
	 * are downloaded from the repository into the bundle pool and added to the target definition.
	 * 
	 * @param monitor for reporting progress
	 * @throws CoreException if there is a problem with the requirements or there is a problem downloading
	 */
	private void resolveWithPlanner(ITargetDefinition target, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, 200);

		// Get the root IUs for every relevant container in the target definition
		IInstallableUnit[] units = getRootIUs(target);
		if (subMonitor.isCanceled()) {
			return;
		}

		// create the provisioning plan
		IPlanner planner = getPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(fProfile);
		// first remove everything that was explicitly installed.  Then add it back.  This has the net effect of 
		// removing everything that is no longer needed.
		computeRemovals(fProfile, request, getIncludeSource());
		request.addAll(Arrays.asList(units));
		for (int i = 0; i < units.length; i++) {
			IInstallableUnit unit = units[i];
			request.setInstallableUnitProfileProperty(unit, PROP_INSTALLED_IU, Boolean.toString(true));
		}

		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(getMetadataRepositories(target));
		context.setArtifactRepositories(getArtifactRepositories(target));

		if (subMonitor.isCanceled()) {
			return;
		}

		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, subMonitor.newChild(20));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		setPlanProperties(plan, target, TargetDefinitionPersistenceHelper.MODE_PLANNER);
		IProvisioningPlan installerPlan = plan.getInstallerPlan();
		if (installerPlan != null) {
			// this plan requires an update to the installer first, log the fact and attempt
			// to continue, we don't want to update the running SDK while provisioning a target
			PDECore.log(new Status(IStatus.INFO, PDECore.PLUGIN_ID, Messages.IUBundleContainer_6));
		}
		subMonitor.worked(10);
		if (subMonitor.isCanceled()) {
			return;
		}

		// execute the provisioning plan
		IPhaseSet phases = createPhaseSet();
		IEngine engine = getEngine();
		IStatus result = engine.perform(plan, phases, subMonitor.newChild(100));
		if (subMonitor.isCanceled()) {
			return;
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// Now that we have a plan with all the binary and explicit bundles, do a second pass and add 
		// in all the source.
		try {
			planInSourceBundles(fProfile, context, subMonitor.newChild(60));
		} catch (CoreException e) {
			// XXX Review required: is adding in the source critical or optional?
			// We failed adding in the source so remove the intermediate profile and rethrow
			getProfileRegistry().removeProfile(fProfile.getProfileId(), fProfile.getTimestamp());
			throw e;
		}
	}

	private void setPlanProperties(IProvisioningPlan plan, ITargetDefinition definition, String mode) {
		plan.setProfileProperty(PROP_PROVISION_MODE, mode);
		plan.setProfileProperty(PROP_ALL_ENVIRONMENTS, Boolean.toString(getIncludeAllEnvironments()));
		plan.setProfileProperty(PROP_AUTO_INCLUDE_SOURCE, Boolean.toString(getIncludeSource()));
		plan.setProfileProperty(PROP_INCLUDE_CONFIGURE_PHASE, Boolean.toString(getIncludeConfigurePhase()));
		plan.setProfileProperty(PROP_SEQUENCE_NUMBER, Integer.toString(((TargetDefinition) definition).getSequenceNumber()));
	}

	/**
	 * @return the phase set to execute, includes the configuration phase if {@link #getIncludeConfigurePhase()} is <code>true<code>
	 */
	private IPhaseSet createPhaseSet() {
		ArrayList phases = new ArrayList(4);
		phases.add(new Collect(100));
		phases.add(new Property(1));
		phases.add(new Uninstall(50, true));
		phases.add(new Install(50));
		phases.add(new CollectNativesPhase(100));
		if (getIncludeConfigurePhase()) {
			phases.add(new Configure(100));
		}

		return new PhaseSet((Phase[]) phases.toArray(new Phase[phases.size()]));
	}

	/** 
	 * Update the given change request to remove anything that was explicitly installed
	 * including the internal source IU.  
	 */
	private void computeRemovals(IProfile profile, IProfileChangeRequest request, boolean includeSource) {
		// if include source is off then ensure that the source IU is removed.
		if (!includeSource) {
			IInstallableUnit sourceIU = getCurrentSourceIU(profile);
			if (sourceIU != null)
				request.remove(sourceIU);
		}
		// remove everything that is marked as roots.  The plan will have the new roots added in anyway.
		IQuery query = new IUProfilePropertyQuery(PROP_INSTALLED_IU, Boolean.toString(true));
		IQueryResult installedIUs = profile.query(query, null);
		request.removeAll(installedIUs.toSet());
	}

	// run a second pass of the planner to add in the source bundles for everything that's
	// in the current profile.
	private void planInSourceBundles(IProfile fProfile, ProvisioningContext context, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.P2TargetUtils_ProvisioningSourceTask, 100);

		// create an IU that optionally and greedily requires the related source bundles.
		// Completely replace any source IU that may already be in place
		IInstallableUnit currentSourceIU = getCurrentSourceIU(fProfile);

		// determine the new version number.  start at 1
		Version sourceVersion = Version.createOSGi(1, 0, 0);
		if (currentSourceIU != null) {
			Integer major = (Integer) currentSourceIU.getVersion().getSegment(0);
			sourceVersion = Version.createOSGi(major.intValue() + 1, 0, 0);
		}
		IInstallableUnit sourceIU = createSourceIU(fProfile, sourceVersion);

		// call the planner again to add in the new source IU and all available source bundles
		IPlanner planner = getPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(fProfile);
		if (currentSourceIU != null)
			request.remove(currentSourceIU);
		request.add(sourceIU);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, subMonitor.newChild(25));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		if (subMonitor.isCanceled()) {
			return;
		}

		// execute the provisioning plan
		long oldTimestamp = fProfile.getTimestamp();
		IPhaseSet phases = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE, PhaseSetFactory.PHASE_UNINSTALL});
		IEngine engine = getEngine();
		plan.setProfileProperty(PROP_PROVISION_MODE, TargetDefinitionPersistenceHelper.MODE_PLANNER);
		plan.setProfileProperty(PROP_ALL_ENVIRONMENTS, Boolean.toString(false));
		IStatus result = engine.perform(plan, phases, subMonitor.newChild(75));

		if (subMonitor.isCanceled()) {
			return;
		}
		if (!result.isOK()) {
			throw new CoreException(result);
		}

		// remove the old (intermediate) profile version now we have a new one with source.
		getProfileRegistry().removeProfile(fProfile.getProfileId(), oldTimestamp);
	}

	// Create and return an IU that has optional and greedy requirements on all source bundles
	// related to bundle IUs in the given queryable. 
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
				// use fully qualified name to avoid conflict with other VersionRange class
				org.eclipse.equinox.p2.metadata.VersionRange range = new org.eclipse.equinox.p2.metadata.VersionRange(version, true, version, true);
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

	// Lookup and return (if any) the source IU in the given queryable.
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
	 * @param monitor for reporting progress
	 * @throws CoreException if there is a problem interacting with the repositories
	 */
	private void resolveWithSlicer(ITargetDefinition target, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, 100);

		// resolve IUs
		IInstallableUnit[] units = getRootIUs(target);
		if (subMonitor.isCanceled()) {
			return;
		}

		URI[] repositories = getMetadataRepositories(target);
		int repoCount = repositories.length;
		if (repoCount == 0) {
			return;
		}
		IQueryable allMetadata = getQueryableMetadata(repositories, subMonitor.newChild(10));

		// do an initial slice to add everything the user requested
		IQueryResult queryResult = slice(units, allMetadata, target, subMonitor.newChild(10));
		if (subMonitor.isCanceled() || queryResult == null || queryResult.isEmpty()) {
			return;
		}

		// If we are including source then create a source IU to bring in the relevant source
		// bundles and run the slicer again.
		if (getIncludeSource()) {
			// Build an IU that represents all the source bundles and slice again to add them in if available
			IInstallableUnit sourceIU = createSourceIU(queryResult, Version.createOSGi(1, 0, 0));
			IInstallableUnit[] units2 = new IInstallableUnit[units.length + 1];
			System.arraycopy(units, 0, units2, 0, units.length);
			units2[units.length] = sourceIU;

			queryResult = slice(units2, allMetadata, target, subMonitor.newChild(10));
			if (subMonitor.isCanceled() || queryResult == null || queryResult.isEmpty()) {
				return;
			}
		}

		IEngine engine = getEngine();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(repositories);
		context.setArtifactRepositories(getArtifactRepositories(target));
		IProvisioningPlan plan = engine.createPlan(fProfile, context);
		setPlanProperties(plan, target, TargetDefinitionPersistenceHelper.MODE_SLICER);

		Set newSet = queryResult.toSet();
		Iterator itor = newSet.iterator();
		while (itor.hasNext()) {
			plan.addInstallableUnit((IInstallableUnit) itor.next());
		}
		for (int i = 0; i < units.length; i++) {
			IInstallableUnit unit = units[i];
			plan.setInstallableUnitProfileProperty(unit, PROP_INSTALLED_IU, Boolean.toString(true));
		}

		// remove all units that are in the current profile but not in the new slice
		Set toRemove = fProfile.query(QueryUtil.ALL_UNITS, null).toSet();
		toRemove.removeAll(newSet);
		for (Iterator i = toRemove.iterator(); i.hasNext();) {
			plan.removeInstallableUnit((IInstallableUnit) i.next());
		}

		if (subMonitor.isCanceled()) {
			return;
		}
		subMonitor.worked(10);

		// execute the provisioning plan
		IPhaseSet phases = createPhaseSet();
		IStatus result = engine.perform(plan, phases, subMonitor.newChild(60));
		if (!result.isOK()) {
			throw new CoreException(result);
		}
	}

	/**
	 * Sets up a slice operation to download the set of installable units that are both required
	 * by the provided root IUs and available in the repositories specified in the metadata.
	 * 
	 * @param units The set of root IUs to search for dependencies of in the repositories
	 * @param allMetadata metadata describing the repositories where the slicer can search
	 * @param definition the target definition this operation is being executed for
	 * @param monitor progress monitor, done will not be called
	 * @return the result of the slice operation
	 * @throws CoreException if a problem occurs during the slice operation that should stop this location from resolving
	 */
	private IQueryResult slice(IInstallableUnit[] units, IQueryable allMetadata, ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		// slice IUs and all prerequisites
		PermissiveSlicer slicer = null;
		if (getIncludeAllEnvironments()) {
			slicer = new PermissiveSlicer(allMetadata, new HashMap(), true, false, true, true, false);
		} else {
			Map props = new HashMap();
			props.put("osgi.os", definition.getOS() != null ? definition.getOS() : Platform.getOS()); //$NON-NLS-1$
			props.put("osgi.ws", definition.getWS() != null ? definition.getWS() : Platform.getWS()); //$NON-NLS-1$
			props.put("osgi.arch", definition.getArch() != null ? definition.getArch() : Platform.getOSArch()); //$NON-NLS-1$
			props.put("osgi.nl", definition.getNL() != null ? definition.getNL() : Platform.getNL()); //$NON-NLS-1$
			props.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
			slicer = new PermissiveSlicer(allMetadata, props, true, false, false, true, false);
		}
		IQueryable slice = slicer.slice(units, subMonitor.newChild(50));
		IStatus sliceStatus = slicer.getStatus();
		// If the slicer encounters an error, stop the operation
		if (sliceStatus.getSeverity() == IStatus.ERROR) {
			throw new CoreException(sliceStatus);
		}

		// Collect the IUs from the sliced
		IQueryResult queryResult = null;
		if (slice != null)
			queryResult = slice.query(QueryUtil.createIUAnyQuery(), subMonitor.newChild(50));

		// If the slicer encounters a non-error status, only report it if the slice returned no IU results
		// It would be better to inform the user, but we do not want to stop the location from resolving (bug 350772)
		if (!sliceStatus.isOK()) {
			if (!queryResult.iterator().hasNext()) {
				throw new CoreException(sliceStatus);
			}
		}

		return queryResult;
	}

	/**
	 * Returns the artifact repositories to consider when getting artifacts.  Returns a default set of
	 * repositories if current repository settings are <code>null</code>).
	 *  
	 * @return URI's of repositories to use when getting artifacts
	 * @exception CoreException
	 */
	private URI[] getArtifactRepositories(ITargetDefinition target) throws CoreException {
		Set result = new HashSet();
		ITargetLocation[] containers = target.getTargetLocations();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		for (int i = 0; i < containers.length; i++) {
			ITargetLocation container = containers[i];
			if (container instanceof IUBundleContainer) {
				URI[] repos = ((IUBundleContainer) container).getRepositories();
				if (repos == null) {
					repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
				}
				result.addAll(Arrays.asList(repos));
			}
		}
		if (useAdditionalLocalArtifacts()) {
			// get all the artifact repos we know in the manager currently
			result.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)));

			// Add in the IDE profile bundle pool and all known workspaces
			findProfileRepos(result);
			findWorkspaceRepos(result);
		}
		return (URI[]) result.toArray(new URI[result.size()]);
	}

	/**
	 * return whether or not to use local artifact repositories when provisioning the target
	 */
	private boolean useAdditionalLocalArtifacts() {
		// XXX consider using a preference here or another strategy if users are able to spec 
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
		IPreferencesService prefs = getPreferences();
		if (prefs == null)
			return;
		String recent = prefs.getString("org.eclipse.ui.ide", "RECENT_WORKSPACES", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		if (recent == null)
			return;
		String[] recents = recent.split("\n"); //$NON-NLS-1$
		for (int i = 0; i < recents.length; i++) {
			File bundlePool = new File(recents[i] + "/.metadata/.plugins/org.eclipse.pde.core/.bundle_pool"); //$NON-NLS-1$
			if (bundlePool.exists()) {
				additionalRepos.add(bundlePool.toURI().normalize());
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
			IProfileRegistry profileRegistry = (IProfileRegistry) getGlobalAgent().getService(IProfileRegistry.SERVICE_NAME);
			if (profileRegistry == null)
				return;
			IProfile self = profileRegistry.getProfile(IProfileRegistry.SELF);
			if (self == null)
				return;

			IAgentLocation location = (IAgentLocation) getGlobalAgent().getService(IAgentLocation.SERVICE_NAME);
			URI dataArea = location.getDataArea("org.eclipse.equinox.p2.engine"); //$NON-NLS-1$
			dataArea = URIUtil.append(dataArea, "profileRegistry/" + self.getProfileId() + ".profile"); //$NON-NLS-1$//$NON-NLS-2$
			ProfileMetadataRepository profileRepo = new ProfileMetadataRepository(getGlobalAgent(), dataArea, null);
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
	 * Returns the IU's for the given target related to the given containers
	 * 
	 * @param containers the bundle containers to filter with
	 * @return the discovered IUs
	 * @exception CoreException if unable to retrieve IU's
	 */
	private IInstallableUnit[] getRootIUs(ITargetDefinition definition) throws CoreException {
		HashSet result = new HashSet();
		ITargetLocation[] containers = definition.getTargetLocations();
		for (int i = 0; i < containers.length; i++) {
			ITargetLocation container = containers[i];
			if (container instanceof IUBundleContainer) {
				IUBundleContainer iuContainer = (IUBundleContainer) container;
				IQueryable repos = getQueryableMetadata(iuContainer.getRepositories(), new NullProgressMonitor());
				String[] ids = iuContainer.getIds();
				Version[] versions = iuContainer.getVersions();
				for (int j = 0; j < ids.length; j++) {
					IQuery query = QueryUtil.createIUQuery(ids[j], versions[j]);
					IQueryResult queryResult = repos.query(query, null);
					if (queryResult.isEmpty())
						throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.IUBundleContainer_1, ids[j])));
					result.add(queryResult.iterator().next());
				}
			}
		}
		return (IInstallableUnit[]) result.toArray(new IInstallableUnit[result.size()]);
	}

	/**
	 * Returns the repositories to consider when resolving IU's (will return default set of
	 * repositories if current repository settings are <code>null</code>).
	 *  
	 * @return URI's of repositories to use when resolving bundles
	 * @exception CoreException
	 */
	private URI[] getMetadataRepositories(ITargetDefinition target) throws CoreException {
		Set result = new HashSet();
		ITargetLocation[] containers = target.getTargetLocations();
		IMetadataRepositoryManager manager = getRepoManager();
		for (int i = 0; i < containers.length; i++) {
			ITargetLocation container = containers[i];
			if (container instanceof IUBundleContainer) {
				URI[] repos = ((IUBundleContainer) container).getRepositories();
				if (repos == null) {
					repos = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
				}
				result.addAll(Arrays.asList(repos));
			}
		}
		return (URI[]) result.toArray(new URI[result.size()]);
	}

	private static final String NATIVE_ARTIFACTS = "nativeArtifacts"; //$NON-NLS-1$
	private static final String NATIVE_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$

	protected static class CollectNativesAction extends ProvisioningAction {
		public IStatus execute(Map parameters) {
			InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
			IInstallableUnit installableUnit = operand.second();
			if (installableUnit == null)
				return Status.OK_STATUS;

			IArtifactRepositoryManager manager;
			try {
				Collection toDownload = installableUnit.getArtifacts();
				if (toDownload == null)
					return Status.OK_STATUS;

				List artifactRequests = (List) parameters.get(NATIVE_ARTIFACTS);
				IArtifactRepository destinationArtifactRepository = getBundlePool();
				manager = getArtifactRepositoryManager();
				for (Iterator i = toDownload.iterator(); i.hasNext();) {
					IArtifactKey keyToDownload = (IArtifactKey) i.next();
					IArtifactRequest request = manager.createMirrorRequest(keyToDownload, destinationArtifactRepository, null, null);
					artifactRequests.add(request);
				}
			} catch (CoreException e) {
				return e.getStatus();
			}
			return Status.OK_STATUS;
		}

		public IStatus undo(Map parameters) {
			// nothing to do for now
			return Status.OK_STATUS;
		}
	}

	protected static class CollectNativesPhase extends InstallableUnitPhase {
		public CollectNativesPhase(int weight) {
			super(NATIVE_ARTIFACTS, weight);
		}

		protected List getActions(InstallableUnitOperand operand) {
			IInstallableUnit unit = operand.second();
			if (unit != null && unit.getTouchpointType().getId().equals(NATIVE_TYPE)) {
				return Collections.singletonList(new CollectNativesAction());
			}
			return null;
		}

		protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
			parameters.put(NATIVE_ARTIFACTS, new ArrayList());
			parameters.put(PARM_PROFILE, profile);
			return null;
		}

		protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
			List artifactRequests = (List) parameters.get(NATIVE_ARTIFACTS);
			ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
			IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
			DownloadManager dm = new DownloadManager(context, agent);
			for (Iterator i = artifactRequests.iterator(); i.hasNext();) {
				dm.add((IArtifactRequest) i.next());
			}
			return dm.start(monitor);
		}
	}

	/**
	 * @return the profile associated with this synchronizer
	 */
	IProfile getProfile() {
		return fProfile;
	}

//	/**
//	 * @return the target definition associated with this synchronizer
//	 */
//	ITargetDefinition getTargetDefinition() {
//		return fTarget;
//	}

	void markDirty() {
		fDirty = true;
	}
}
