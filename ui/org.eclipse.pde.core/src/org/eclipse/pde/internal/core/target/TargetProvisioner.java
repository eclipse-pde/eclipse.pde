package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
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

	private ITargetDefinition fTarget;
	private TargetResolver fResolver;
	private IProvisioningAgent fAgent;

	private MultiStatus fStatus;
	private IProfile fProfile;

	TargetProvisioner(ITargetDefinition target, TargetResolver resolver) {
		fTarget = target;
		fResolver = resolver;
		fAgent = TargetUtils.getAgentForTarget(target);
	}

	public IStatus getStatus() {
		return fStatus;
	}

	public IStatus provision(IProgressMonitor monitor) {
		fStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, "Problems occurred while provisioning plug-ins in the target platform", null);

		if (fAgent == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			return fStatus;
		}

		IProfileRegistry registry = (IProfileRegistry) fAgent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			return fStatus;
		}

		IEngine engine = (IEngine) fAgent.getService(IEngine.SERVICE_NAME);
		if (engine == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			return fStatus;
		}

		if (fResolver.getStatus() == null || !fResolver.getStatus().isOK()) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "The target has not been resolved"));
			return fStatus;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, "Provisioning bundles in target platform", 100);

		fProfile = null;
		String profileName = TargetUtils.getProfileID(fTarget);

		// Delete any previous profiles with the same ID
		registry.removeProfile(profileName);

		// Create the profile
		Properties props = new Properties();
		// TODO Need to set install folder and bundle pool
		props.put(IProfile.PROP_CACHE, TargetUtils.getSharedBundlePool());

//		properties.put(IProfile.PROP_INSTALL_FOLDER, AbstractTargetHandle.INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
//		properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
//		// set up environment & NL properly so OS specific fragments are down loaded/installed
//		properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties());
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

		// Create the provisioning context
		Set repos = new HashSet();
		for (Iterator iterator = fResolver.getResolvedRepositories().iterator(); iterator.hasNext();) {
			IRepository currentRepo = (IRepository) iterator.next();
			if (currentRepo instanceof IMetadataRepository) {
				repos.add(currentRepo.getLocation());
			}
		}
		ProvisioningContext context = new ProvisioningContext((URI[]) repos.toArray(new URI[repos.size()]));

		subMon.worked(5);

		// Get the list of installable units including
		Collection includedIUs = fResolver.calculateIncludedIUs();
		subMon.worked(20);

		// TODO Only install bundles for now
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

		subMon.done();
		if (!result.isOK()) {
			fStatus.add(result);
		}
		if (subMon.isCanceled()) {
			fStatus.add(Status.CANCEL_STATUS);
			return fStatus;
		}

		return fStatus;
	}

	public IStatus provisionExisting(IProgressMonitor monitor) {
		fStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, "Problems occurred while provisioning plug-ins in the target platform", null);
		fProfile = null;

		if (fAgent == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			return fStatus;
		}

		IProfileRegistry registry = (IProfileRegistry) fAgent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			return fStatus;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, "Loading previous target profile", 50);

		String profileName = TargetUtils.getProfileID(fTarget);
		fProfile = registry.getProfile(profileName);

		if (fProfile == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Could not find a profile to restore from.  Use the Target Platform Preference Page to reload."));
			return fStatus;
		}

		// TODO Test to see if all bundles specified in the profile actually exist on disk.

		subMon.done();
		return fStatus;

	}

	public BundleInfo[] getProvisionedBundles() {
		if (fProfile != null) {

			// Hack to test directory containers, need to create a compound set of artifact repos that can be queried
//			IFileArtifactRepository repo = null;
//			try {
//				IBundleContainer[] containers = fResolver.fTarget.getBundleContainers();
//				for (int i = 0; i < containers.length; i++) {
//					if (containers[i] instanceof DirectoryBundleContainer) {
//						repo = ((DirectoryBundleContainer) containers[i]).generateArtifactRepository();
//					}
//				}
//			} catch (CoreException e) {
//				// TODO For now, just don't set the repository
//			}

			// Hack to just use bundle pool
			IFileArtifactRepository repo = getBundlePool(fProfile);

			List bundleInfos = new ArrayList();
			IQueryResult result = fProfile.query(P2Utils.BUNDLE_QUERY, null);
			for (Iterator iterator = result.iterator(); iterator.hasNext();) {
				IInstallableUnit unit = (IInstallableUnit) iterator.next();
				Collection artifacts = unit.getArtifacts();
				if (!artifacts.isEmpty()) {
					IArtifactKey key = (IArtifactKey) artifacts.iterator().next();
					URI location = null;
					// TODO Hack for testing
					if (repo != null) {
						File file = repo.getArtifactFile(key);
						if (file != null && file.exists()) {
							location = file.toURI();
						}
					}
					BundleInfo newBundle = new BundleInfo(unit.getId(), unit.getVersion().toString(), location, BundleInfo.NO_LEVEL, false);
					bundleInfos.add(newBundle);

				}

			}
			return (BundleInfo[]) bundleInfos.toArray(new BundleInfo[bundleInfos.size()]);
		}
		return new BundleInfo[0];
	}

	/**
	 * Returns the local bundle pool (repository) where bundles are stored for the
	 * given profile.
	 * 
	 * @param profile profile bundles are stored
	 * @return local file artifact repository
	 * @throws CoreException
	 */
	private IFileArtifactRepository getBundlePool(IProfile profile) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) fAgent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null) {
			// TODO Handle broken service
//			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			return null;
		}

		String path = profile.getProperty(IProfile.PROP_CACHE);
		if (path != null) {
			URI uri = new File(path).toURI();
			try {
				return (IFileArtifactRepository) manager.loadRepository(uri, null);
			} catch (ProvisionException e) {
				//the repository doesn't exist, so fall through and create a new one
			}
		}
		return null;
	}

}
