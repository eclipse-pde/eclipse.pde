/*******************************************************************************
 * Copyright (c) 2015, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.swt.widgets.Display;
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
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (requiresClasspathUpdate(event)) {
			requestClasspathUpdate();
		}
	}

	private void requestClasspathUpdate() {
		WorkspaceJob job = new WorkspaceJob(Messages.ProjectClasspathPreferenceChangeListener_jobName) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				updateClasspathContainer(project, monitor);
				return Status.OK_STATUS;
			};
		};
		job.setSystem(true);
		job.setRule(project.getProject());

		Display display = Display.getCurrent();
		if (display != null) {
			PlatformUI.getWorkbench().getProgressService().showInDialog(display.getActiveShell(), job);
		}

		job.schedule();
	}

	static boolean requiresClasspathUpdate(PreferenceChangeEvent event) {
		return Activator.PREF_CLASSPATH.equals(event.getKey()) || Activator.PREF_SPEC_VERSION.equals(event.getKey())
				|| Activator.PREF_ENABLED.equals(event.getKey());
	}

	static void updateClasspathContainer(IJavaProject project, IProgressMonitor monitor) {
		if (monitor != null)
			monitor.beginTask(project.getElementName(), 1);

		try {
			if (monitor != null && monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

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

			if (monitor != null) {
				monitor.worked(1);
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
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
