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
package org.eclipse.pde.internal.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RequiredPluginsInitializer extends ClasspathContainerInitializer {

	private static final Job initPDEJob = Job.create(PDECoreMessages.PluginModelManager_InitializingPluginModels,
			monitor -> {
				if (!PDECore.getDefault().getModelManager().isInitialized()) {
					PDECore.getDefault().getModelManager().targetReloaded(monitor);
				}
			});

	private static final DeferredClasspathContainerInitializerJob deferredClasspathContainerInitializerJob = new DeferredClasspathContainerInitializerJob();

	private static class DeferredClasspathContainerInitializerJob extends Job {

		private final Set<IJavaProject> projects = new LinkedHashSet<>();

		public DeferredClasspathContainerInitializerJob() {
			// This name is not displayed to a user.
			super("DeferredClasspathContainerInitializerJob"); //$NON-NLS-1$
			setSystem(true);
		}

		public synchronized void initialize(IJavaProject project) {
			if (projects.add(project)) {
				schedule();
			}
		}

		private synchronized IJavaProject[] consumeProjects() {
			try {
				return projects.toArray(IJavaProject[]::new);
			} finally {
				projects.clear();
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				setupClasspath(consumeProjects());
			} catch (JavaModelException e) {
				PDECore.log(e);
			}
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == PluginModelManager.class;
		}
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		if (Job.getJobManager().isSuspended()) {
			// if the jobmanager is currently suspended we can't use the
			// schedule/join pattern here, instead we must retry the requested
			// action once jobs are enabled again, this will the possibly
			// trigger a rebuild if required or notify other listeners.
			deferredClasspathContainerInitializerJob.initialize(javaProject);
		} else {
			setupClasspath(javaProject);
		}
	}

	protected static void setupClasspath(IJavaProject... javaProjects) throws JavaModelException {
		// The first project to be built may initialize the PDE models,
		// potentially long running, so allow cancellation
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		if (!manager.isInitialized()) {
			initPDEJob.schedule();
			try {
				initPDEJob.join();
			} catch (InterruptedException e) {
			}
		}

		Map<IJavaProject, RequiredPluginsClasspathContainer> classPathContainers = new LinkedHashMap<>();
		for (IJavaProject javaProject : javaProjects) {
			IProject project = javaProject.getProject();
			if (project.exists() && project.isOpen()) {
				IPluginModelBase model = manager.findModel(project);
				RequiredPluginsClasspathContainer requiredPluginsClasspathContainer = new RequiredPluginsClasspathContainer(
						model, project);
				classPathContainers.put(javaProject, requiredPluginsClasspathContainer);
			}
		}

		JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH,
				classPathContainers.keySet().toArray(IJavaProject[]::new),
				classPathContainers.values().toArray(IClasspathContainer[]::new), null);
	}

	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		if (containerPath == null || project == null) {
			return null;
		}

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
