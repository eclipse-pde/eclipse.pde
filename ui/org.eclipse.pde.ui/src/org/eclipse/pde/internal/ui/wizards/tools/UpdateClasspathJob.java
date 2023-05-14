/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class UpdateClasspathJob extends Job {
	IPluginModelBase[] fModels;

	public UpdateClasspathJob(IPluginModelBase[] models) {
		super(PDEUIMessages.UpdateClasspathJob_title);
		setPriority(Job.LONG);
		fModels = models;
	}

	public boolean doUpdateClasspath(IProgressMonitor monitor, IPluginModelBase[] models) throws CoreException {
		monitor.beginTask(PDEUIMessages.UpdateClasspathJob_task, models.length);
		try {
			for (IPluginModelBase model : models) {
				monitor.subTask(model.getPluginBase().getId());
				// no reason to compile classpath for a non-Java model
				IProject project = model.getUnderlyingResource().getProject();
				if (!project.hasNature(JavaCore.NATURE_ID)) {
					monitor.worked(1);
					continue;
				}
				IProjectDescription projDesc = project.getDescription();
				if (projDesc == null)
					continue;
				projDesc.setReferencedProjects(new IProject[0]);
				project.setDescription(projDesc, null);
				IFile file = project.getFile(".project"); //$NON-NLS-1$
				if (file.exists())
					file.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
				ClasspathComputer.setClasspath(project, model);
				monitor.worked(1);
				if (monitor.isCanceled())
					return false;
			}
		} finally {
			monitor.done();
		}
		return true;
	}

	class UpdateClasspathWorkspaceRunnable implements IWorkspaceRunnable {
		boolean fCanceled = false;

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			fCanceled = doUpdateClasspath(monitor, fModels);
		}

		public boolean isCanceled() {
			return fCanceled;
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			UpdateClasspathWorkspaceRunnable runnable = new UpdateClasspathWorkspaceRunnable();
			PDEPlugin.getWorkspace().run(runnable, monitor);
			if (runnable.isCanceled()) {
				return new Status(IStatus.CANCEL, IPDEUIConstants.PLUGIN_ID, IStatus.CANCEL, "", null); //$NON-NLS-1$
			}

		} catch (CoreException e) {
			String title = PDEUIMessages.UpdateClasspathJob_error_title;
			String message = PDEUIMessages.UpdateClasspathJob_error_message;
			PDEPlugin.logException(e, title, message);
			return Status.error(message, e);
		}
		return Status.OK_STATUS;
	}

}
