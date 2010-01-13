package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
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
	private MultiStatus fStatus;

	private IProfile fProfile;
	/**
	 * TODO Hack to allow bundle pool to be used as only repository
	 */
	private IFileArtifactRepository fArtifactRepo;

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

		IProvisioningAgent agent = TargetUtils.getProvisioningAgent(fTarget);
		if (agent == null) {
			fStatus.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
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

			fProfile = null;
			String profileName = TargetUtils.getProfileID(fTarget);

			// Delete any previous profiles with the same ID
			registry.removeProfile(profileName);

			// Create the profile
			Properties props = new Properties();
			props.put(IProfile.PROP_CACHE, TargetUtils.getSharedBundlePool(fTarget));
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

			// TODO Hack to add artifact repository, no need to load it here
			try {
				fArtifactRepo = getBundlePool(agent, fProfile);
			} catch (CoreException e) {
				fStatus.add(e.getStatus());
				return fStatus;
			}

			context.setArtifactRepositories(new URI[] {fArtifactRepo.getLocation()});

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

		IProvisioningAgent agent = TargetUtils.getProvisioningAgent(fTarget);

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

			String profileName = TargetUtils.getProfileID(fTarget);
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

			List bundleInfos = new ArrayList();
			IQueryResult result = fProfile.query(P2Utils.BUNDLE_QUERY, null);
			for (Iterator iterator = result.iterator(); iterator.hasNext();) {
				IInstallableUnit unit = (IInstallableUnit) iterator.next();
				Collection artifacts = unit.getArtifacts();
				if (!artifacts.isEmpty()) {
					IArtifactKey key = (IArtifactKey) artifacts.iterator().next();
					URI location = null;
					// TODO Hack for testing
					if (fArtifactRepo != null) {
						File file = fArtifactRepo.getArtifactFile(key);
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
	private IFileArtifactRepository getBundlePool(IProvisioningAgent agent, IProfile profile) throws CoreException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
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
