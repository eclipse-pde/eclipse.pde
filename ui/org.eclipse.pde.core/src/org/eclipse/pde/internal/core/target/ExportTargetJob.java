/*******************************************************************************
 * Copyright (c) 2010, 2011 EclipseSource Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.net.URI;
import java.util.*;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.internal.repository.tools.Repo2Runnable;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;

/**
 * This job exports the bundles and features that make up your target.
 */
public class ExportTargetJob extends Job {

	private URI fDestination;
	private boolean fclearDestinationDirectory = true;
	private IFileStore featureDir;
	private IFileStore pluginDir;
	private IFileSystem fileSystem;
	private Map filter;
	private ITargetDefinition fTarget;

	public ExportTargetJob(ITargetDefinition target, URI destination, boolean clearDestinationDirectory) {
		super("Export Current Target Definition Job"); //$NON-NLS-1$
		fTarget = target;
		fDestination = destination;
		fclearDestinationDirectory = clearDestinationDirectory;
	}

	protected IStatus run(IProgressMonitor monitor) {
		try {
			constructFilter(fTarget);
			ITargetLocation[] containers = fTarget.getTargetLocations();
			int totalWork = containers.length;
			monitor.beginTask(PDECoreMessages.ExportTargetDefinition_task, totalWork);

			monitor.subTask(PDECoreMessages.ExportTargetJob_ConfiguringDestination);
			setupDestination(monitor);

			monitor.subTask(PDECoreMessages.ExportTargetJob_ExportingTargetContents);
			for (int i = 0; i < containers.length; i++) {
				ITargetLocation container = containers[i];
				container.resolve(fTarget, monitor);
				if (!(container instanceof IUBundleContainer))
					exportContainer(container, fTarget, featureDir, pluginDir, fileSystem, monitor);
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
		if (included == null)
			return;
		filter = new HashMap();
		for (int i = 0; i < included.length; i++) {
			NameVersionDescriptor inclusion = included[i];
			NameVersionDescriptor[] versions = (NameVersionDescriptor[]) filter.get(inclusion.getId());
			if (versions == null)
				filter.put(inclusion.getId(), new NameVersionDescriptor[] {inclusion});
			else {
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
		featureDir = destination.getChild("features"); //$NON-NLS-1$
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
		if (filter == null || descriptor.getType().equals(NameVersionDescriptor.TYPE_FEATURE))
			return true;
		NameVersionDescriptor[] versions = (NameVersionDescriptor[]) filter.get(descriptor.getId());
		if (versions == null)
			return false;
		for (int i = 0; i < versions.length; i++) {
			String version = versions[i].getVersion();
			if ((version == null || version.equals(descriptor.getVersion())) && descriptor.getType().equals(versions[i].getType()))
				return true;
		}
		return false;
	}

	private boolean shouldExport(TargetFeature feature) {
		if (filter == null)
			return true;
		NameVersionDescriptor descriptor = new NameVersionDescriptor(feature.getId(), feature.getVersion(), NameVersionDescriptor.TYPE_FEATURE);
		return shouldExport(descriptor);
	}

	private boolean shouldExport(TargetBundle bundle) {
		if (filter == null)
			return true;
		NameVersionDescriptor descriptor = new NameVersionDescriptor(bundle.getBundleInfo().getSymbolicName(), bundle.getBundleInfo().getVersion(), NameVersionDescriptor.TYPE_PLUGIN);
		return shouldExport(descriptor);
	}

	private boolean shouldExport(IInstallableUnit iu) {
		if (filter == null)
			return true;
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
		for (Iterator i = iu.getProvidedCapabilities().iterator(); i.hasNext();) {
			IProvidedCapability capability = (IProvidedCapability) i.next();
			if (capability.getNamespace().equals(namespace))
				return capability.getName();
		}
		return null;
	}

	private void exportContainer(ITargetLocation container, ITargetDefinition target, IFileStore featureDir, IFileStore pluginDir, IFileSystem fileSystem, IProgressMonitor monitor) throws CoreException {
		TargetFeature[] features = container.getFeatures();
		if (features != null) {
			monitor.subTask(PDECoreMessages.ExportTargetExportFeatures);
			for (int i = 0; i < features.length; i++) {
				if (shouldExport(features[i]))
					copy(features[i].getLocation(), featureDir, fileSystem, monitor);
			}
		}

		TargetBundle[] bundles = container.getBundles();
		if (bundles != null) {
			monitor.subTask(PDECoreMessages.ExportTargetExportPlugins);
			for (int i = 0; i < bundles.length; i++) {
				if (shouldExport(bundles[i]))
					copy(bundles[i].getBundleInfo().getLocation().getPath(), pluginDir, fileSystem, monitor);
			}
		}
	}

	private IStatus copy(String src, IFileStore destinationParent, IFileSystem fileSystem, IProgressMonitor monitor) throws CoreException {
		Path srcPath = new Path(src);
		IFileStore source = fileSystem.getStore(srcPath);
		String elementName = srcPath.segment(srcPath.segmentCount() - 1);
		IFileStore destination = destinationParent.getChild(elementName);
		if (destination.fetchInfo().exists()) {
			monitor.worked(1);
			return Status.OK_STATUS;
		}
		if (source.fetchInfo().isDirectory()) {
			destination.mkdir(EFS.NONE, new NullProgressMonitor());
		}
		source.copy(destination, EFS.OVERWRITE, new SubProgressMonitor(monitor, 1));
		return Status.OK_STATUS;
	}

	private RepositoryDescriptor createRepoDescriptor(URI location, String name, String kind) {
		RepositoryDescriptor result = new RepositoryDescriptor();
		result.setLocation(location);
		result.setKind(kind);
		result.setName(name);
		if (fclearDestinationDirectory)
			result.setAppend(false);
		return result;
	}

	private void exportProfile(ITargetDefinition target, URI destination, IProgressMonitor monitor) throws CoreException {
		Repo2Runnable exporter = new Repo2Runnable();
		exporter.addDestination(createRepoDescriptor(destination, P2TargetUtils.getProfileId(target), RepositoryDescriptor.KIND_METADATA));
		exporter.addDestination(createRepoDescriptor(destination, P2TargetUtils.getProfileId(target), RepositoryDescriptor.KIND_ARTIFACT));
		exporter.addSource(createRepoDescriptor(P2TargetUtils.getBundlePool().getLocation(), null, RepositoryDescriptor.KIND_ARTIFACT));

		IQueryResult ius = P2TargetUtils.getIUs(target, monitor);
		ArrayList toExport = new ArrayList();
		for (Iterator i = ius.iterator(); i.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) i.next();
			if (shouldExport(iu))
				toExport.add(iu);
		}
		exporter.setSourceIUs(toExport);
		exporter.run(monitor);
	}
}
