package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Helper class for TargetDefinition to encapsulate code for resolving a target.
 * 
 * @since 3.6
 * @see TargetProvisioner
 * @see TargetDefinition
 */
public class TargetResolver {

	private ITargetDefinition fTarget;

	private MultiStatus fStatus;
	private List fMetaRepos;
	private List fArtifactRepos;
	private List fRootIUs;
	private Collection fAvailableIUs;

	TargetResolver(ITargetDefinition target) {
		fTarget = target;
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

		IProvisioningAgent agent;
		try {
			agent = TargetPlatformService.getProvisioningAgent();
		} catch (CoreException e) {
			fStatus.add(e.getStatus());
			return fStatus;
		}

		try {
			fMetaRepos = new ArrayList();
			fArtifactRepos = new ArrayList();
			fRootIUs = new ArrayList();
			fAvailableIUs = new ArrayList();

			// Ask locations to generate repositories
			subMon.subTask(Messages.TargetResolver_generateLocalMetadataTask);
			IStatus result = generateRepos(agent, subMon.newChild(30));
			if (!result.isOK()) {
				fStatus.add(result);
			}
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			// Combine generated repos and explicit repos
			subMon.subTask(Messages.TargetResolver_checkRemoteRepoTask);
			result = loadExplicitRepos(agent, subMon.newChild(10));
			if (!result.isOK()) {
				fStatus.add(result);
			}
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			// Collect the list of IUs
			subMon.subTask(Messages.TargetResolver_findPluginSetTask);
			result = collectRootIUs(subMon.newChild(10));
			if (!result.isOK()) {
				// If one or more locations had problems loading, don't show warnings for empty locations
				if (fStatus.getSeverity() != IStatus.ERROR || result.getSeverity() != IStatus.WARNING) {
					fStatus.add(result);
				}
			}
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			// Use slicer/planner to get complete enclosure of IUs
			result = collectAllIUs(subMon.newChild(30));
			if (!result.isOK()) {
				fStatus.add(result);
			}
			if (subMon.isCanceled()) {
				fStatus.add(Status.CANCEL_STATUS);
				return fStatus;
			}

			subMon.subTask(""); //$NON-NLS-1$

		} catch (CoreException e) {
			fStatus.add(e.getStatus());
		} finally {
			agent.stop();
		}
		subMon.done();
		return fStatus;
	}

	private IStatus generateRepos(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		// Clear temp repo location if it exists
		IPath repoPath = TargetPlatformService.getRepositoryLocation(fTarget);
		File repoLocation = new File(repoPath.toOSString());
		delete(repoLocation);

		// Ask each bundle container to generate its repositories
		MultiStatus repoStatus = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetResolver_problemsReadingLocal, null);
		IBundleContainer[] containers = fTarget.getBundleContainers();
		SubMonitor subMon = SubMonitor.convert(monitor, containers.length);
		for (int i = 0; i < containers.length; i++) {
			try {
				IRepository[] currentRepos = containers[i].generateRepositories(agent, repoPath.append(Integer.toString(i)), subMon.newChild(1));
				for (int j = 0; j < currentRepos.length; j++) {
					if (currentRepos[j] instanceof IMetadataRepository) {
						fMetaRepos.add(currentRepos[j]);
					} else if (currentRepos[j] instanceof IArtifactRepository) {
						fArtifactRepos.add(currentRepos[j]);
					}
				}
			} catch (CoreException e) {
				repoStatus.add(e.getStatus());
			}
		}
		if (repoStatus.getChildren().length == 1) {
			return repoStatus.getChildren()[0];
		}
		return repoStatus;
	}

	/**
	 * Recursively deletes folder and files.
	 * 
	 * @param folder
	 */
	private void delete(File folder) {
		if (folder.isFile()) {
			folder.delete();
		} else if (folder.isDirectory()) {
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
	}

	private IStatus loadExplicitRepos(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (metaManager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (artifactManager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		MultiStatus result = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetResolver_unableToLoadRepositories, null);

		URI[] explicit = fTarget.getRepositories();
		SubMonitor subMon = SubMonitor.convert(monitor, explicit.length * 4);
		for (int i = 0; i < explicit.length; i++) {
			try {
				fMetaRepos.add(metaManager.loadRepository(explicit[i], subMon.newChild(2)));
				fArtifactRepos.add(artifactManager.loadRepository(explicit[i], subMon.newChild(2)));
			} catch (ProvisionException e) {
				result.add(e.getStatus());
			}
		}
		if (!result.isOK()) {
			if (result.getChildren().length == 1) {
				return result.getChildren()[0];
			}
			return result;
		}
		return Status.OK_STATUS;
	}

	private IStatus collectRootIUs(IProgressMonitor monitor) throws CoreException {
		MultiStatus resultCollector = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetResolver_someLocationsDontContainPlugins, null);
		IBundleContainer[] containers = fTarget.getBundleContainers();
		SubMonitor subMon = SubMonitor.convert(monitor, containers.length);
		for (int i = 0; i < containers.length; i++) {
			InstallableUnitDescription[] currentIUs = containers[i].getRootIUs();
			if (currentIUs != null && currentIUs.length > 0) {
				for (int j = 0; j < currentIUs.length; j++) {
					fRootIUs.add(currentIUs[j]);
				}
			} else if (containers[i] instanceof AbstractLocalBundleContainer && !(containers[i] instanceof FeatureBundleContainer)) {
				resultCollector.add(new Status(IStatus.WARNING, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetResolver_noPluginsFound, ((AbstractLocalBundleContainer) containers[i]).getLocation(true))));
			}
			subMon.worked(1);
		}
		if (resultCollector.isOK()) {
			return Status.OK_STATUS;
		}
		if (resultCollector.getChildren().length == 1) {
			return resultCollector.getChildren()[0];
		}
		return resultCollector;
	}

	private IStatus collectAllIUs(IProgressMonitor monitor) {
		if (fMetaRepos.size() == 0) {
			fAvailableIUs = new ArrayList(0);
			return Status.OK_STATUS;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, 60);

		// Combine the repositories into a single queryable object
		IQueryable allRepos;
		if (fMetaRepos.size() == 1) {
			allRepos = (IMetadataRepository) fMetaRepos.get(0);
		} else {
			allRepos = new CompoundQueryable(fMetaRepos);
		}

		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetResolver_problemsCollectingPluginSet, null);

		// Get the list of root IUs as actual installable units
		InstallableUnitDescription[] rootDescriptions = (InstallableUnitDescription[]) fRootIUs.toArray(new InstallableUnitDescription[fRootIUs.size()]);
		List rootUnits = new ArrayList();
		for (int i = 0; i < rootDescriptions.length; i++) {
			InstallableUnitQuery query = new InstallableUnitQuery(rootDescriptions[i].getId(), rootDescriptions[i].getVersion());
			IQueryResult result = allRepos.query(query, null);
			if (result.isEmpty()) {
				status.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetResolver_couldNotFindUnit, new String[] {rootDescriptions[i].getId(), rootDescriptions[i].getVersion().toString()})));
			}
			rootUnits.add(result.iterator().next());
		}
		subMon.worked(10);

		// Create slicer to calculate requirements
		PermissiveSlicer slicer = null;
		Properties props = new Properties();
		// TODO How to handle platform specific installable units
