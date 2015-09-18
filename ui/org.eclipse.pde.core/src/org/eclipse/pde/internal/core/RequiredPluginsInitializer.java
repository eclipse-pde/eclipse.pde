/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RequiredPluginsInitializer extends ClasspathContainerInitializer {

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		IProject project = javaProject.getProject();
		// The first project to be built may initialize the PDE models, potentially long running, so allow cancellation
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		if (!manager.isInitialized()) {
			Job initPDEJob = new Job(PDECoreMessages.PluginModelManager_InitializingPluginModels) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (!PDECore.getDefault().getModelManager().isInitialized()) {
						PDECore.getDefault().getModelManager().targetReloaded(monitor);
					}
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					return Status.OK_STATUS;
				}
			};
			initPDEJob.schedule();
			try {
				initPDEJob.join();
			} catch (InterruptedException e) {
			}
		}
		if (project.exists() && project.isOpen()) {
			IPluginModelBase model = manager.findModel(project);
			JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, new IJavaProject[] {javaProject}, new IClasspathContainer[] {new RequiredPluginsClasspathContainer(model)}, null);
		}
	}

	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		if (containerPath == null || project == null)
			return null;

		return containerPath.segment(0) + "/" + project.getPath().segment(0); //$NON-NLS-1$
	}

	@Override
	public String getDescription(IPath containerPath, IJavaProject project) {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description;
	}

	@Override
	public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
		// Allow custom source attachments for PDE classpath containers (Bug 338182)
		return Status.OK_STATUS;
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		// The only supported update is to modify the source attachment
		return true;
	}

	@Override
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
		// The only supported update is to modify the source attachment
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project}, new IClasspathContainer[] {containerSuggestion}, null);
	}

}
