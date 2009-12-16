package org.eclipse.pde.internal.core.target;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Helper class for TargetDefinition to encapsulate code for resolving a target.
 * 
 * @since 3.6
 * @see TargetDefinition
 */
public class TargetResolver {

	private static final String PROFILE_NAME = "TARGET_PROFILE";

	private ITargetDefinition fTarget;
	private IProvisioningAgent fAgent;

	private MultiStatus fStatus;
	private List fAllRepos;
	private List fRootIUs;
	private Collection fAvailableIUs;

	private IProfile fProfile;

	/**
	 * Query to find installable units that match a set of id/version pairs stored in InstallableUnitDescription objects
	 */
	private class IUDescriptionQuery extends MatchQuery {
		private InstallableUnitDescription[] fDescriptions;

		public IUDescriptionQuery(InstallableUnitDescription[] descriptions) {
			fDescriptions = descriptions;
		}

		public boolean isMatch(Object object) {
			if (!(object instanceof IInstallableUnit))
				return false;
			if (fDescriptions == null)
				return true;
			IInstallableUnit unit = (IInstallableUnit) object;
			for (int i = 0; i < fDescriptions.length; i++) {
				if (fDescriptions[i].getId().equalsIgnoreCase(unit.getId()) && fDescriptions[i].getVersion().equals(unit.getVersion())) {

					// TODO Having problems slicing non-bundle IUs
					IProvidedCapability[] provided = unit.getProvidedCapabilities();
					for (int j = 0; j < provided.length; j++) {
						if (provided[j].getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_BUNDLE)) {
							return true;
						}
					}

				}
			}
			return false;
		}
	}

	TargetResolver(ITargetDefinition target) {
		fTarget = target;
		// TODO Get proper agent in target metadata area
		fAgent = (IProvisioningAgent) PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
	}

	public IStatus getStatus() {
		return fStatus;
	}

	public Collection getAvailableIUs() {
		return fAvailableIUs;
	}

	public IStatus resolve(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, Messages.TargetDefinition_1, 80);
		fStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.AbstractBundleContainer_0, null);

		try {
			if (fAgent == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
			}

			fAllRepos = new ArrayList();
			fRootIUs = new ArrayList();
			fAvailableIUs = new ArrayList();

			// TODO Handle exceptions more gracefully, or try to continue
			// TODO Give descriptive names to monitor tasks/subtasks

			// Ask locations to generate repositories
			fStatus.add(generateRepos(subMon.newChild(20)));
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			// Combine generated repos and explicit repos
			fStatus.add(loadExplicitRepos(subMon.newChild(20)));
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			// Collect the list of IUs
			fStatus.add(collectRootIUs(subMon.newChild(20)));
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			// Use slicer/planner to get complete enclosure of IUs
			fStatus.add(collectAllIUs(subMon.newChild(20)));
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

		} catch (CoreException e) {
			fStatus.add(e.getStatus());
		}
		return fStatus;
	}

	public IStatus provision(IProgressMonitor monitor) throws CoreException {
		if (fAgent == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IProfileRegistry registry = (IProfileRegistry) fAgent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IEngine engine = (IEngine) fAgent.getService(IEngine.SERVICE_NAME);
		if (engine == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		// TODO Monitor and status handling
		fProfile = null;

		// Delete any previous profiles with the same ID
		registry.removeProfile(PROFILE_NAME);

		// Create the profile
		Properties props = new Properties();
		// TODO Need to set install folder and bundle pool
//		props.setProperty(IProfile.PROP_INSTALL_FOLDER, registryArea.getAbsolutePath());

//		properties.put(IProfile.PROP_INSTALL_FOLDER, AbstractTargetHandle.INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
//		properties.put(IProfile.PROP_CACHE, AbstractTargetHandle.BUNDLE_POOL.toOSString());
//		properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
//		// set up environment & NL properly so OS specific fragments are down loaded/installed
//		properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties());

		fProfile = registry.addProfile(PROFILE_NAME, props);

		// Create the provisioning context
		Set repos = new HashSet();
		for (Iterator iterator = fAllRepos.iterator(); iterator.hasNext();) {
			IRepository currentRepo = (IRepository) iterator.next();
			if (currentRepo instanceof IMetadataRepository) {
				repos.add(currentRepo.getLocation());
			}
		}
		ProvisioningContext context = new ProvisioningContext((URI[]) repos.toArray(new URI[repos.size()]));

		// Get the list of installable units including
		Collection includedIUs = calculateIncludedIUs(null);

		// Create operands to install the metadata
		ArrayList operands = new ArrayList(includedIUs.size());
		Iterator itor = includedIUs.iterator();
		while (itor.hasNext()) {
			operands.add(new InstallableUnitOperand(null, (IInstallableUnit) itor.next()));
		}
		// Add installed property to rootIUS
//		for (Iterator iterator = fRootIUs.iterator(); iterator.hasNext();) {
//			IInstallableUnit unit = (IInstallableUnit) iterator.next();
//			operands.add(new InstallableUnitPropertyOperand(unit, AbstractTargetHandle.PROP_INSTALLED_IU, null, Boolean.toString(true)));
//		}

		PhaseSet phases = DefaultPhaseSet.createDefaultPhaseSet(DefaultPhaseSet.PHASE_CHECK_TRUST | DefaultPhaseSet.PHASE_CONFIGURE | DefaultPhaseSet.PHASE_UNCONFIGURE | DefaultPhaseSet.PHASE_UNINSTALL);
		ProvisioningPlan plan = new ProvisioningPlan(fProfile, (Operand[]) operands.toArray(new Operand[operands.size()]), context);
		IStatus result = engine.perform(plan, phases, null);

		if (!result.isOK() && result.getSeverity() != IStatus.CANCEL) {
			throw new CoreException(result);
		}

		return Status.OK_STATUS;
	}

	public BundleInfo[] getProvisionedBundles() {
		if (fProfile != null) {
			IQuery query = new MatchQuery() {
				public boolean isMatch(Object candidate) {
					if (candidate instanceof IInstallableUnit) {
						IInstallableUnit unit = (IInstallableUnit) candidate;
						IProvidedCapability[] provided = unit.getProvidedCapabilities();
						for (int j = 0; j < provided.length; j++) {
							if (provided[j].getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_BUNDLE)) {
								return true;
							}
						}
					}
					return false;
				}
			};
			List urls = new ArrayList();
			IQueryResult result = fProfile.query(query, null);
			for (Iterator iterator = result.iterator(); iterator.hasNext();) {
				IInstallableUnit unit = (IInstallableUnit) iterator.next();

				// TODO
//				Map bundles = new LinkedHashMap();
//				IFileArtifactRepository repo = getBundlePool(profile);
//				Iterator iterator = collector.iterator();
//				while (iterator.hasNext()) {
//					IInstallableUnit unit = (IInstallableUnit) iterator.next();
//					IArtifactKey[] artifacts = unit.getArtifacts();
//					for (int i = 0; i < artifacts.length; i++) {
//						IArtifactKey key = artifacts[i];
//						File file = repo.getArtifactFile(key);
//						if (file == null) {
//							// TODO: missing bundle
//						} else {
//							IResolvedBundle bundle = generateBundle(file);
//							if (bundle != null) {
//								bundles.put(bundle.getBundleInfo(), bundle);
//							}
//						}
//					}
//				}

			}
		}
		return new BundleInfo[0];
	}

	private IStatus generateRepos(IProgressMonitor monitor) throws CoreException {
		IBundleContainer[] containers = fTarget.getBundleContainers();
		SubMonitor subMon = SubMonitor.convert(monitor, containers.length);
		for (int i = 0; i < containers.length; i++) {
			IRepository[] currentRepos = containers[i].generateRepositories(fAgent, subMon.newChild(1));
			for (int j = 0; j < currentRepos.length; j++) {
				fAllRepos.add(currentRepos[j]);
			}
		}
		return Status.OK_STATUS;
	}

	private IStatus loadExplicitRepos(IProgressMonitor monitor) throws CoreException {
		URI[] explicit = fTarget.getRepositories();
		SubMonitor subMon = SubMonitor.convert(monitor, explicit.length);
		for (int i = 0; i < explicit.length; i++) {
			fAllRepos.add(Publisher.loadMetadataRepository(fAgent, explicit[i], false, false));
			subMon.worked(1);
		}
		return Status.OK_STATUS;
	}

	private IStatus collectRootIUs(IProgressMonitor monitor) throws CoreException {
		IBundleContainer[] containers = fTarget.getBundleContainers();
		SubMonitor subMon = SubMonitor.convert(monitor, containers.length);
		for (int i = 0; i < containers.length; i++) {
			InstallableUnitDescription[] currentIUs = containers[i].getRootIUs(fAgent, subMon.newChild(1));
			for (int j = 0; j < currentIUs.length; j++) {
				fRootIUs.add(currentIUs[j]);
			}
		}
		return Status.OK_STATUS;
	}

	private IStatus collectAllIUs(IProgressMonitor monitor) {
		if (fAllRepos.size() == 0) {
			fAvailableIUs = new ArrayList(0);
			return Status.OK_STATUS;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 60);

		// Combine the repositories into a single queryable object
		IQueryable allRepos;
		if (fAllRepos.size() == 1) {
			allRepos = (IMetadataRepository) fAllRepos.get(0);
		} else {
			allRepos = new CompoundQueryable((IMetadataRepository[]) fAllRepos.toArray(new IMetadataRepository[fAllRepos.size()]));
		}

		// Get the list of root IUs as actual installable units
		InstallableUnitDescription[] rootDescriptions = (InstallableUnitDescription[]) fRootIUs.toArray(new InstallableUnitDescription[fRootIUs.size()]);
		IUDescriptionQuery rootIUQuery = new IUDescriptionQuery(rootDescriptions);
		IQueryResult result = allRepos.query(rootIUQuery, subMon.newChild(10));
		IInstallableUnit[] rootUnits = (IInstallableUnit[]) result.toArray(IInstallableUnit.class);
		if (rootDescriptions.length != rootUnits.length) {
			// TODO Return a warning status?
		}

		// Create slicer to calculate requirements
		PermissiveSlicer slicer = null;
		Properties props = new Properties();
		// TODO How to handle platform specific problems
		props.setProperty("osgi.os", fTarget.getOS() != null ? fTarget.getOS() : Platform.getOS()); //$NON-NLS-1$
		props.setProperty("osgi.ws", fTarget.getWS() != null ? fTarget.getWS() : Platform.getWS()); //$NON-NLS-1$
		props.setProperty("osgi.arch", fTarget.getArch() != null ? fTarget.getArch() : Platform.getOSArch()); //$NON-NLS-1$
		props.setProperty("osgi.nl", fTarget.getNL() != null ? fTarget.getNL() : Platform.getNL()); //$NON-NLS-1$
		slicer = new PermissiveSlicer(allRepos, props, true, false, false, true, false);
		subMon.worked(10);

		// Run the slicer and collect units from the result
		IQueryable slice = slicer.slice(rootUnits, subMon.newChild(30));
		if (slice == null) {
			return slicer.getStatus();
		}
		IQueryResult collector = slice.query(InstallableUnitQuery.ANY, subMon.newChild(10));

		fAvailableIUs = collector.toCollection();
		return Status.OK_STATUS;

		// TODO Could support multiple environments in the target
//		if (getIncludeAllEnvironments()) {
//			slicer = new PermissiveSlicer(allMetadata, new Properties(), true, false, true, true, false);
//		} else {

	}

	public Collection calculateMissingIUs(IProgressMonitor monitor) {
		// TODO Copy logic from other method?
		return null;
	}

	public Collection calculateIncludedIUs(IProgressMonitor monitor) {
		// TODO Move to TargetDefinition to allow cacheing 
		// TODO We no longer support returning a status for missing included bundles
		// VERSION DOES NOT EXIST
//		int sev = IStatus.ERROR;
//		String message = NLS.bind(Messages.AbstractBundleContainer_1, new Object[] {info.getVersion(), info.getSymbolicName()});
//		if (optional) {
//			sev = IStatus.INFO;
//			message = NLS.bind(Messages.AbstractBundleContainer_2, new Object[] {info.getVersion(), info.getSymbolicName()});
//		}
//		return new ResolvedBundle(info, parentContainer, new Status(sev, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, message, null), null, optional, false);

		BundleInfo[] included = fTarget.getIncluded();
		BundleInfo[] optional = fTarget.getOptional();
		if (included == null && optional == null) {
			return fAvailableIUs;
		}

		// Map unit names to associated unit
		Map bundleMap = new HashMap(fAvailableIUs.size());
		for (Iterator iterator = fAvailableIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			List list = (List) bundleMap.get(unit.getId());
			if (list == null) {
				list = new ArrayList(3);
				bundleMap.put(unit.getId(), list);
			}
			list.add(unit);
		}

		List includedIUs = new ArrayList();

		// Add included bundles
		if (included == null) {
			includedIUs.addAll(fAvailableIUs);
		} else {
			for (int i = 0; i < included.length; i++) {
				BundleInfo include = included[i];
				IInstallableUnit bestUnit = determineBestUnit(bundleMap, include);
				if (bestUnit != null) {
					includedIUs.add(bestUnit);
				}
			}
		}

		// Add optional bundles
		if (optional != null) {
			for (int i = 0; i < optional.length; i++) {
				BundleInfo option = optional[i];
				IInstallableUnit bestUnit = determineBestUnit(bundleMap, option);
				if (bestUnit != null && !includedIUs.contains(bestUnit)) {
					includedIUs.add(bestUnit);
				}
			}
		}

		return includedIUs;
	}

	private static IInstallableUnit determineBestUnit(Map unitMap, BundleInfo info) {
		// TODO We no longer have a way to return a status if a specific included bundle cannot be found
		List list = (List) unitMap.get(info.getSymbolicName());
		if (list != null) {
			// If there is a version set, select the specific version if available, select newest otherwise 
			if (info.getVersion() != null) {
				Version version = Version.create(info.getVersion());
				Iterator iterator = list.iterator();
				while (iterator.hasNext()) {
					IInstallableUnit unit = (IInstallableUnit) iterator.next();
					if (version.equals(unit.getVersion())) {
						return unit;
					}
				}
			}

			// If there is no version set, select newest available
			if (list.size() > 1) {
				// sort the list
				Collections.sort(list, new Comparator() {
					public int compare(Object o1, Object o2) {
						Version v1 = ((IInstallableUnit) o1).getVersion();
						Version v2 = ((IInstallableUnit) o2).getVersion();
						return v1.compareTo(v2);
					}
				});
			}
			// select the last one
			return (IInstallableUnit) list.get(list.size() - 1);
		}
		return null;
	}
}
