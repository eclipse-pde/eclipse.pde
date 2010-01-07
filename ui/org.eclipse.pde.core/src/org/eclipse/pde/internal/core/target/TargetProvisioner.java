package org.eclipse.pde.internal.core.target;

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
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.pde.internal.core.*;

/**
 * Helper class for TargetDefinition to encapsulate code for provisioning a target.
 * 
 * @since 3.6
 * @see TargetResolver
 * @see TargetDefinition
 */
public class TargetProvisioner {

	private static final String PROFILE_NAME = "TARGET_PROFILE";

	private IProvisioningAgent fAgent;
	private TargetResolver fResolver;

	private MultiStatus fStatus;
	private IProfile fProfile;

	TargetProvisioner(TargetResolver resolver) {
		fResolver = resolver;
		// TODO Get proper agent in target metadata area
		fAgent = (IProvisioningAgent) PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
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

		// Delete any previous profiles with the same ID
		registry.removeProfile(PROFILE_NAME);

		// Create the profile
		Properties props = new Properties();
		// TODO Need to set install folder and bundle pool
		props.setProperty(IProfile.PROP_INSTALL_FOLDER, "/home/cwindatt/Cache/");

//		properties.put(IProfile.PROP_INSTALL_FOLDER, AbstractTargetHandle.INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
//		properties.put(IProfile.PROP_CACHE, AbstractTargetHandle.BUNDLE_POOL.toOSString());
//		properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
//		// set up environment & NL properly so OS specific fragments are down loaded/installed
//		properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties());
		try {
			fProfile = registry.addProfile(PROFILE_NAME, props);
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

	public BundleInfo[] getProvisionedBundles() {
		if (fProfile != null) {
			List bundleInfos = new ArrayList();
			IQueryResult result = fProfile.query(P2Utils.BUNDLE_QUERY, null);
			for (Iterator iterator = result.iterator(); iterator.hasNext();) {
				IInstallableUnit unit = (IInstallableUnit) iterator.next();
				Collection artifacts = unit.getArtifacts();
				if (!artifacts.isEmpty()) {
					IArtifactKey key = (IArtifactKey) artifacts.iterator().next();
//					IFileArtifactRepository repo = getBundlePool(profile);
//					File file = repo.getArtifactFile(key);
//					if (file == null) {
//						// TODO: missing bundle
//					} else {
// TODO Add in bundle location
					BundleInfo newBundle = new BundleInfo(unit.getId(), unit.getVersion().toString(), null, BundleInfo.NO_LEVEL, false);
					bundleInfos.add(newBundle);
//					}
				}

			}
			return (BundleInfo[]) bundleInfos.toArray(new BundleInfo[bundleInfos.size()]);
		}
		return new BundleInfo[0];
	}
}
