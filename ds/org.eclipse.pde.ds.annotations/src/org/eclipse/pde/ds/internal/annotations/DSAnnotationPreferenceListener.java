/*******************************************************************************
 * Copyright (c) 2012, 2016 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.ui.PlatformUI;

public class DSAnnotationPreferenceListener implements IPreferenceChangeListener {

	public DSAnnotationPreferenceListener() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(this);
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		if (!ws.isAutoBuilding() && !Activator.PREF_CLASSPATH.equals(event.getKey()))
			return;

		WorkspaceJob job = new WorkspaceJob(Messages.DSAnnotationPreferenceListener_jobName) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IProject[] projects = ws.getRoot().getProjects();
				ArrayList<IProject> managedProjects = new ArrayList<>(projects.length);

				for (IProject project : projects) {
					if (project.isOpen() && DSAnnotationCompilationParticipant.isManaged(project))
						managedProjects.add(project);
				}

				SubMonitor progress = SubMonitor.convert(monitor, Messages.DSAnnotationPreferenceListener_taskName, managedProjects.size());
				for (IProject project : managedProjects) {
					if (Activator.PREF_CLASSPATH.equals(event.getKey())
							|| Activator.PREF_ENABLED.equals(event.getKey()))
						ProjectClasspathPreferenceChangeListener.updateClasspathContainer(JavaCore.create(project), progress.newChild(1));

					project.build(IncrementalProjectBuilder.FULL_BUILD, progress.newChild(1));
				}

				return Status.OK_STATUS;
			};
		};

		PlatformUI.getWorkbench().getProgressService().showInDialog(null, job);
		job.schedule();
	}

	public void dispose() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).removePreferenceChangeListener(this);
	}
}
