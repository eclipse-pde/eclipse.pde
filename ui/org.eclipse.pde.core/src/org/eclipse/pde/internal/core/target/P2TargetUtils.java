/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Inc. and others.
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
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.osgi.framework.*;

public class P2TargetUtils {

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
				if (id.startsWith(P2TargetUtils.PROFILE_ID_PREFIX)) {
					String memento = id.substring(P2TargetUtils.PROFILE_ID_PREFIX.length());
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
				if (profiles[i].getProfileId().startsWith(P2TargetUtils.PROFILE_ID_PREFIX)) {
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
	private static String generateEnvironmentProperties(ITargetDefinition target) {
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
	private static String generateNLProperty(ITargetDefinition target) {
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
			IProvisioningAgent result = provider.createAgent(AGENT_LOCATION);
			// turn off the garbage collector for the PDE agent.  GC is managed on a coarser grain
			getGarbageCollector().stop();
			return result;
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
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_10));
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
	 * Returns the local bundle pool (repository) where bundles are stored for the
	 * given profile.
	 * 
	 * @param profile profile bundles are stored
	 * @return local file artifact repository
	 * @throws CoreException
	 */
	public static IFileArtifactRepository getBundlePool(IProfile profile) throws CoreException {
		String path = profile.getProperty(IProfile.PROP_CACHE);
		if (path == null) {
			// We should always be setting the bundle pool, so if the bundle pool location is missing there isn't much we can do 
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.IUBundleContainer_NoBundlePool));
		}
		URI uri = new File(path).toURI();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		return (IFileArtifactRepository) manager.loadRepository(uri, null);
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
		IPlanner planner = (IPlanner) P2TargetUtils.getAgent().getService(IPlanner.class.getName());
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
	 * Returns the profile for the this target handle, creating one if required.
	 * 
	 * @return profile
	 * @throws CoreException in unable to retrieve profile
	 */
	public static IProfile getProfile(ITargetDefinition target) throws CoreException {
		IProfileRegistry registry = getProfileRegistry();
		if (registry == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AbstractTargetHandle_0));
		}
		AbstractTargetHandle handle = ((AbstractTargetHandle) target.getHandle());
		String id = P2TargetUtils.getProfileId(handle);
		IProfile profile = registry.getProfile(id);
		if (profile != null) {
			boolean recreate = false;
			// check if all environments setting is the same
			boolean all = false;
			String value = profile.getProperty(P2TargetUtils.PROP_ALL_ENVIRONMENTS);
			if (value != null) {
				all = Boolean.valueOf(value).booleanValue();
				if (!Boolean.toString(isAllEnvironments(target)).equals(value)) {
					recreate = true;
				}
			}
			// ensure environment & NL settings are still the same (else we need a new profile)
			String property = null;
			if (!recreate && !all) {
				property = generateEnvironmentProperties(target);
				value = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
				if (!property.equals(value)) {
					recreate = true;
				}
			}
			// check provisioning mode: slice versus plan
			String mode = getProvisionMode(target);
			if (mode != null) {
				value = profile.getProperty(P2TargetUtils.PROP_PROVISION_MODE);
				if (!mode.equals(value)) {
					recreate = true;
				}
			}

			if (!recreate) {
				property = generateNLProperty(target);
				value = profile.getProperty(IProfile.PROP_NL);
				if (!property.equals(value)) {
					recreate = true;
				}
			}
			if (!recreate) {
				// check top level IU's. If any have been removed from the containers that are
				// still in the profile, we need to recreate (rather than uninstall)
				IUProfilePropertyQuery propertyQuery = new IUProfilePropertyQuery(P2TargetUtils.PROP_INSTALLED_IU, Boolean.toString(true));
				IQueryResult queryResult = profile.query(propertyQuery, null);
				Iterator iterator = queryResult.iterator();
				if (iterator.hasNext()) {
					Set installedIUs = new HashSet();
					while (iterator.hasNext()) {
						IInstallableUnit unit = (IInstallableUnit) iterator.next();
						installedIUs.add(new NameVersionDescriptor(unit.getId(), unit.getVersion().toString()));
					}
					IBundleContainer[] containers = target.getBundleContainers();
					if (containers != null) {
						for (int i = 0; i < containers.length; i++) {
							if (containers[i] instanceof IUBundleContainer) {
								IUBundleContainer bc = (IUBundleContainer) containers[i];
								String[] ids = bc.getIds();
								Version[] versions = bc.getVersions();
								for (int j = 0; j < versions.length; j++) {
									installedIUs.remove(new NameVersionDescriptor(ids[j], versions[j].toString()));
								}
							}
						}
					}
					if (!installedIUs.isEmpty()) {
						recreate = true;
					}
				}
			}
			if (recreate) {
				P2TargetUtils.deleteProfile(handle);
				profile = null;
			}
		}
		if (profile == null) {
			// create profile
			Map properties = new HashMap();
			properties.put(IProfile.PROP_INSTALL_FOLDER, P2TargetUtils.INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
			properties.put(IProfile.PROP_CACHE, P2TargetUtils.BUNDLE_POOL.toOSString());
			properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
			// set up environment & NL properly so OS specific fragments are down loaded/installed
			properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties(target));
			properties.put(IProfile.PROP_NL, generateNLProperty(target));
			String mode = getProvisionMode(target);
			if (mode != null) {
				properties.put(P2TargetUtils.PROP_PROVISION_MODE, mode);
				properties.put(P2TargetUtils.PROP_ALL_ENVIRONMENTS, Boolean.toString(isAllEnvironments(target)));
			}
			profile = registry.addProfile(id, properties);
		}
		return profile;
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
	private static String getProvisionMode(ITargetDefinition target) {
		IBundleContainer[] containers = target.getBundleContainers();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof IUBundleContainer) {
					IUBundleContainer iu = (IUBundleContainer) containers[i];
					if (iu.getIncludeAllRequired()) {
						return TargetDefinitionPersistenceHelper.MODE_PLANNER;
					}
					return TargetDefinitionPersistenceHelper.MODE_SLICER;
				}
			}
		}
		return null;
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
	 * Returns the metadata repository with the given URI.
	 * 
	 * @param uri location
	 * @return repository
	 * @throws CoreException
	 */
	public static IMetadataRepository getRepository(URI uri) throws CoreException {
		IMetadataRepositoryManager manager = getRepoManager();
		IMetadataRepository repo = manager.loadRepository(uri, null);
		return repo;
	}

	/**
	 * Returns whether software site containers are configured to provision for all environments
	 * versus a single environment.
	 * 
	 * @return whether all environments will be provisioned
	 */
	private static boolean isAllEnvironments(ITargetDefinition target) {
		IBundleContainer[] containers = target.getBundleContainers();
		if (containers != null) {
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof IUBundleContainer) {
					IUBundleContainer iu = (IUBundleContainer) containers[i];
					if (iu.getIncludeAllEnvironments()) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
