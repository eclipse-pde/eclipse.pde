package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.AggregatedBundleRepository;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Helper class for TargetDefinition to encapsulate code for provisioning a target.
 * 
 * @since 3.6
 * @see TargetResolver
 * @see TargetDefinition
 */
public class TargetProvisioner {

	private static final String PROP_CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
//	private static final String PROP_ARTIFACT_REPOS = "artifact_repositories";
	private static final String REPO_DELIMITER = "|"; //$NON-NLS-1$

	private ITargetDefinition fTarget;
	private TargetResolver fResolver;
	private MultiStatus fStatus;
	private IProfile fProfile;
	private AggregatedBundleRepository fArtifactRepo;

	TargetProvisioner(ITargetDefinition target, TargetResolver resolver) {
		fTarget = target;
		fResolver = resolver;
	}

	public IStatus getStatus() {
		return fStatus;
	}

	public IStatus provision(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, "Provisioning bundles in target platform", 100);
		fStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, "Problems occurred while provisioning plug-ins in the target platform", null);

		IProvisioningAgent agent;
		try {
			agent = TargetPlatformService.getProvisioningAgent();
		} catch (CoreException e) {
			fStatus.add(e.getStatus());
			return fStatus;
		}

		try {
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			if (registry == null) {
				fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
				return fStatus;
			}

			IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
			if (engine == null) {
				fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
				return fStatus;
			}

			if (fResolver.getStatus() == null || !fResolver.getStatus().isOK()) {
				fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "The target has not been resolved"));
				return fStatus;
			}

			String profileName = null;
			try {
				profileName = TargetPlatformService.getProfileID(fTarget);
			} catch (CoreException e) {
				fStatus.add(e.getStatus());
				return fStatus;
			}

			// Delete any previous profiles with the same ID
			registry.removeProfile(profileName);

			// Create the profile
			Properties props = new Properties();
			props.put(IProfile.PROP_CACHE, TargetPlatformService.BUNDLE_POOL);

			// Save the artifact repositories to the profile so we can load them after a restart
			StringBuffer buffer = new StringBuffer();
			for (Iterator iterator = fResolver.getArtifactRepositories().iterator(); iterator.hasNext();) {
				IArtifactRepository currentRepo = (IArtifactRepository) iterator.next();
				if (buffer.length() > 0) {
					buffer.append(REPO_DELIMITER);
				}
				buffer.append(currentRepo.getLocation().toString());
			}
			props.put(PROP_CACHE_EXTENSIONS, buffer.toString());

			try {
				fProfile = registry.addProfile(profileName, props);
			} catch (ProvisionException e) {
				fStatus.add(e.getStatus());
				return fStatus;
			}

			subMon.worked(10);
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			// Create the provisioning context with metadata and artifact repositories
			Set metaRepos = new HashSet();
			for (Iterator iterator = fResolver.getMetadataRepositories().iterator(); iterator.hasNext();) {
				IMetadataRepository currentRepo = (IMetadataRepository) iterator.next();
				metaRepos.add(currentRepo.getLocation());
			}
			ProvisioningContext context = new ProvisioningContext((URI[]) metaRepos.toArray(new URI[metaRepos.size()]));
			Set artifactRepos = new HashSet();
			for (Iterator iterator = fResolver.getArtifactRepositories().iterator(); iterator.hasNext();) {
				IArtifactRepository currentRepo = (IArtifactRepository) iterator.next();
				artifactRepos.add(currentRepo.getLocation());
			}
			// If we don't have any artifact repos, try to use anything the manager knows about
			if (artifactRepos.size() > 0) {
				context.setArtifactRepositories((URI[]) artifactRepos.toArray(new URI[artifactRepos.size()]));
			}

			subMon.worked(5);

			// Get the list of installable units including
			Collection includedIUs = fResolver.calculateIncludedIUs();
			subMon.worked(20);

			IQueryResult includedBundles = P2Utils.BUNDLE_QUERY.perform(includedIUs.iterator());
			subMon.worked(5);

			// Create operands to install the metadata
			ArrayList operands = new ArrayList(includedBundles.unmodifiableSet().size());
			Iterator iterator = includedBundles.iterator();
			while (iterator.hasNext()) {
				operands.add(new InstallableUnitOperand(null, (IInstallableUnit) iterator.next()));
			}
			// Add installed property to rootIUS
			//		for (Iterator iterator = fRootIUs.iterator(); iterator.hasNext();) {
			//			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			//			operands.add(new InstallableUnitPropertyOperand(unit, AbstractTargetHandle.PROP_INSTALLED_IU, null, Boolean.toString(true)));
			//		}

			subMon.worked(5);

			// TODO Fix progress monitor!
			PhaseSet phases = DefaultPhaseSet.createDefaultPhaseSet(DefaultPhaseSet.PHASE_CHECK_TRUST | DefaultPhaseSet.PHASE_CONFIGURE | DefaultPhaseSet.PHASE_UNCONFIGURE | DefaultPhaseSet.PHASE_UNINSTALL);
			ProvisioningPlan plan = new ProvisioningPlan(fProfile, (Operand[]) operands.toArray(new Operand[operands.size()]), context);
			IStatus result = engine.perform(plan, phases, subMon.newChild(55));

			if (!result.isOK()) {
				fStatus.add(result);
			}
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

		} finally {
			agent.stop();
		}

		subMon.done();
		return fStatus;
	}

	public IStatus provisionExisting(IProgressMonitor monitor) {
		fStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, "Problems occurred while provisioning plug-ins in the target platform", null);
		fProfile = null;
		SubMonitor subMon = SubMonitor.convert(monitor, "Loading previous target profile", 50);

		IProvisioningAgent agent;
		try {
			agent = TargetPlatformService.getProvisioningAgent();
		} catch (CoreException e) {
			fStatus.add(e.getStatus());
			return fStatus;
		}

		try {
			if (agent == null) {
				fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
				return fStatus;
			}

			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			if (registry == null) {
				fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
				return fStatus;
			}

			String profileName = null;
			try {
				profileName = TargetPlatformService.getProfileID(fTarget);
			} catch (CoreException e) {
				fStatus.add(e.getStatus());
				return fStatus;
			}

			fProfile = registry.getProfile(profileName);

			if (fProfile == null) {
				fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Could not find a profile to restore from.  Use the Target Platform Preference Page to reload."));
				return fStatus;
			}

			// TODO Test to see if all bundles specified in the profile actually exist on disk.
		} finally {
			agent.stop();
		}

		subMon.done();
		return fStatus;

	}

	public BundleInfo[] getProvisionedBundles() {
		if (fProfile != null) {
			try {
				IFileArtifactRepository bundlePool = getBundlePoolRepo();
				List bundleInfos = new ArrayList();
				IQueryResult result = fProfile.query(P2Utils.BUNDLE_QUERY, null);
				for (Iterator iterator = result.iterator(); iterator.hasNext();) {
					IInstallableUnit unit = (IInstallableUnit) iterator.next();
					Collection artifacts = unit.getArtifacts();
					if (!artifacts.isEmpty()) {
						IArtifactKey key = (IArtifactKey) artifacts.iterator().next();
						URI location = null;
						File file = bundlePool.getArtifactFile(key);
						if (file != null && file.exists()) {
							location = file.toURI();
							BundleInfo newBundle = new BundleInfo(unit.getId(), unit.getVersion().toString(), location, BundleInfo.NO_LEVEL, false);
							bundleInfos.add(newBundle);
						}
					}
				}
				return (BundleInfo[]) bundleInfos.toArray(new BundleInfo[bundleInfos.size()]);
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
		return new BundleInfo[0];
	}

	public BundleInfo[] getSourceBundles() {
		if (fProfile != null) {
			try {
				IFileArtifactRepository bundlePool = getBundlePoolRepo();
				List bundleInfos = new ArrayList();
				IQueryResult result = fProfile.query(P2Utils.SOURCE_QUERY, null);
				for (Iterator iterator = result.iterator(); iterator.hasNext();) {
					IInstallableUnit unit = (IInstallableUnit) iterator.next();
					Collection artifacts = unit.getArtifacts();
					if (!artifacts.isEmpty()) {
						IArtifactKey key = (IArtifactKey) artifacts.iterator().next();
						URI location = null;
						File file = bundlePool.getArtifactFile(key);
						if (file != null && file.exists()) {
							location = file.toURI();
							BundleInfo newBundle = new BundleInfo(unit.getId(), unit.getVersion().toString(), location, BundleInfo.NO_LEVEL, false);
							bundleInfos.add(newBundle);
						}
					}
				}
				return (BundleInfo[]) bundleInfos.toArray(new BundleInfo[bundleInfos.size()]);
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
		return new BundleInfo[0];
	}

	private IFileArtifactRepository getBundlePoolRepo() throws CoreException {
		if (fArtifactRepo != null) {
			return fArtifactRepo;
		}

		if (fProfile == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Target profile unavailable"));
		}

		IProvisioningAgent agent = TargetPlatformService.getProvisioningAgent();

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		List repos = new ArrayList();
		String repoProperty = fProfile.getProperty(PROP_CACHE_EXTENSIONS);
		if (repoProperty != null) {
			String[] repoLocations = repoProperty.split("\\" + REPO_DELIMITER); //$NON-NLS-1$
			for (int i = 0; i < repoLocations.length; i++) {
				try {
					URI currentLocation = new URI(repoLocations[i]);
					if (URIUtil.isFileURI(currentLocation)) {
						repos.add(manager.loadRepository(currentLocation, null));
					}
				} catch (URISyntaxException e) {
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Problems parsing saved repository information", e));
				}
			}
		}
		URI bundlePool = new File(TargetPlatformService.BUNDLE_POOL).toURI();
		repos.add(manager.loadRepository(bundlePool, null));

		fArtifactRepo = new AggregatedBundleRepository(repos);
		agent.stop();
		return fArtifactRepo;
	}
}
