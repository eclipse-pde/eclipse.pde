/*******************************************************************************
 * Copyright (c) 2010, 2015 EclipseSource Inc. and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 477527
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;

/**
 * This job exports the bundles and features that make up your target.
 */
public class ExportTargetJob extends Job {

	private final URI fDestination;
	private boolean fclearDestinationDirectory = true;
	private IFileStore featureDir;
	private IFileStore pluginDir;
	private IFileSystem fileSystem;
	private Map<String, NameVersionDescriptor[]> filter;
	private final ITargetDefinition fTarget;

	public ExportTargetJob(ITargetDefinition target, URI destination, boolean clearDestinationDirectory) {
		super("Export Current Target Definition"); //$NON-NLS-1$
		fTarget = target;
		fDestination = destination;
		fclearDestinationDirectory = clearDestinationDirectory;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			constructFilter(fTarget);
			ITargetLocation[] containers = fTarget.getTargetLocations();
			if (containers == null) {
				containers = new ITargetLocation[0];
			}
			int totalWork = containers.length;
			monitor.beginTask(PDECoreMessages.ExportTargetDefinition_task, totalWork);

			monitor.subTask(PDECoreMessages.ExportTargetJob_ConfiguringDestination);
			setupDestination(monitor);

			monitor.subTask(PDECoreMessages.ExportTargetJob_ExportingTargetContents);
			for (ITargetLocation targetLocation : containers) {
				ITargetLocation container = targetLocation;
				container.resolve(fTarget, monitor);
				if (!(container instanceof IUBundleContainer)) {
					exportContainer(container, fTarget, featureDir, pluginDir, fileSystem, monitor);
				}
			}
			exportProfile(fTarget, fDestination, monitor);
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Failed to export the target", e); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void constructFilter(ITargetDefinition target) {
		NameVersionDescriptor[] included = target.getIncluded();
		if (included == null) {
			return;
		}
		filter = new HashMap<>();
		for (NameVersionDescriptor inclusion : included) {
			NameVersionDescriptor[] versions = filter.get(inclusion.getId());
			if (versions == null) {
				filter.put(inclusion.getId(), new NameVersionDescriptor[] {inclusion});
			} else {
				NameVersionDescriptor[] versions2 = new NameVersionDescriptor[versions.length + 1];
				System.arraycopy(versions, 0, versions2, 0, versions.length);
				versions2[versions.length] = inclusion;
				filter.put(inclusion.getId(), versions2);
			}
		}
	}

	private void setupDestination(IProgressMonitor monitor) throws CoreException {
		fileSystem = EFS.getLocalFileSystem();
		if (!fileSystem.canWrite()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Destination directory not writable.")); //$NON-NLS-1$
		}
		IFileStore destination = fileSystem.getStore(fDestination);
		featureDir = destination.getChild("features"); //$NON-NLS-1$ExportTargetJob
		pluginDir = destination.getChild("plugins"); //$NON-NLS-1$
		if (fclearDestinationDirectory) {
			monitor.subTask(PDECoreMessages.ExportTargetDeleteOldData); //Deleting old data...
			featureDir.delete(EFS.NONE, null);
			pluginDir.delete(EFS.NONE, null);
		}
		featureDir.mkdir(EFS.NONE, null);
		pluginDir.mkdir(EFS.NONE, null);
	}

