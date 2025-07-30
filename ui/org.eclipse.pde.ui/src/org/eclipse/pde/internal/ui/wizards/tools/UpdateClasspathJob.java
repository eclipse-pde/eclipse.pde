/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Simplify UpdateClasspathJob and leverage the implicit cancellation checks and progress reporting of SubMonitor.split()
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ClasspathContainerState;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class UpdateClasspathJob {
	private UpdateClasspathJob() {
	}

	public static Job scheduleFor(List<IPluginModelBase> models, boolean user) {
		WorkspaceJob job = new WorkspaceJob(PDEUIMessages.UpdateClasspathJob_title) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				List<IProject> toUpdate = new ArrayList<>();
				SubMonitor mon = SubMonitor.convert(monitor, PDEUIMessages.UpdateClasspathJob_task, models.size());
				for (IPluginModelBase model : models) {
					IProject project = updateClasspath(model, mon.split(1));
					if (project != null) {
						toUpdate.add(project);
					}
				}
				ClasspathContainerState.requestClasspathUpdate(toUpdate);
				return Status.OK_STATUS;
			}
		};
		job.setUser(user);
		job.setPriority(Job.LONG);
		job.schedule();
		return job;
	}

	private static IProject updateClasspath(IPluginModelBase model, SubMonitor monitor) throws CoreException {
		try {
			monitor.subTask(model.getPluginBase().getId());
			// no reason to compile classpath for a non-Java model
			IProject project = model.getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IProjectDescription projDesc = project.getDescription();
				if (projDesc != null) {
					projDesc.setReferencedProjects(new IProject[0]);
					project.setDescription(projDesc, null);
					IFile file = project.getFile(".project"); //$NON-NLS-1$
					if (file.exists()) {
						file.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
					}
					ClasspathComputer.setClasspath(project, model);
					return project;
				}
			}
		} catch (CoreException e) {
			String message = PDEUIMessages.UpdateClasspathJob_error_message;
			PDEPlugin.logException(e, PDEUIMessages.UpdateClasspathJob_error_title, message);
			throw new CoreException(Status.error(message, e));
		}
		return null;
	}

}
