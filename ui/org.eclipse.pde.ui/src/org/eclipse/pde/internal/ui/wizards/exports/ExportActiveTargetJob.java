/*******************************************************************************
 * Copyright (c) 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial API and implementation
 *     Ian Bull <irbull@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.net.URI;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * This job exports the bundles and features that make up your target. 
 */
public class ExportActiveTargetJob extends Job {

	private URI fDestination;
	private boolean fclearDestinationDirectory = false;

	public ExportActiveTargetJob(URI destination, boolean clearDestinationDirectory) {
		super("Export Current Target Definition Job"); //$NON-NLS-1$
		fDestination = destination;
		fclearDestinationDirectory = clearDestinationDirectory;
	}

	protected IStatus run(IProgressMonitor monitor) {

		IFileSystem fileSystem = EFS.getLocalFileSystem();
		if (!fileSystem.canWrite()) {
			return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Destination directory not writable."); //$NON-NLS-1$ 
		}
		IFileStore destination = fileSystem.getStore(fDestination);

		FeatureModelManager featureManager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] featureModels = featureManager.getModels();
		IPluginModelBase[] pluginModels = PluginRegistry.getExternalModels();

		IFileStore featureDir = destination.getChild("features"); //$NON-NLS-1$
		IFileStore pluginDir = destination.getChild("plugins"); //$NON-NLS-1$
		IFileStore metadataXML = destination.getChild("content.xml"); //$NON-NLS-1$
		IFileStore metadataJAR = destination.getChild("content.jar"); //$NON-NLS-1$

		int totalWork = featureModels.length + pluginModels.length;

		try {
			monitor.beginTask(PDEUIMessages.ExportTargetDefinition_task, totalWork);
			if (fclearDestinationDirectory) {
				monitor.subTask(PDEUIMessages.ExportTargetDeleteOldData); //Deleting old data...
			}
			IProvisioningAgent agent = (IProvisioningAgent) PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
			ExportTargetMetadata component = null;

			if (agent != null) {
				component = new ExportTargetMetadata(agent);
			}
			try {
				if (fclearDestinationDirectory) {
					if (component != null) {
						// If p2 is available, clear the existing repositories
						component.clearExporedRepository(fDestination);
					}
					if (featureDir.fetchInfo().exists()) {
						featureDir.delete(EFS.NONE, new NullProgressMonitor());
					}
					if (pluginDir.fetchInfo().exists()) {
						pluginDir.delete(EFS.NONE, new NullProgressMonitor());
					}
					if (metadataJAR.fetchInfo().exists()) {
						metadataJAR.delete(EFS.NONE, new NullProgressMonitor());
					}
					if (metadataXML.fetchInfo().exists()) {
						metadataXML.delete(EFS.NONE, new NullProgressMonitor());
					}
				}

				if (!featureDir.fetchInfo().exists()) {
					featureDir.mkdir(EFS.NONE, new NullProgressMonitor());
				}
				if (!pluginDir.fetchInfo().exists()) {
					pluginDir.mkdir(EFS.NONE, new NullProgressMonitor());
				}
			} catch (CoreException e1) {
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Failed to create destination directory.", e1); //$NON-NLS-1$
			}

			monitor.subTask(PDEUIMessages.ExportTargetExportFeatures);
			for (int i = 0; i < featureModels.length; i++) {
				IFeatureModel model = featureModels[i];
				if (model.isEnabled() && model instanceof ExternalFeatureModel) {
					copy(model.getInstallLocation(), featureDir, fileSystem, monitor);
				}
			}

			monitor.subTask(PDEUIMessages.ExportTargetExportPlugins);
			for (int i = 0; i < pluginModels.length; i++) {
				IPluginModelBase model = pluginModels[i];
				//if (model.isEnabled()) {
				copy(model.getInstallLocation(), pluginDir, fileSystem, monitor);
				//}
			}

			try {
				if (component != null) {
					// If p2 is available, export the metadata
					TargetDefinition definition = ((TargetDefinition) TargetPlatformService.getDefault().getWorkspaceTargetHandle().getTargetDefinition());
					IProfile profile = definition.getProfile();
					IStatus status = component.exportMetadata(profile, fDestination, definition.getName());
					if (status.isOK()) {
						return status;
					}
					return Status.OK_STATUS;
				}
			} catch (CoreException e) {
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Failed to export the target", e); //$NON-NLS-1$ 
			}
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Failed to export the target", e); //$NON-NLS-1$ 
		}

		finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private IStatus copy(String src, IFileStore destinationParent, IFileSystem fileSystem, IProgressMonitor monitor) throws CoreException {
		Path srcPath = new Path(src);
		IFileStore source = fileSystem.getStore(srcPath);
		String elementName = srcPath.segment(srcPath.segmentCount() - 1);
		IFileStore destinationDirectory = destinationParent.getChild(elementName);
		if (destinationDirectory.fetchInfo().exists()) {
			monitor.worked(1);
			return Status.OK_STATUS;
		}
		if (source.fetchInfo().isDirectory()) {
			destinationDirectory.mkdir(EFS.NONE, new NullProgressMonitor());
		}
		source.copy(destinationDirectory, EFS.OVERWRITE, new SubProgressMonitor(monitor, 1));
		return Status.OK_STATUS;
	}

}