	private boolean shouldExport(NameVersionDescriptor descriptor) {
		// currently PDE does not selectively include/exclude features
		if (filter == null || descriptor.getType().equals(NameVersionDescriptor.TYPE_FEATURE)) {
			return true;
		}
		NameVersionDescriptor[] versions = filter.get(descriptor.getId());
		if (versions == null) {
			return false;
		}
		for (NameVersionDescriptor nameVersionDescriptor : versions) {
			String version = nameVersionDescriptor.getVersion();
			if ((version == null || version.equals(descriptor.getVersion())) && descriptor.getType().equals(nameVersionDescriptor.getType())) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldExport(TargetFeature feature) {
		if (filter == null) {
			return true;
		}
		NameVersionDescriptor descriptor = new NameVersionDescriptor(feature.getId(), feature.getVersion(), NameVersionDescriptor.TYPE_FEATURE);
		return shouldExport(descriptor);
	}

	private boolean shouldExport(TargetBundle bundle) {
		if (filter == null) {
			return true;
		}
		NameVersionDescriptor descriptor = new NameVersionDescriptor(bundle.getBundleInfo().getSymbolicName(), bundle.getBundleInfo().getVersion(), NameVersionDescriptor.TYPE_PLUGIN);
		return shouldExport(descriptor);
	}

	private boolean shouldExport(IInstallableUnit iu) {
		if (filter == null) {
			return true;
		}
		NameVersionDescriptor descriptor = null;
		String feature = getCapability(iu, "org.eclipse.update.feature"); //$NON-NLS-1$
		if (feature != null) {
			descriptor = new NameVersionDescriptor(feature, iu.getVersion().toString(), NameVersionDescriptor.TYPE_FEATURE);
		} else if (iu.getId().endsWith(".feature.group")) { //$NON-NLS-1$
			descriptor = new NameVersionDescriptor(iu.getId(), iu.getVersion().toString(), NameVersionDescriptor.TYPE_FEATURE);
		} else if ("bundle".equalsIgnoreCase(getCapability(iu, "org.eclipse.equinox.p2.eclipse.type"))) { //$NON-NLS-1$ //$NON-NLS-2$
			descriptor = new NameVersionDescriptor(iu.getId(), iu.getVersion().toString(), NameVersionDescriptor.TYPE_PLUGIN);
		} else if ("source".equalsIgnoreCase(getCapability(iu, "org.eclipse.equinox.p2.eclipse.type"))) { //$NON-NLS-1$ //$NON-NLS-2$
			descriptor = new NameVersionDescriptor(iu.getId(), iu.getVersion().toString(), NameVersionDescriptor.TYPE_PLUGIN);
		}
		// default to true unless we know otherwise. This ensures that random metadata bits
		// are moved over to the target as they might be needed in a future provisioning operation.
		// We could move only unknown IUs that do NOT have artifacts... Have to think about that.
		return descriptor == null ? true : shouldExport(descriptor);
	}

	private String getCapability(IInstallableUnit iu, String namespace) {
		for (IProvidedCapability capability : iu.getProvidedCapabilities()) {
			if (capability.getNamespace().equals(namespace)) {
				return capability.getName();
			}
		}
		return null;
	}

	private void exportContainer(ITargetLocation container, ITargetDefinition target, IFileStore featureDir, IFileStore pluginDir, IFileSystem fileSystem, IProgressMonitor monitor) throws CoreException {
		TargetFeature[] features = container.getFeatures();
		if (features != null) {
			monitor.subTask(PDECoreMessages.ExportTargetExportFeatures);
			for (TargetFeature feature : features) {
				if (shouldExport(feature)) {
					copy(feature.getLocation(), featureDir, fileSystem, monitor);
				}
			}
		}

		TargetBundle[] bundles = container.getBundles();
		if (bundles != null) {
			monitor.subTask(PDECoreMessages.ExportTargetExportPlugins);
			for (TargetBundle bundle : bundles) {
				if (shouldExport(bundle)) {
					copy(bundle.getBundleInfo().getLocation().getPath(), pluginDir, fileSystem, monitor);
				}
			}
		}
	}

	private IStatus copy(String src, IFileStore destinationParent, IFileSystem fileSystem, IProgressMonitor monitor) throws CoreException {
		Path srcPath = new Path(src);

		IFileStore source = fileSystem.getStore(srcPath);
		String elementName = srcPath.segment(srcPath.segmentCount() - 1);
		IFileStore destination = destinationParent.getChild(elementName);

		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);

		if (destination.fetchInfo().exists()) {
			return Status.OK_STATUS;
		}
		if (source.fetchInfo().isDirectory()) {
			destination.mkdir(EFS.NONE, new NullProgressMonitor());
		}
		source.copy(destination, EFS.OVERWRITE, subMonitor.split(1));
		return Status.OK_STATUS;
	}

	private RepositoryDescriptor createRepoDescriptor(URI location, String name, String kind) {
		RepositoryDescriptor result = new RepositoryDescriptor();
		result.setLocation(location);
		result.setKind(kind);
		result.setName(name);
		if (fclearDestinationDirectory) {
			result.setAppend(false);
		}
		return result;
	}

	private void exportProfile(ITargetDefinition target, URI destination, IProgressMonitor monitor) throws CoreException {
		Repo2Runnable exporter = new Repo2Runnable();
		exporter.addDestination(createRepoDescriptor(destination, P2TargetUtils.getProfileId(target), RepositoryDescriptor.KIND_METADATA));
		exporter.addDestination(createRepoDescriptor(destination, P2TargetUtils.getProfileId(target), RepositoryDescriptor.KIND_ARTIFACT));
		exporter.addSource(createRepoDescriptor(P2TargetUtils.getBundlePool().getLocation(), null, RepositoryDescriptor.KIND_ARTIFACT));

		IQueryResult<?> ius = P2TargetUtils.getIUs(target, monitor);
		ArrayList<IInstallableUnit> toExport = new ArrayList<>();
		for (Object installableUnit : ius) {
			IInstallableUnit iu = (IInstallableUnit) installableUnit;
			if (shouldExport(iu)) {
				toExport.add(iu);
			}
		}
		exporter.setSourceIUs(toExport);
		exporter.run(monitor);
	}
}
