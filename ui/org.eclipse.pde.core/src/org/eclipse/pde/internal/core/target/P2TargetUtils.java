/*******************************************************************************
 * Copyright (c) 2010, 2024 EclipseSource Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Inc. - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *     Manumitting Technologies Inc - Bug 437726
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import static org.eclipse.pde.internal.core.target.IUBundleContainer.queryFirst;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IPhaseSet;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer.UnitDeclaration;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class P2TargetUtils {

	private static final String SOURCE_IU_ID = "org.eclipse.pde.core.target.source.bundles"; //$NON-NLS-1$

	/**
	 * URI to the local directory where the p2 agent keeps its information.
	 */
	public static URI AGENT_LOCATION;
	static {
		try {
			AGENT_LOCATION = PDECore.getDefault().getStateLocation().append(".p2").toPath().toUri(); //$NON-NLS-1$
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
	 * Installable unit property to store the version-specifications of
	 * root/installed IU's that are declared in the target container as a
	 * semicolon separated list.
	 */
	static final String PROP_IU_VERSION_DECLARATION = PDECore.PLUGIN_ID + ".iu_version_declaration"; //$NON-NLS-1$

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
	 * Profile property that tracks whether or not repository references should
	 * be followed when installing
	 */
	static final String PROP_FOLLOW_REPOSITORY_REFERENCES = PDECore.PLUGIN_ID + ".followRepositoryReferences"; //$NON-NLS-1$

	/**
	 * Profile property that keeps track the list of repositories declared in a
	 * target definition, separated by the {@link #REPOSITORY_LIST_DELIMITER
	 * §§§} character.
	 */
	static final String PROP_DECLARED_REPOSITORIES = PDECore.PLUGIN_ID + ".repositories"; //$NON-NLS-1$

	/**
	 * Table mapping {@link ITargetDefinition} to synchronizer (P2TargetUtils) instance.
	 */
	private static final Map<ITargetDefinition, P2TargetUtils> SYNCHRONIZERS = new WeakHashMap<>();

	/**
	 * Table mapping of  ITargetDefinition and IFileArtifactRepository
	 */
	static final Map<ITargetDefinition, IFileArtifactRepository> fgTargetArtifactRepo = new ConcurrentHashMap<>();

	/**
	 * Table mapping IArtifactKey to table map of IFileArtifactRepository and IFileArtifactRepository
	 */
	static final Map<IArtifactKey, Map<IFileArtifactRepository, File>> fgArtifactKeyRepoFile = new ConcurrentHashMap<>();

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
	 * by the slicer so {@link IUBundleContainer#INCLUDE_REQUIRED} must be turned off for this setting to be used.
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
	 * Whether repository references should be resolved. If this option is
	 * false, references will be skipped entirely. In essence, this forces
	 * repositories to be self-contained.
	 *
	 * <p>
	 * <code>true</code> by default
	 * </p>
	 */
	private boolean fFollowRepositoryReferences = true;

	/**
	 * Deletes any profiles associated with target definitions that no longer
	 * exist and returns a list of profile identifiers that were deleted.
	 */
	public static List<String> cleanOrphanedTargetDefinitionProfiles() throws CoreException {
		List<String> list = new ArrayList<>();
		ITargetPlatformService tps = TargetPlatformService.getDefault();
		if (tps != null) {
			IProfile[] profiles = getProfileRegistry().getProfiles();
			for (IProfile profile : profiles) {
				String id = profile.getProfileId();
				if (id.startsWith(PROFILE_ID_PREFIX)) {
					String memento = id.substring(PROFILE_ID_PREFIX.length());
					try {
						ITargetHandle handle = tps.getTarget(memento);
						if (handle.exists()) {
							continue;
						}
					} catch (CoreException e) {
						// don't break the chain here, but delete the profile as
						// it seems to be invalid now
					}
					deleteProfileWithId(id);
					list.add(id);
				}
			}
		}
		return list;
	}

	/**
	 * Deletes the profile associated with this target handle, if any. Returns
	 * <code>true</code> if a profile existed and was deleted, otherwise <code>false</code>.
	 *
	 * @throws CoreException if unable to delete the profile
	 */
	public static void deleteProfile(ITargetHandle handle) throws CoreException {
		deleteProfileWithId(getProfileId(handle));
	}

	private static void deleteProfileWithId(String profileId) throws CoreException {
		IProfileRegistry registry = getProfileRegistry();
		IProfile profile = registry.getProfile(profileId);
		if (profile != null) {
			String location = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
			registry.removeProfile(profileId);
			if (location != null && location.length() > 0) {
				File folder = new File(location);
				CoreUtility.deleteContent(folder);
			}
		}
	}

	public static void forceCheckTarget(final ITargetDefinition target) {
		final P2TargetUtils result = getSynchronizer(target);
		result.resetProfile();
	}

	@SuppressWarnings("restriction")
	private synchronized void resetProfile() {
		if (getProfile() instanceof org.eclipse.equinox.internal.p2.engine.Profile profile) {
			profile.setProperty(PROP_SEQUENCE_NUMBER, "-1"); //$NON-NLS-1$
		}
		fProfile = null;
	}

	/**
	 * Performs garbage collection based on remaining profiles. Should be called to avoid
	 * having PDE's bundle pool area grow unbounded.
	 */
	public static void garbageCollect() {
		try {
			IProfile[] profiles = getProfileRegistry().getProfiles();
			for (IProfile profile : profiles) {
				if (profile.getProfileId().startsWith(PROFILE_ID_PREFIX)) {
					getGarbageCollector().runGC(profile);
				}
			}
		} catch (CoreException e) {
			// XXX likely should log something here.
		}
	}

	/**
	 * Generates the environment properties string for this target definition's p2 profile.
	 *
	 * @return environment properties
	 */
	private String generateEnvironmentProperties(ITargetDefinition target) {
		StringBuilder env = new StringBuilder();
		appendEnv(env, ICoreConstants.OSGI_WS, target.getWS(), Platform::getWS);
		env.append(","); //$NON-NLS-1$
		appendEnv(env, ICoreConstants.OSGI_OS, target.getOS(), Platform::getOS);
		env.append(","); //$NON-NLS-1$
		appendEnv(env, ICoreConstants.OSGI_ARCH, target.getArch(), Platform::getOSArch);
		return env.toString();
	}

	private void appendEnv(StringBuilder env, String key, String value, Supplier<String> defaultValue) {
		env.append(key).append('=').append(value != null ? value : defaultValue.get());
	}

	/**
	 * Generates the NL property for this target definition's p2 profile.
	 *
	 * @return NL profile property
	 */
	private String generateNLProperty(ITargetDefinition target) {
		String nl = target.getNL();
		return nl != null ? nl : Platform.getNL();
	}

	public static IProvisioningAgent getAgent() throws CoreException {
		//Is there already an agent for this location?
		String filter = "(locationURI=" + AGENT_LOCATION + ")"; //$NON-NLS-1$//$NON-NLS-2$
		ServiceReference<IProvisioningAgent> reference = null;
		BundleContext context = PDECore.getDefault().getBundleContext();
		try {
			Collection<ServiceReference<IProvisioningAgent>> serviceReferences = context
					.getServiceReferences(IProvisioningAgent.class, filter);
			if (!serviceReferences.isEmpty()) {
				reference = serviceReferences.iterator().next();
				return context.getService(reference);
			}
		} catch (InvalidSyntaxException e) {
			// ignore
		} finally {
			if (reference != null) {
				context.ungetService(reference);
			}
		}

		IProvisioningAgentProvider provider = PDECore.getDefault().acquireService(IProvisioningAgentProvider.class);
		try {
			if (provider == null) {
				throw new CoreException(Status.error(Messages.IUBundleContainer_7));
			}
			IProvisioningAgent agent = provider.createAgent(AGENT_LOCATION);
			if (agent == null) {
				throw new CoreException(Status.error(Messages.IUBundleContainer_7));
			}
			// turn off the garbage collector for the PDE agent.  GC is managed on a coarser grain
			GarbageCollector garbageCollector = agent.getService(GarbageCollector.class);
			if (garbageCollector != null) {
				garbageCollector.stop();
			}
			return agent;
		} catch (ProvisionException e) {
			throw new CoreException(Status.error(Messages.IUBundleContainer_7, e));
		}
	}

	/**
	 * Returns the global p2 provisioning agent.  This is useful when looking to inherit or use
	 * some settings from the global p2 world.
	 *
	 * @return the global p2 provisioning agent
	 */
	public static IProvisioningAgent getGlobalAgent() throws CoreException {
		IProvisioningAgent agent = PDECore.getDefault().acquireService(IProvisioningAgent.class);
		if (agent == null) {
			throw new CoreException(Status.error(Messages.IUBundleContainer_11));
		}
		return agent;
	}

	/**
	 * Returns the provisioning agent location service.
	 *
	 * @return provisioning agent location service
	 * @throws CoreException if none
	 */
	public static IAgentLocation getAgentLocation() throws CoreException {
		return getP2Service(IAgentLocation.class, Messages.IUBundleContainer_10);
	}

	/**
	 * Returns the provisioning engine service.
	 *
	 * @return provisioning engine
	 * @throws CoreException if none
	 */
	public static IArtifactRepositoryManager getArtifactRepositoryManager() throws CoreException {
		return getP2Service(IArtifactRepositoryManager.class, Messages.IUBundleContainer_3);
	}

	/**
	 * Returns the local bundle pool (repository) where bundles are stored
	 *
	 * @return local file artifact repository
	 */
	public static IFileArtifactRepository getBundlePool() throws CoreException {
		URI uri = BUNDLE_POOL.toFile().toURI();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		try {
			if (manager.contains(uri)) {
				return (IFileArtifactRepository) manager.loadRepository(uri, null);
			}
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
		return getP2Service(IEngine.class, Messages.IUBundleContainer_4);
	}

	/**
	 * Returns the p2 garbage collector
	 *
	 * @return p2 garbage collector
	 * @throws CoreException if none
	 */
	public static GarbageCollector getGarbageCollector() throws CoreException {
		return getP2Service(GarbageCollector.class, Messages.IUBundleContainer_9);
	}

	/**
	 * Returns the provisioning planner.
	 *
	 * @return provisioning planner
	 * @throws CoreException if none
	 */
	public static IPlanner getPlanner() throws CoreException {
		return getP2Service(IPlanner.class, Messages.IUBundleContainer_5);
	}

	/**
	 * Returns the preferences service.
	 *
	 * @return preferences service or null if none
	 */
	public static IPreferencesService getPreferences() {
		return PDECore.getDefault().acquireService(IPreferencesService.class);
	}

	/**
	 * Returns whether the contents of the profile matches the expected contents of the target definition
	 *
	 * @return whether or not the profile and target definitions match
	 */
	private boolean checkProfile(ITargetDefinition target, final IProfile profile) {
		// make sure we have a profile to validate
		if (profile == null) {
			return false;
		}

		// check that the target and profiles are in sync. If they are then life is good.
		// If they are not equal, there is still a chance that everything is ok.
		String profileNumber = profile.getProperty(PROP_SEQUENCE_NUMBER);
		if (Integer.toString(((TargetDefinition) target).getSequenceNumber()).equals(profileNumber)) {
			return true;
		}

		// check if all environments setting is the same
		boolean all = false;
		String value = profile.getProperty(PROP_ALL_ENVIRONMENTS);
		if (value != null) {
			all = Boolean.parseBoolean(value);
			if (!Boolean.toString(getIncludeAllEnvironments()).equals(value)) {
				return false;
			}
		}
		// ensure environment & NL settings are still the same (else we need a new profile)
		if (!all && !generateEnvironmentProperties(target).equals(profile.getProperty(IProfile.PROP_ENVIRONMENTS))) {
			return false;
		}
		if (!generateNLProperty(target).equals(profile.getProperty(IProfile.PROP_NL))) {
			return false;
		}
		// check provisioning mode: slice versus plan
		if (!getProvisionMode().equals(profile.getProperty(PROP_PROVISION_MODE))) {
			return false;
		}
		if (getIncludeSource() != Boolean.parseBoolean(profile.getProperty(PROP_AUTO_INCLUDE_SOURCE))) {
			return false;
		}
		if (getIncludeConfigurePhase() != Boolean.parseBoolean(profile.getProperty(PROP_INCLUDE_CONFIGURE_PHASE))) {
			return false;
		}
		if (isFollowRepositoryReferences() != Boolean
				.parseBoolean((profile.getProperty(PROP_FOLLOW_REPOSITORY_REFERENCES)))) {
			return false;
		}

		List<IUBundleContainer> iuContainers = iuBundleContainersOf(target).toList();

		// ensure list of repositories is still the same. If empty versions or
		// version ranges are used, just changing the repos can change content
		String recordedRepositories = profile.getProperty(PROP_DECLARED_REPOSITORIES);
		Set<URI> declaredRepositories = iuContainers.stream().map(IUBundleContainer::getRepositories)
				.flatMap(List::stream).collect(Collectors.toSet());
		if (recordedRepositories != null && !decodeURIs(recordedRepositories).equals(declaredRepositories)) {
			return false;
		}

		// check top level IU's. If any have been removed from the containers that are
		// still in the profile, we need to recreate (rather than uninstall)
		IUProfilePropertyQuery propertyQuery = new IUProfilePropertyQuery(PROP_INSTALLED_IU, Boolean.toString(true));
		IQueryResult<IInstallableUnit> queryResult = profile.query(propertyQuery, null);

		// Check if each installed/root IU can be matched with exactly one
		// IU-declaration. If not, the profile is not in sync anymore.
		Map<String, Set<VersionRange>> installedIUs = new HashMap<>();
		for (IInstallableUnit unit : queryResult) {
			Set<VersionRange> declarations = installedIUs.computeIfAbsent(unit.getId(), id -> new HashSet<>(1));
			String declaredVersions = profile.getInstallableUnitProperty(unit, PROP_IU_VERSION_DECLARATION);
			parseVersions(unit, declaredVersions).forEach(declarations::add);
		}
		Map<String, Set<VersionRange>> declaredIUs = iuContainers.stream() //
				.map(IUBundleContainer::getDeclaredUnits).flatMap(Collection::stream) //
				.collect(Collectors.groupingBy(UnitDeclaration::id,
						Collectors.mapping(UnitDeclaration::version, Collectors.toSet())));

		return installedIUs.equals(declaredIUs);
	}

	private Stream<IUBundleContainer> iuBundleContainersOf(ITargetDefinition target) {
		ITargetLocation[] locations = target.getTargetLocations();
		return locations == null ? Stream.empty()
				: Arrays.stream(locations).filter(IUBundleContainer.class::isInstance)
						.map(IUBundleContainer.class::cast);
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
	 * Set whether or not repository references should be followed
	 *
	 * @param value whether or not repository references should be followed
	 */
	public void setFollowRepositoryReferences(boolean value) {
		fFollowRepositoryReferences = value;
	}


	/**
	 * Return whether or not repository references should be followed
	 *
	 * @return whether or not repository references should be followed
	 */
	public boolean isFollowRepositoryReferences() {
		return fFollowRepositoryReferences;
	}

	/**
	 * Return whether or not the given target has a matching profile that is in sync
	 * @param target the target to check
	 * @return whether or not the target has been resolved at the p2 level
	 */
	public static boolean isResolved(ITargetDefinition target) {
		P2TargetUtils synchronizer = getSynchronizer(target);
		if (synchronizer == null) {
			return false;
		}
		return synchronizer.checkProfile(target, synchronizer.getProfile())
				&& allReferencedTargets(target).allMatch(P2TargetUtils::isResolved);
	}

	/**
	 * Return whether or not the given target's matching profile  is in sync
	 * @param target the target to check
	 * @return whether or not the target has been resolved at the p2 level
	 */
	public static boolean isProfileValid(ITargetDefinition target) {
		P2TargetUtils synchronizer = getSynchronizer(target);
		if (synchronizer == null) {
			return false;
		}
		return synchronizer.checkProfile(target, synchronizer.updateProfileFromRegistry(target))
				&& allReferencedTargets(target).allMatch(P2TargetUtils::isProfileValid);
	}

	private static Stream<ITargetDefinition> allReferencedTargets(ITargetDefinition target) {
		return Arrays.stream(target.getTargetLocations()).filter(TargetReferenceBundleContainer.class::isInstance)
				.map(TargetReferenceBundleContainer.class::cast).flatMap(referenceContainer -> {
					try {
						ITargetDefinition refTarget = referenceContainer.getTargetDefinition();
						return Stream.concat(Stream.of(refTarget), allReferencedTargets(refTarget));
					} catch (CoreException e) {
						ILog.get().error("Failed to retrieve referenced target", e); //$NON-NLS-1$
					}
					return Stream.empty();
				});
	}

	private synchronized IProfile updateProfileFromRegistry(ITargetDefinition target) {
		if (fProfile == null) {
			try {
				fProfile = getProfileRegistry().getProfile(getProfileId(target));
			} catch (CoreException e) {
			}
		}
		return fProfile;
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
		return SYNCHRONIZERS.computeIfAbsent(target, t -> new P2TargetUtils());
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
	public static IQueryResult<IInstallableUnit> getIUs(ITargetDefinition target, IProgressMonitor monitor)
			throws CoreException {
		P2TargetUtils synchronizer = getSynchronizer(target);
		if (synchronizer == null) {
			return null;
		}
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
		IProfile profile = getProfile();
		// Happiness if we have a profile and it checks out or if we can load one and it checks out.
		if (profile == null) {
			profile = getProfileRegistry().getProfile(getProfileId(target));
		}
		if (profile != null && checkProfile(target, profile)) {
			// always push the changes to the target because there can be many target objects
			// for the same synchronizer (doh!)
			fProfile = profile;
			notify(target, progress.split(25));
			return;
		}

		// Either no profile was found or it was stale.  Delete the current profile and recreate.
		// This keeps the internal agent data clean and does not cost us much.
		deleteProfile(target.getHandle());
		profile = createProfile(target);

		if (progress.isCanceled()) {
			return;
		}
		progress.setWorkRemaining(75);

		try {
			// Now resolve the profile and refresh the relate IU containers
			if (getIncludeAllRequired()) {
				resolveWithPlanner(target, profile, progress.split(60));
			} else {
				resolveWithSlicer(target, profile, progress.split(60));
			}
			fProfile = profile;
			// If we are updating a profile then delete the old snapshot on success.
			notify(target, progress.split(15));
		} catch (CoreException e) {
			fProfile = null;
			// There was at least one problem getting the contents, delete the profile so we don't cache in a bad state, Bug 439034
			// TODO ALL we really want to delete is the sequence property, so that checkProfile will compare settings and contents
			try {
				deleteProfile(target.getHandle());
			} catch (CoreException e2) {
				PDECore.log(e2.getStatus());
			}
			throw e;
		}
	}

	private IProfile createProfile(ITargetDefinition target) throws CoreException {
		// create a new profile
		IProfileRegistry registry = getProfileRegistry();
		Map<String, String> properties = new HashMap<>();
		properties.put(IProfile.PROP_INSTALL_FOLDER, INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
		properties.put(IProfile.PROP_CACHE, BUNDLE_POOL.toOSString());
		properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
		properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties(target));
		properties.put(IProfile.PROP_NL, generateNLProperty(target));
		setProperties(properties::put, target, getProvisionMode());
		return registry.addProfile(getProfileId(target), properties);
	}

	/**
	 * Signal the relevant bundle containers that the given profile has changed.
	 */
	private void notify(ITargetDefinition target, IProgressMonitor monitor) {
		// flush the target caches first since some of them are used in rebuilding
		// the container caches (e.g., featureModels)
		((TargetDefinition) target).flushCaches(P2TargetUtils.BUNDLE_POOL.toOSString());
		// Now proactively recompute all the related container caches.
		ITargetLocation[] containers = target.getTargetLocations();
		if (containers != null) {
			for (ITargetLocation container : containers) {
				if (container instanceof IUBundleContainer iuContainer) {
					iuContainer.synchronizerChanged(target);
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
		String memento = handle.getMemento();
		return PROFILE_ID_PREFIX + getProfileSuffix(memento);
	}

	/**
	 * Returns the potentially shortened profile identifier based on the given
	 * memento.
	 *
	 * @param memento
	 *            the ITargetHandle memento as the basis of the ID.
	 * @return an ID string short enough that it does not exceed the max file
	 *         length.
	 */
	private static String getProfileSuffix(String memento) {
		// Memento strings can be very long and exceed max filename lengths,
		// trim down to 200 + prefix + hashcode, considering also how the ID
		// will
		// be encoded.
		// org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry.escape(String)
		int escapedLength = 0;
		int length = memento.length();
		// length * 4 > 200
		if (length > 50) {
			for (int i = length - 1; i >= 0; --i) {
				escapedLength += switch (memento.charAt(i)) {
				case '\\', '/', ':', '*', '?', '"', '<', '>', '|', '%' -> 4;
				default -> 1;
				};
				if (escapedLength > 200) {
					return memento.substring(i + 1) + memento.hashCode();
				}
			}
		}
		return memento;
	}

	/**
	 * Returns the profile identifier for this target handle. There is one profile
	 * per target definition.
	 *
	 * @return definition the target to lookup
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
	 */
	public static IProfileRegistry getProfileRegistry() throws CoreException {
		return getP2Service(IProfileRegistry.class, Messages.IUBundleContainer_8);
	}

	/**
	 * Returns the mode used to provision this target - slice versus plan or <code>null</code> if
	 * this target has no software sites.
	 *
	 * @return provisioning mode or <code>null</code>
	 */
	private String getProvisionMode() {
		return getIncludeAllRequired() ? TargetDefinitionPersistenceHelper.MODE_PLANNER : TargetDefinitionPersistenceHelper.MODE_SLICER;
	}

	/**
	 * Returns the metadata repository manager.
	 *
	 * @return metadata repository manager
	 * @throws CoreException if none
	 */
	public static IMetadataRepositoryManager getRepoManager() throws CoreException {
		return getP2Service(IMetadataRepositoryManager.class, Messages.IUBundleContainer_2);
	}

	private static <T> T getP2Service(Class<T> key, String absentErrorMessage) throws CoreException {
		T service = getAgent().getService(key);
		if (service == null) {
			throw new CoreException(Status.error(absentErrorMessage));
		}
		return service;
	}

	/**
	 * Return a queryable on the metadata defined in the given repo locations
	 *
	 * @param repos the repos to lookup
	 * @param followRepositoryReferences whether to follow repository references
	 * @param monitor the progress monitor
	 * @return the set of metadata repositories found
	 * @throws CoreException if there is a problem getting the repositories
	 */
	static IQueryable<IInstallableUnit> getQueryableMetadata(Collection<URI> repos, boolean followRepositoryReferences,
			IProgressMonitor monitor) throws CoreException {
		IMetadataRepositoryManager manager = getRepoManager();
		if (repos.isEmpty()) {
			repos = Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL));
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, repos.size() * 2);

		Set<IRepositoryReference> seen = new HashSet<>();
		List<IMetadataRepository> result = new ArrayList<>(repos.size());
		List<IMetadataRepository> additional = new ArrayList<>();
		MultiStatus repoStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.IUBundleContainer_ProblemsLoadingRepositories);
		for (URI location : repos) {
			try {
				IMetadataRepository repository = manager.loadRepository(location, subMonitor.split(1));
				result.add(repository);
				if (followRepositoryReferences) {
					addReferences(repository, additional, seen, manager, subMonitor.split(1));
				}
			} catch (ProvisionException e) {
				repoStatus.add(e.getStatus());
			}
		}

		if (result.size() != repos.size()) {
			throw new CoreException(repoStatus);
		}
		result.addAll(additional);
		if (result.size() == 1) {
			return result.get(0);
		}
		return QueryUtil.compoundQueryable(new LinkedHashSet<>(result));
	}

	private static void addReferences(IMetadataRepository repository, List<IMetadataRepository> result,
			Set<IRepositoryReference> seen, IMetadataRepositoryManager manager, IProgressMonitor monitor) {
		Collection<IRepositoryReference> references = repository.getReferences();
		SubMonitor subMonitor = SubMonitor.convert(monitor, references.size() * 2);
		for (IRepositoryReference reference : references) {
			if (reference.getType() == IRepository.TYPE_METADATA && reference.isEnabled() && seen.add(reference)) {
				try {
					IMetadataRepository referencedRepository = manager.loadRepository(reference.getLocation(),
							subMonitor.split(1));
					result.add(referencedRepository);
					addReferences(referencedRepository, result, seen, manager, subMonitor.split(1));
				} catch (ProvisionException e) {
					//if reference can't be loaded just ignore it here but log the error just in case the user wants to act on this
					PDECore.log(e);
				}
			}
		}

	}

	/**
	 * Used to resolve the contents of this container if the user is including all required software.  The p2 planner is used
	 * to determine the complete set of IUs required to run the selected software.  If all requirements are met, the bundles
	 * are downloaded from the repository into the bundle pool and added to the target definition.
	 *
	 * @param monitor for reporting progress
	 * @throws CoreException if there is a problem with the requirements or there is a problem downloading
	 */
	private void resolveWithPlanner(ITargetDefinition target, IProfile profile, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, 220);

		// Get the root IUs for every relevant container in the target definition
		Map<IInstallableUnit, String> units = getRootIUs(target, subMonitor.split(20));

		// create the provisioning plan
		IPlanner planner = getPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		// first remove everything that was explicitly installed.  Then add it back.  This has the net effect of
		// removing everything that is no longer needed.
		computeRemovals(profile, request, getIncludeSource());
		request.addAll(units.keySet());
		units.forEach((unit, versionDeclarations) -> {
			request.setInstallableUnitProfileProperty(unit, PROP_INSTALLED_IU, Boolean.toString(true));
			request.setInstallableUnitProfileProperty(unit, PROP_IU_VERSION_DECLARATION, versionDeclarations);
		});

		List<IArtifactRepository> extraArtifactRepositories = new ArrayList<>();
		List<IMetadataRepository> extraMetadataRepositories = new ArrayList<>();
		addAdditionalProvisionIUs(target, extraArtifactRepositories, extraMetadataRepositories);
		ProvisioningContext context = new ProvisioningContext(getAgent()) {
			@Override
			public IQueryable<IArtifactRepository> getArtifactRepositories(IProgressMonitor monitor) {
				return QueryUtil.compoundQueryable(super.getArtifactRepositories(monitor),
						(query, ignore) -> query.perform(extraArtifactRepositories.iterator()));
			}
			@Override
			public IQueryable<IInstallableUnit> getMetadata(IProgressMonitor monitor) {
				return QueryUtil.compoundQueryable(super.getMetadata(monitor),
						QueryUtil.compoundQueryable(extraMetadataRepositories));
			}
		};
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, Boolean.toString(isFollowRepositoryReferences()));
		context.setProperty(ProvisioningContext.FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, Boolean.toString(isFollowRepositoryReferences()));
		context.setMetadataRepositories(getMetadataRepositories(target).toArray(URI[]::new));
		context.setArtifactRepositories(getArtifactRepositories(target).toArray(URI[]::new));

		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, subMonitor.split(20));
		IStatus status = plan.getStatus();
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		setProperties(plan::setProfileProperty, target, TargetDefinitionPersistenceHelper.MODE_PLANNER);
		IProvisioningPlan installerPlan = plan.getInstallerPlan();
		if (installerPlan != null) {
			// this plan requires an update to the installer first, log the fact and attempt
			// to continue, we don't want to update the running SDK while provisioning a target
			PDECore.log(Status.info(Messages.IUBundleContainer_6));
		}
		subMonitor.split(10);

		// execute the provisioning plan
		IPhaseSet phases = createPhaseSet();
		IEngine engine = getEngine();
		IStatus result = engine.perform(plan, phases, subMonitor.split(100));
		if (result.getSeverity() == IStatus.ERROR || result.getSeverity() == IStatus.CANCEL) {
			throw new CoreException(result);
		}

		// Now that we have a plan with all the binary and explicit bundles, do a second pass and add
		// in all the source.
		try {
			planInSourceBundles(profile, context, subMonitor.split(60));
		} catch (CoreException e) {
			// XXX Review required: is adding in the source critical or optional?
			// We failed adding in the source so remove the intermediate profile and rethrow
			try {
				getProfileRegistry().removeProfile(profile.getProfileId(), profile.getTimestamp());
			} catch (CoreException e2) {
				PDECore.log(e2.getStatus());
			}
			throw e;
		}
	}

	private void setProperties(BiConsumer<String, String> setter, ITargetDefinition target, String mode) {
		setter.accept(PROP_PROVISION_MODE, mode);
		setter.accept(PROP_ALL_ENVIRONMENTS, Boolean.toString(getIncludeAllEnvironments()));
		setter.accept(PROP_AUTO_INCLUDE_SOURCE, Boolean.toString(getIncludeSource()));
		setter.accept(PROP_INCLUDE_CONFIGURE_PHASE, Boolean.toString(getIncludeConfigurePhase()));
		setter.accept(PROP_FOLLOW_REPOSITORY_REFERENCES, Boolean.toString(isFollowRepositoryReferences()));
		setter.accept(PROP_SEQUENCE_NUMBER, Integer.toString(((TargetDefinition) target).getSequenceNumber()));
		setter.accept(PROP_DECLARED_REPOSITORIES, iuBundleContainersOf(target).map(IUBundleContainer::getRepositories)
				.flatMap(List::stream).collect(joiningEncodeURIs()));
	}

	private static final String REPOSITORY_LIST_DELIMITER = ","; //$NON-NLS-1$

	private static Collector<URI, ?, String> joiningEncodeURIs() {
		return Collectors.mapping(u -> URLEncoder.encode(u.toASCIIString(), StandardCharsets.UTF_8),
				Collectors.joining(REPOSITORY_LIST_DELIMITER));
	}

	private Set<URI> decodeURIs(String encodedList) {
		return Arrays.stream(encodedList.split(REPOSITORY_LIST_DELIMITER))
				.map(t -> URLDecoder.decode(t, StandardCharsets.UTF_8)).map(URI::create).collect(Collectors.toSet());
	}

	/**
	 * @return the phase set to execute, includes the configuration phase if
	 *         {@link #getIncludeConfigurePhase()} is <code>true</code>
	 */
	@SuppressWarnings("restriction")
	private IPhaseSet createPhaseSet() {
		List<org.eclipse.equinox.internal.p2.engine.Phase> phases = new ArrayList<>(4);
		phases.add(new org.eclipse.equinox.internal.p2.engine.phases.Collect(100));
		phases.add(new org.eclipse.equinox.internal.p2.engine.phases.Property(1));
		phases.add(new org.eclipse.equinox.internal.p2.engine.phases.Uninstall(50, true));
		phases.add(new org.eclipse.equinox.internal.p2.engine.phases.Install(50));
		phases.add(new CollectNativesPhase(100));
		if (getIncludeConfigurePhase()) {
			phases.add(new org.eclipse.equinox.internal.p2.engine.phases.Configure(100));
		}

		return new org.eclipse.equinox.internal.p2.engine.PhaseSet(
				phases.toArray(org.eclipse.equinox.internal.p2.engine.Phase[]::new));
	}

	/**
	 * Update the given change request to remove anything that was explicitly installed
	 * including the internal source IU.
	 */
	private void computeRemovals(IProfile profile, IProfileChangeRequest request, boolean includeSource) {
		// if include source is off then ensure that the source IU is removed.
		if (!includeSource) {
			IInstallableUnit sourceIU = getCurrentSourceIU(profile);
			if (sourceIU != null) {
				request.remove(sourceIU);
			}
		}
		// remove everything that is marked as roots.  The plan will have the new roots added in anyway.
		IQuery<IInstallableUnit> query = new IUProfilePropertyQuery(PROP_INSTALLED_IU, Boolean.toString(true));
		IQueryResult<IInstallableUnit> installedIUs = profile.query(query, null);
		installedIUs.forEach(request::remove);
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
		if (currentSourceIU != null) {
			request.remove(currentSourceIU);
		}
		request.add(sourceIU);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, subMonitor.split(25));
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
		IStatus result = engine.perform(plan, phases, subMonitor.split(75));

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
	private IInstallableUnit createSourceIU(IQueryable<IInstallableUnit> queryable, Version iuVersion) {
		// compute the set of source bundles we could possibly need for the bundles in the profile
		IRequirement bundleRequirement = MetadataFactory.createRequirement("org.eclipse.equinox.p2.eclipse.type", "bundle", null, null, false, false, false); //$NON-NLS-1$ //$NON-NLS-2$
		IQueryResult<IInstallableUnit> profileIUs = queryable.query(QueryUtil.createIUAnyQuery(), null);
		List<IRequirement> requirements = new ArrayList<>();
		for (IInstallableUnit profileIU : profileIUs) {
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
	private IInstallableUnit getCurrentSourceIU(IQueryable<IInstallableUnit> queryable) {
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(SOURCE_IU_ID);
		return queryFirst(queryable, query, null).orElse(null);
	}

	/**
	 * Used to resolve the contents of this container when the user has chosen to manage the dependencies in the target
	 * themselves.  The selected IUs and any required software that can be found will be retrieved from the repositories
	 * and added to the target.  Any missing required software will be ignored.
	 *
	 * @param monitor for reporting progress
	 * @throws CoreException if there is a problem interacting with the repositories
	 */
	private void resolveWithSlicer(ITargetDefinition target, IProfile profile, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, 110);

		// resolve IUs
		Map<IInstallableUnit, String> units = getRootIUs(target, subMonitor.split(40));

		Collection<URI> repositories = getMetadataRepositories(target);
		if (repositories.isEmpty()) {
			return;
		}
		IQueryable<IInstallableUnit> allMetadata = getQueryableMetadata(repositories, isFollowRepositoryReferences(),
				subMonitor.split(5));

		// do an initial slice to add everything the user requested
		IQueryResult<IInstallableUnit> queryResult = slice(units.keySet(), allMetadata, target, subMonitor.split(5));
		if (queryResult == null || queryResult.isEmpty()) {
			return;
		}

		// If we are including source then create a source IU to bring in the relevant source
		// bundles and run the slicer again.
		if (getIncludeSource()) {
			// Build an IU that represents all the source bundles and slice again to add them in if available
			IInstallableUnit sourceIU = createSourceIU(queryResult, Version.createOSGi(1, 0, 0));
			List<IInstallableUnit> units2 = new ArrayList<>(units.keySet());
			units2.add(sourceIU);

			queryResult = slice(units2, allMetadata, target, subMonitor.split(5));
			if (queryResult == null || queryResult.isEmpty()) {
				return;
			}
		}

		IEngine engine = getEngine();
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(repositories.toArray(URI[]::new));
		context.setArtifactRepositories(getArtifactRepositories(target).toArray(URI[]::new));
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, Boolean.toString(isFollowRepositoryReferences()));
		context.setProperty(ProvisioningContext.FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, Boolean.toString(isFollowRepositoryReferences()));
		IProvisioningPlan plan = engine.createPlan(profile, context);
		setProperties(plan::setProfileProperty, target, TargetDefinitionPersistenceHelper.MODE_SLICER);

		Set<IInstallableUnit> newSet = queryResult.toUnmodifiableSet();
		for (IInstallableUnit unit : newSet) {
			plan.addInstallableUnit(unit);
		}
		units.forEach((unit, versionDeclarations) -> {
			plan.setInstallableUnitProfileProperty(unit, PROP_INSTALLED_IU, Boolean.toString(true));
			plan.setInstallableUnitProfileProperty(unit, PROP_IU_VERSION_DECLARATION, versionDeclarations);
		});

		// remove all units that are in the current profile but not in the new slice
		Set<IInstallableUnit> toRemove = profile.query(QueryUtil.ALL_UNITS, null).toSet();
		toRemove.removeAll(newSet);
		for (IInstallableUnit name : toRemove) {
			plan.removeInstallableUnit(name);
		}

		subMonitor.split(5);

		// execute the provisioning plan
		IPhaseSet phases = createPhaseSet();
		IStatus result = engine.perform(plan, phases, subMonitor.split(50));
		if (result.getSeverity() == IStatus.ERROR || result.getSeverity() == IStatus.CANCEL) {
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
	private IQueryResult<IInstallableUnit> slice(Collection<IInstallableUnit> units,
			IQueryable<IInstallableUnit> allMetadata, ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		// slice IUs and all prerequisites
		PermissiveSlicer slicer = null;
		if (getIncludeAllEnvironments()) {
			slicer = new PermissiveSlicer(allMetadata, new HashMap<>(), true, false, true, true, false);
		} else {
			Map<String, String> props = new HashMap<>();
			props.put("osgi.os", definition.getOS() != null ? definition.getOS() : Platform.getOS()); //$NON-NLS-1$
			props.put("osgi.ws", definition.getWS() != null ? definition.getWS() : Platform.getWS()); //$NON-NLS-1$
			props.put("osgi.arch", definition.getArch() != null ? definition.getArch() : Platform.getOSArch()); //$NON-NLS-1$
			props.put("osgi.nl", definition.getNL() != null ? definition.getNL() : Platform.getNL()); //$NON-NLS-1$
			props.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
			slicer = new PermissiveSlicer(allMetadata, props, true, false, false, true, false);
		}
		IQueryable<IInstallableUnit> slice = slicer.slice(units, subMonitor.split(50));
		IStatus sliceStatus = slicer.getStatus();
		// If the slicer encounters an error, stop the operation
		if (sliceStatus.getSeverity() == IStatus.ERROR) {
			throw new CoreException(sliceStatus);
		}

		// Collect the IUs from the sliced
		IQueryResult<IInstallableUnit> queryResult = null;
		if (slice != null) {
			queryResult = slice.query(QueryUtil.createIUAnyQuery(), subMonitor.split(50));
		}

		// If the slicer encounters a non-error status, only report it if the slice returned no IU results
		// It would be better to inform the user, but we do not want to stop the location from resolving (bug 350772)
		if (!sliceStatus.isOK() && queryResult != null && queryResult.isEmpty()) {
			throw new CoreException(sliceStatus);
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
	private Collection<URI> getArtifactRepositories(ITargetDefinition target) throws CoreException {
		Set<URI> result = new LinkedHashSet<>();
		ITargetLocation[] containers = target.getTargetLocations();
		if (containers == null) {
			containers = new ITargetLocation[0];
		}
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		for (ITargetLocation container : containers) {
			if (container instanceof IUBundleContainer iuContainer) {
				List<URI> repos = iuContainer.getRepositories();
				if (repos.isEmpty()) {
					repos = Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL));
				}
				result.addAll(repos);
			} else if (container instanceof TargetReferenceBundleContainer targetRefContainer) {
				ITargetDefinition referencedTargetDefinition = targetRefContainer.getTargetDefinition();
				result.addAll(getArtifactRepositories(referencedTargetDefinition));
			}
		}

		// get all the local artifact repos we know in the manager currently
		result.addAll(Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_LOCAL)));

		if (Boolean.parseBoolean(System.getProperty("pde.usePoolsInfo", "true"))) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				result.addAll(RepositoryHelper.getSharedBundlePools().stream().map(Path::toUri).toList());
			} catch (Exception e) {
				//$FALL-THROUGH$
			}
		}

		// Add in the IDE profile bundle pool and all known workspaces
		findProfileRepos(result);
		findWorkspaceRepos(result);

		return result;
	}

	/**
	 * Add the artifact repos from the PDE target bundle pools from all known repos.  For example, the list
	 * of "recent workspaces" maintained by the IDE is a good source.
	 *
	 * @param additionalRepos the set to which additional repos are added.
	 */
	private void findWorkspaceRepos(Set<URI> additionalRepos) {
		if (Boolean.parseBoolean(System.getProperty("pde.usePoolsInfo", "true"))) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				additionalRepos.addAll(RepositoryHelper.getWorkspaceBundlePools().stream().map(Path::toUri).toList());
			} catch (Exception e) {
				//$FALL-THROUGH$
			}
		}

		IPreferencesService prefs = getPreferences();
		if (prefs == null) {
			return;
		}
		String recent = prefs.getString("org.eclipse.ui.ide", "RECENT_WORKSPACES", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		if (recent == null) {
			return;
		}
		String[] recents = recent.split("\n"); //$NON-NLS-1$
		for (String recentWorkspace : recents) {
			File bundlePool = new File(recentWorkspace + "/.metadata/.plugins/org.eclipse.pde.core/.bundle_pool"); //$NON-NLS-1$
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
	private void findProfileRepos(Set<URI> additionalRepos) {
		try {
			// NOTE: be sure to use the global p2 agent here as we are looking for SELF.
			IProfileRegistry profileRegistry = getGlobalAgent().getService(IProfileRegistry.class);
			if (profileRegistry == null) {
				return;
			}
			IProfile self = profileRegistry.getProfile(IProfileRegistry.SELF);
			if (self == null) {
				return;
			}

			IAgentLocation location = getGlobalAgent().getService(IAgentLocation.class);
			URI dataArea = location.getDataArea("org.eclipse.equinox.p2.engine"); //$NON-NLS-1$
			dataArea = URIUtil.append(dataArea, "profileRegistry/" + self.getProfileId() + ".profile"); //$NON-NLS-1$//$NON-NLS-2$
			@SuppressWarnings("restriction")
			Collection<IRepositoryReference> repos = new org.eclipse.equinox.internal.p2.engine.ProfileMetadataRepository(
					getGlobalAgent(), dataArea, null).getReferences();
			for (IRepositoryReference reference : repos) {
				if (reference.getType() == IRepository.TYPE_ARTIFACT && reference.getLocation() != null) {
					additionalRepos.add(reference.getLocation());
				}
			}
		} catch (CoreException e) {
			// if there is a problem, move on.  Could log something here
		}
	}

	/**
	 * Returns the IU's for the given target related to the given containers
	 *
	 * @param definition the definition to filter with
	 * @return the discovered IUs
	 * @exception CoreException if unable to retrieve IU's
	 */
	private Map<IInstallableUnit, String> getRootIUs(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {

		ITargetLocation[] containers = definition.getTargetLocations();
		if (containers == null) {
			return Map.of();
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.IUBundleContainer_0, containers.length);
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.IUBundleContainer_ProblemsLoadingRepositories);
		Map<IInstallableUnit, String> result = new HashMap<>();
		// Collect all declared IUs and their version declaration.
		// An IU may be declared multiple times with different versions
		for (ITargetLocation container : containers) {
			if (container instanceof IUBundleContainer iuContainer) {
				try {
					iuContainer.getRootIUs(subMonitor.split(1))
							.forEach((iu, versionDeclarations) -> addDeclaredVersions(result, iu, versionDeclarations));
				} catch (CoreException e) {
					status.add(e.getStatus());
				}
			}
		}
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		return result;
	}

	private static final String VERSION_DECLARATION_SEPARATOR = ";"; //$NON-NLS-1$

	private void addDeclaredVersions(Map<IInstallableUnit, String> result, IInstallableUnit iu,
			Set<VersionRange> versionDeclarations) {
		String joindVersions = versionDeclarations.stream().map(VersionRange::toString)
				.collect(Collectors.joining(VERSION_DECLARATION_SEPARATOR));
		result.merge(iu, joindVersions, (v1, v2) -> v1 + VERSION_DECLARATION_SEPARATOR + v2);
	}

	private Stream<VersionRange> parseVersions(IInstallableUnit unit, String versionList) {
		return versionList == null // if null, a specific version was declared
				? Stream.of(new VersionRange(unit.getVersion(), true, unit.getVersion(), true))
				: Arrays.stream(versionList.split(VERSION_DECLARATION_SEPARATOR)).map(VersionRange::create);
	}

	/**
	 * Returns the repositories to consider when resolving IU's (will return default set of
	 * repositories if current repository settings are <code>null</code>).
	 *
	 * @return URI's of repositories to use when resolving bundles
	 * @exception CoreException
	 */
	private Collection<URI> getMetadataRepositories(ITargetDefinition target) throws CoreException {
		Set<URI> result = new HashSet<>();
		ITargetLocation[] containers = target.getTargetLocations();
		if (containers == null) {
			containers = new ITargetLocation[0];
		}
		IMetadataRepositoryManager manager = getRepoManager();
		for (ITargetLocation container : containers) {
			if (container instanceof IUBundleContainer iuContainer) {
				List<URI> repos = iuContainer.getRepositories();
				if (repos.isEmpty()) {
					repos = Arrays.asList(manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL));
				}
				result.addAll(repos);
			}
			if (container instanceof TargetReferenceBundleContainer targetRefContainer) {
				ITargetDefinition referencedTargetDefinition = targetRefContainer.getTargetDefinition();
				result.addAll(getMetadataRepositories(referencedTargetDefinition));
			}
		}
		return result;
	}

	private void addAdditionalProvisionIUs(ITargetDefinition target,
			Collection<IArtifactRepository> extraArtifactRepositories,
			Collection<IMetadataRepository> extraMetadataRepositories) throws CoreException {
		ITargetLocation[] containers = target.getTargetLocations();
		if (containers != null) {
			for (ITargetLocation container : containers) {
				if (container instanceof IUBundleContainer) {
					// this is already handled by getMetadataRepositories(..)
					continue;
				}
				if (container instanceof TargetReferenceBundleContainer targetRefContainer) {
					ITargetDefinition referencedTargetDefinition = targetRefContainer.getTargetDefinition();
					addAdditionalProvisionIUs(referencedTargetDefinition, extraArtifactRepositories,
							extraMetadataRepositories);
					continue;
				}
				if (!container.isResolved()) {
					container.resolve(target, new NullProgressMonitor());
				}
				extraArtifactRepositories.add(new VirtualArtifactRepository(getAgent(), container));
				List<IInstallableUnit> installableUnits = InstallableUnitGenerator //
						.generateInstallableUnits(container.getBundles(), container.getFeatures()) //
						.toList();
				extraMetadataRepositories.add(new VirtualMetadataRepository(getAgent(), installableUnits));
			}
		}
	}

	private static final String NATIVE_ARTIFACTS = "nativeArtifacts"; //$NON-NLS-1$
	private static final String NATIVE_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$

	protected static class CollectNativesAction extends ProvisioningAction {
		@Override
		public IStatus execute(Map<String, Object> parameters) {
			@SuppressWarnings("restriction")
			IInstallableUnit installableUnit = ((org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand) parameters
					.get(PARM_OPERAND)).second();
			if (installableUnit == null) {
				return Status.OK_STATUS;
			}

			IArtifactRepositoryManager manager;
			try {
				Collection<IArtifactKey> toDownload = installableUnit.getArtifacts();
				if (toDownload == null) {
					return Status.OK_STATUS;
				}

				@SuppressWarnings("unchecked")
				List<IArtifactRequest> artifactRequests = (List<IArtifactRequest>) parameters.get(NATIVE_ARTIFACTS);
				IArtifactRepository destinationArtifactRepository = getBundlePool();
				manager = getArtifactRepositoryManager();
				for (IArtifactKey keyToDownload : toDownload) {
					IArtifactRequest request = manager.createMirrorRequest(keyToDownload, destinationArtifactRepository, null, null);
					artifactRequests.add(request);
				}
			} catch (CoreException e) {
				return e.getStatus();
			}
			return Status.OK_STATUS;
		}

		@Override
		public IStatus undo(Map<String, Object> parameters) {
			// nothing to do for now
			return Status.OK_STATUS;
		}
	}

	@SuppressWarnings("restriction")
	protected static class CollectNativesPhase extends org.eclipse.equinox.internal.p2.engine.InstallableUnitPhase {
		public CollectNativesPhase(int weight) {
			super(NATIVE_ARTIFACTS, weight);
		}

		@Override
		protected List<ProvisioningAction> getActions(
				org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand operand) {
			IInstallableUnit unit = operand.second();
			if (unit != null && unit.getTouchpointType().getId().equals(NATIVE_TYPE)) {
				return List.of(new CollectNativesAction());
			}
			return null;
		}

		@Override
		protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
			parameters.put(NATIVE_ARTIFACTS, new ArrayList<>());
			parameters.put(PARM_PROFILE, profile);
			return null;
		}

		@Override
		protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map<String, Object> parameters) {
			@SuppressWarnings("unchecked")
			List<IArtifactRequest> artifactRequests = (List<IArtifactRequest>) parameters.get(NATIVE_ARTIFACTS);
			ProvisioningContext context = (ProvisioningContext) parameters.get(PARM_CONTEXT);
			IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
			org.eclipse.equinox.internal.p2.engine.DownloadManager dm = new org.eclipse.equinox.internal.p2.engine.DownloadManager(
					context, agent);
			for (IArtifactRequest iArtifactRequest : artifactRequests) {
				dm.add(iArtifactRequest);
			}
			return dm.start(monitor);
		}
	}

	/**
	 * @return the profile associated with this synchronizer
	 */
	synchronized IProfile getProfile() {
		return fProfile;
	}

}
