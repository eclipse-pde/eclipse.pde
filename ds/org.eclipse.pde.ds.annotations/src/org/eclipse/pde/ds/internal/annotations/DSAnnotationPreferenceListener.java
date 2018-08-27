/*******************************************************************************
 * Copyright (c) 2012, 2017 Ecliptical Software Inc. and others.
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

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class DSAnnotationPreferenceListener implements IPreferenceChangeListener {

	public DSAnnotationPreferenceListener() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(this);
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		final boolean autoBuilding = ws.isAutoBuilding();
		final boolean requiresClasspathUpdate = ProjectClasspathPreferenceChangeListener.requiresClasspathUpdate(event);
		if (!autoBuilding && !requiresClasspathUpdate) {
			return;
		}

		WorkspaceJob job = new WorkspaceJob(Messages.DSAnnotationPreferenceListener_jobName) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IProject[] projects = ws.getRoot().getProjects();
				ArrayList<IProject> managedProjects = new ArrayList<>(projects.length);

				for (IProject project : projects) {
					if (project.isOpen() && DSAnnotationCompilationParticipant.isManaged(project)) {
						managedProjects.add(project);
					}
				}

				SubMonitor progress = SubMonitor.convert(monitor, Messages.DSAnnotationPreferenceListener_taskName,
						managedProjects.size() * 2);
				for (IProject project : managedProjects) {
					if (requiresClasspathUpdate) {
						ProjectClasspathPreferenceChangeListener.updateClasspathContainer(JavaCore.create(project), progress.newChild(1));
					} else {
						progress.worked(1);
					}

					if (autoBuilding) {
						project.build(IncrementalProjectBuilder.FULL_BUILD, progress.newChild(1));
					} else {
						progress.worked(1);
					}
				}

				return Status.OK_STATUS;
			};

			@Override
			public boolean belongsTo(Object family) {
				return autoBuilding && ResourcesPlugin.FAMILY_AUTO_BUILD.equals(family);
			}
		};

		Display display = Display.getCurrent();
		if (display != null) {
			PlatformUI.getWorkbench().getProgressService().showInDialog(display.getActiveShell(), job);
		}

		job.schedule();
	}

	public void dispose() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).removePreferenceChangeListener(this);
	}
}