//		props.setProperty("osgi.os", fTarget.getOS() != null ? fTarget.getOS() : Platform.getOS()); //$NON-NLS-1$
//		props.setProperty("osgi.ws", fTarget.getWS() != null ? fTarget.getWS() : Platform.getWS()); //$NON-NLS-1$
//		props.setProperty("osgi.arch", fTarget.getArch() != null ? fTarget.getArch() : Platform.getOSArch()); //$NON-NLS-1$
//		props.setProperty("osgi.nl", fTarget.getNL() != null ? fTarget.getNL() : Platform.getNL()); //$NON-NLS-1$
		slicer = new PermissiveSlicer(allRepos, props, true, false, true, true, false);
		subMon.worked(10);

		// Run the slicer and collect units from the result
		IQueryable slice = slicer.slice((IInstallableUnit[]) rootUnits.toArray(new IInstallableUnit[rootUnits.size()]), subMon.newChild(30));
		if (slice == null) {
			status.add(slicer.getStatus());
		} else {
			IQueryResult collector = slice.query(InstallableUnitQuery.ANY, subMon.newChild(10));
			fAvailableIUs = collector.toSet();
		}

		if (!status.isOK()) {
			if (status.getChildren().length == 1) {
				return status.getChildren()[0];
			}
			return status;
		}
		return Status.OK_STATUS;
	}

	public Collection calculateMissingIUs(IProgressMonitor monitor) {
		return null;
	}

	/**
	 * @return List of {@link IMetadataRepository} that were loaded during the resolve
	 */
	public List getMetadataRepositories() {
		return fMetaRepos;
	}

	/**
	 * @return List of {@link IArtifactRepository} that were loaded during the resolve
	 */
	public List getArtifactRepositories() {
		return fArtifactRepos;
	}

	public Collection calculateIncludedIUs() {
		InstallableUnitDescription[] included = fTarget.getIncluded();
		InstallableUnitDescription[] optional = fTarget.getOptional();
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
				InstallableUnitDescription include = included[i];
				IInstallableUnit bestUnit = determineBestUnit(bundleMap, include);
				if (bestUnit != null) {
					includedIUs.add(bestUnit);
				}
			}
		}

		// Add optional bundles
		if (optional != null) {
			for (int i = 0; i < optional.length; i++) {
				InstallableUnitDescription option = optional[i];
				IInstallableUnit bestUnit = determineBestUnit(bundleMap, option);
				if (bestUnit != null && !includedIUs.contains(bestUnit)) {
					includedIUs.add(bestUnit);
				}
			}
		}

		return includedIUs;
	}

	public IInstallableUnit getUnit(InstallableUnitDescription unit) {
		// Combine the repositories into a single queryable object
		IQueryable allRepos;
		if (fMetaRepos.size() == 1) {
			allRepos = (IMetadataRepository) fMetaRepos.get(0);
		} else {
			allRepos = new CompoundQueryable(fMetaRepos);
		}

		// Look for the requested unit
		InstallableUnitQuery query = new InstallableUnitQuery(unit.getId(), unit.getVersion());
		IQueryResult result = allRepos.query(query, null);
		if (!result.isEmpty()) {
			return (IInstallableUnit) result.iterator().next();
		}
		return null;
	}

	private static IInstallableUnit determineBestUnit(Map unitMap, InstallableUnitDescription info) {
		List list = (List) unitMap.get(info.getId());
		if (list != null) {
			// If there is a version set, select the specific version if available, select newest otherwise 
			if (info.getVersion() != null) {
				Iterator iterator = list.iterator();
				while (iterator.hasNext()) {
					IInstallableUnit unit = (IInstallableUnit) iterator.next();
					if (info.getVersion().equals(unit.getVersion())) {
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
