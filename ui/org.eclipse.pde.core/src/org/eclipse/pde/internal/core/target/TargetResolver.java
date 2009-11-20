package org.eclipse.pde.internal.core.target;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Helper class for TargetDefinition to encapsulate code for resolving a target.
 * 
 * @since 3.6
 * @see TargetDefinition
 */
public class TargetResolver {

	private ITargetDefinition fTarget;
	private IProvisioningAgent fAgent;

	private IStatus fStatus;
	private Collection fAvailableIUs;
	private Collection fMissingNames;

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
				if (fDescriptions[i].getId().equalsIgnoreCase(unit.getId()) && fDescriptions[i].getVersion().equals(unit.getVersion()))
					return true;
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

	public Collection getMissingIUNames() {
		return fMissingNames;
	}

	public IStatus resolve(IProgressMonitor monitor) throws CoreException {
		fStatus = null;
		if (fAgent == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		SubMonitor subMon = SubMonitor.convert(monitor, Messages.TargetDefinition_1, 80);

		try {

			// TODO Handle exceptions more gracefully, or try to continue
			// TODO Give descriptive names to monitor tasks/subtasks

			// Ask locations to generate repositories
			List repos = generateRepos(subMon.newChild(20));
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			// Combine generated repos and explicit repos
			List explicit = loadExplicitRepos(subMon.newChild(20));
			repos.addAll(explicit);
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			// Collect the list of IUs
			List rootIUs = collectRootIUs(subMon.newChild(20));
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			// Use slicer/planner to get complete enclosure of IUs
			collectAllIUs(rootIUs, repos, subMon.newChild(20));
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			fStatus = Status.OK_STATUS;
			return fStatus;

		} catch (CoreException e) {
			fStatus = e.getStatus();
			throw e;
		}
	}

	private List generateRepos(IProgressMonitor monitor) throws CoreException {
		List repos = new ArrayList();
		IBundleContainer[] containers = fTarget.getBundleContainers();
		SubMonitor subMon = SubMonitor.convert(monitor, containers.length);
		for (int i = 0; i < containers.length; i++) {
			IRepository[] currentRepos = containers[i].generateRepositories(fAgent, subMon.newChild(1));
			for (int j = 0; j < currentRepos.length; j++) {
				repos.add(currentRepos[j]);
			}
		}
		return repos;
	}

	private List loadExplicitRepos(IProgressMonitor monitor) throws CoreException {
		List repos = new ArrayList();
		URI[] explicit = fTarget.getRepositories();
		SubMonitor subMon = SubMonitor.convert(monitor, explicit.length);
		for (int i = 0; i < explicit.length; i++) {
			repos.add(Publisher.loadMetadataRepository(fAgent, explicit[i], false, false));
			subMon.worked(1);
		}
		return repos;
	}

	private List collectRootIUs(IProgressMonitor monitor) throws CoreException {
		List ius = new ArrayList();
		IBundleContainer[] containers = fTarget.getBundleContainers();
		SubMonitor subMon = SubMonitor.convert(monitor, containers.length);
		for (int i = 0; i < containers.length; i++) {
			InstallableUnitDescription[] currentIUs = containers[i].getRootIUs(fAgent, subMon.newChild(1));
			for (int j = 0; j < currentIUs.length; j++) {
				ius.add(currentIUs[j]);
			}
		}
		return ius;
	}

	private IStatus collectAllIUs(List rootIUS, List repos, IProgressMonitor monitor) {
		if (repos.size() == 0) {
			fAvailableIUs = new ArrayList(0);
			return Status.OK_STATUS;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 60);

		// Combine the repositories into a single queryable object
		IQueryable allRepos;
		if (repos.size() == 1) {
			allRepos = (IMetadataRepository) repos.get(0);
		} else {
			allRepos = new CompoundQueryable((IMetadataRepository[]) repos.toArray(new IMetadataRepository[repos.size()]));
		}

		// Get the list of root IUs as actual installable units
		InstallableUnitDescription[] rootDescriptions = (InstallableUnitDescription[]) rootIUS.toArray(new InstallableUnitDescription[rootIUS.size()]);
		IUDescriptionQuery rootIUQuery = new IUDescriptionQuery(rootDescriptions);
		Collector result = new Collector();
		allRepos.query(rootIUQuery, result, subMon.newChild(10));
		IInstallableUnit[] rootUnits = (IInstallableUnit[]) result.toArray(IInstallableUnit.class);
		if (rootDescriptions.length != rootUnits.length) {
			// TODO Return a warning status?
		}

		// Create slicer to calculate requirements
		PermissiveSlicer slicer = null;
		// TODO How to handle platform specific problems
		Properties props = new Properties();
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
		Collector collector = slice.query(InstallableUnitQuery.ANY, new Collector(), subMon.newChild(10));

		fAvailableIUs = collector.toCollection();
		return Status.OK_STATUS;

		// TODO Could support multiple environments in the target
//		if (getIncludeAllEnvironments()) {
//			slicer = new PermissiveSlicer(allMetadata, new Properties(), true, false, true, true, false);
//		} else {

	}

	public Collection getIncludedIUs(IProgressMonitor monitor) {
		// TODO Fix logic, move to TargetDefinition to allow cacheing 

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

		// TODO
		return fAvailableIUs;

//		for (int i = 0; i < collection.length; i++) {
//			IResolvedBundle resolved = collection[i];
//		}
//		List resolved = new ArrayList();
//		if (included == null) {
//			for (int i = 0; i < collection.length; i++) {
//				resolved.add(collection[i]);
//			}
//		} else {
//			for (int i = 0; i < included.length; i++) {
//				BundleInfo info = included[i];
//				resolved.add(resolveBundle(bundleMap, info, false, parentContainer));
//			}
//		}
//		if (optional != null) {
//			for (int i = 0; i < optional.length; i++) {
//				BundleInfo option = optional[i];
//				IResolvedBundle resolveBundle = resolveBundle(bundleMap, option, true, parentContainer);
//				IStatus status = resolveBundle.getStatus();
//				if (status.isOK()) {
//					// add to list if not there already
//					if (!resolved.contains(resolveBundle)) {
//						resolved.add(resolveBundle);
//					}
//				} else {
//					// missing optional bundle - add it to the list
//					resolved.add(resolveBundle);
//				}
//			}
//		}
//		return (IResolvedBundle[]) resolved.toArray(new IResolvedBundle[resolved.size()]);
	}

	public Collection getMissingIUs(IProgressMonitor monitor) {
		// TODO Fix logic, move to TargetDefinition to allow caching 
		return new ArrayList(0);
	}

//	/**
//	 * Returns the existing profile for this target definition or <code>null</code> if none.
//	 *  
//	 * @return profile or <code>null</code>
//	 */
//	public IProfile findProfile() {
//		IProfileRegistry registry = AbstractTargetHandle.getProfileRegistry();
//		if (registry != null) {
//			AbstractTargetHandle handle = ((AbstractTargetHandle) getHandle());
//			String id;
//			try {
//				id = handle.getProfileId();
//				return registry.getProfile(id);
//			} catch (CoreException e) {
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Returns whether software site containers are configured to provision for all environments
//	 * versus a single environment.
//	 * 
//	 * @return whether all environments will be provisioned
//	 */
//	private boolean isAllEnvironments() {
//		IBundleContainer[] containers = getBundleContainers();
//		if (containers != null) {
//			for (int i = 0; i < containers.length; i++) {
//				if (containers[i] instanceof IUBundleContainer) {
//					IUBundleContainer iu = (IUBundleContainer) containers[i];
//					if (iu.getIncludeAllEnvironments()) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * Returns the mode used to provision this target - slice versus plan or <code>null</code> if
//	 * this target has no software sites.
//	 * 
//	 * @return provisioning mode or <code>null</code>
//	 */
//	private String getProvisionMode() {
//		IBundleContainer[] containers = getBundleContainers();
//		if (containers != null) {
//			for (int i = 0; i < containers.length; i++) {
//				if (containers[i] instanceof IUBundleContainer) {
//					IUBundleContainer iu = (IUBundleContainer) containers[i];
//					if (iu.getIncludeAllRequired()) {
//						return TargetDefinitionPersistenceHelper.MODE_PLANNER;
//					}
//					return TargetDefinitionPersistenceHelper.MODE_SLICER;
//				}
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Returns the profile for the this target handle, creating one if required.
//	 * 
//	 * @return profile
//	 * @throws CoreException in unable to retrieve profile
//	 */
//	public IProfile getProfile() throws CoreException {
//		IProfileRegistry registry = AbstractTargetHandle.getProfileRegistry();
//		if (registry == null) {
//			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AbstractTargetHandle_0));
//		}
//		AbstractTargetHandle handle = ((AbstractTargetHandle) getHandle());
//		String id = handle.getProfileId();
//		IProfile profile = registry.getProfile(id);
//		if (profile != null) {
//			boolean recreate = false;
//			// check if all environments setting is the same
//			boolean all = false;
//			String value = profile.getProperty(AbstractTargetHandle.PROP_ALL_ENVIRONMENTS);
//			if (value != null) {
//				all = Boolean.valueOf(value).booleanValue();
//				if (!Boolean.toString(isAllEnvironments()).equals(value)) {
//					recreate = true;
//				}
//			}
//			// ensure environment & NL settings are still the same (else we need a new profile)
//			String property = null;
//			if (!recreate && !all) {
//				property = generateEnvironmentProperties();
//				value = profile.getProperty(IProfile.PROP_ENVIRONMENTS);
//				if (!property.equals(value)) {
//					recreate = true;
//				}
//			}
//			// check provisioning mode: slice versus plan
//			String mode = getProvisionMode();
//			if (mode != null) {
//				value = profile.getProperty(AbstractTargetHandle.PROP_PROVISION_MODE);
//				if (!mode.equals(value)) {
//					recreate = true;
//				}
//			}
//
//			if (!recreate) {
//				property = generateNLProperty();
//				value = profile.getProperty(IProfile.PROP_NL);
//				if (!property.equals(value)) {
//					recreate = true;
//				}
//			}
//			if (!recreate) {
//				// check top level IU's. If any have been removed from the containers that are
//				// still in the profile, we need to recreate (rather than uninstall)
//				IUProfilePropertyQuery propertyQuery = new IUProfilePropertyQuery(AbstractTargetHandle.PROP_INSTALLED_IU, Boolean.toString(true));
//				propertyQuery.setProfile(profile);
//				Collector collector = profile.query(propertyQuery, new Collector(), null);
//				Iterator iterator = collector.iterator();
//				if (iterator.hasNext()) {
//					Set installedIUs = new HashSet();
//					while (iterator.hasNext()) {
//						IInstallableUnit unit = (IInstallableUnit) iterator.next();
//						installedIUs.add(new NameVersionDescriptor(unit.getId(), unit.getVersion().toString()));
//					}
//					IBundleContainer[] containers = getBundleContainers();
//					if (containers != null) {
//						for (int i = 0; i < containers.length; i++) {
//							if (containers[i] instanceof IUBundleContainer) {
//								IUBundleContainer bc = (IUBundleContainer) containers[i];
//								String[] ids = bc.getIds();
//								Version[] versions = bc.getVersions();
//								for (int j = 0; j < versions.length; j++) {
//									installedIUs.remove(new NameVersionDescriptor(ids[j], versions[j].toString()));
//								}
//							}
//						}
//					}
//					if (!installedIUs.isEmpty()) {
//						recreate = true;
//					}
//				}
//			}
//			if (recreate) {
//				handle.deleteProfile();
//				profile = null;
//			}
//		}
//		if (profile == null) {
//			// create profile
//			Map properties = new HashMap();
//			properties.put(IProfile.PROP_INSTALL_FOLDER, AbstractTargetHandle.INSTALL_FOLDERS.append(Long.toString(LocalTargetHandle.nextTimeStamp())).toOSString());
//			properties.put(IProfile.PROP_CACHE, AbstractTargetHandle.BUNDLE_POOL.toOSString());
//			properties.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
//			// set up environment & NL properly so OS specific fragments are down loaded/installed
//			properties.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties());
//			properties.put(IProfile.PROP_NL, generateNLProperty());
//			String mode = getProvisionMode();
//			if (mode != null) {
//				properties.put(AbstractTargetHandle.PROP_PROVISION_MODE, mode);
//				properties.put(AbstractTargetHandle.PROP_ALL_ENVIRONMENTS, Boolean.toString(isAllEnvironments()));
//			}
//			profile = registry.addProfile(id, properties);
//		}
//		return profile;
//	}
//
//	/**
//	 * Generates the environment properties string for this target definition's p2 profile.
//	 * 
//	 * @return environment properties
//	 */
//	private String generateEnvironmentProperties() {
//		// TODO: are there constants for these keys?
//		StringBuffer env = new StringBuffer();
//		String ws = getWS();
//		if (ws == null) {
//			ws = Platform.getWS();
//		}
//		env.append("osgi.ws="); //$NON-NLS-1$
//		env.append(ws);
//		env.append(","); //$NON-NLS-1$
//		String os = getOS();
//		if (os == null) {
//			os = Platform.getOS();
//		}
//		env.append("osgi.os="); //$NON-NLS-1$
//		env.append(os);
//		env.append(","); //$NON-NLS-1$
//		String arch = getArch();
//		if (arch == null) {
//			arch = Platform.getOSArch();
//		}
//		env.append("osgi.arch="); //$NON-NLS-1$
//		env.append(arch);
//		return env.toString();
//	}
//
//	/**
//	 * Generates the NL property for this target definition's p2 profile.
//	 * 
//	 * @return NL profile property
//	 */
//	private String generateNLProperty() {
//		String nl = getNL();
//		if (nl == null) {
//			nl = Platform.getNL();
//		}
//		return nl;
//	}

}
