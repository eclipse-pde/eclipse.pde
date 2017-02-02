/*******************************************************************************
 * Copyright (c) 2015 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class ProjectClasspathPreferenceChangeListener implements IPreferenceChangeListener, IResourceChangeListener {

	private final IJavaProject project;

	private final ProjectScope scope;

	public ProjectClasspathPreferenceChangeListener(IJavaProject project) {
		this.project = project;
		scope = new ProjectScope(project.getProject());
		scope.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
		requestClasspathUpdate();
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (Activator.PREF_CLASSPATH.equals(event.getKey()) || Activator.PREF_ENABLED.equals(event.getKey())) {
			requestClasspathUpdate();
		}
	}

	private void requestClasspathUpdate() {
		WorkspaceJob job = new WorkspaceJob(Messages.ProjectClasspathPreferenceChangeListener_jobName) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				updateClasspathContainer(project, monitor);
				return Status.OK_STATUS;
			};
		};

		PlatformUI.getWorkbench().getProgressService().showInDialog(null, job);
		job.schedule();
	}

	static void updateClasspathContainer(IJavaProject project, IProgressMonitor monitor) {
		if (monitor != null)
			monitor.beginTask(project.getElementName(), 1);

		try {
			if (monitor != null && monitor.isCanceled())
				throw new OperationCanceledException();

			ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH.segment(0));
			if (initializer != null && initializer.canUpdateClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, project)) {
				IPluginModelBase model = PluginRegistry.findModel(project.getProject());
				if (model != null) {
					try {
						initializer.requestClasspathContainerUpdate(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, project, new RequiredPluginsClasspathContainer(model, ClasspathUtilCore.getBuild(model)));
					} catch (CoreException e) {
						Activator.log(e);
					}
				}
			}

			if (monitor != null)
				monitor.worked(1);
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if ((event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE)
				&& project.getProject().equals(event.getResource())) {
			Activator.getDefault().disposeProjectClasspathPreferenceChangeListener(project);
		}
	}

	public void dispose() {
		scope.getNode(Activator.PLUGIN_ID).removePreferenceChangeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
