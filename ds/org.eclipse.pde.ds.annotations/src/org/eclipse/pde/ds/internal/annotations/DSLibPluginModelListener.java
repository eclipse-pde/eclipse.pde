/*******************************************************************************
 * Copyright (c) 2019 Ecliptical Software Inc. and others.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class DSLibPluginModelListener implements IPluginModelListener {

	private static DSLibPluginModelListener INSTANCE;

	private final HashSet<IJavaProject> projects = new HashSet<>();

	private DSLibPluginModelListener() {
		PluginModelManager.getInstance().addPluginModelListener(this);
	}

	private synchronized static DSLibPluginModelListener getInstance(boolean create) {
		if (create && INSTANCE == null) {
			INSTANCE = new DSLibPluginModelListener();
		}

		return INSTANCE;
	}

	public static void addProject(IJavaProject project) {
		DSLibPluginModelListener instance = getInstance(true);
		synchronized (instance.projects) {
			instance.projects.add(project);
		}
	}

	public static void removeProject(IJavaProject project) {
		DSLibPluginModelListener instance = getInstance(false);
		if (instance != null) {
			synchronized (instance.projects) {
				instance.projects.remove(project);
			}
		}
	}

	private boolean containsModel(ModelEntry[] entries) {
		return Arrays.stream(entries).anyMatch(entry -> Activator.PLUGIN_ID.equals(entry.getId()));
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		if (((delta.getKind() & PluginModelDelta.ADDED) != 0 && containsModel(delta.getAddedEntries()))
				|| ((delta.getKind() & PluginModelDelta.CHANGED) != 0 && containsModel(delta.getChangedEntries()))
				|| ((delta.getKind() & PluginModelDelta.REMOVED) != 0 && containsModel(delta.getRemovedEntries()))) {
			ArrayList<IJavaProject> toUpdate;
			synchronized (projects) {
				toUpdate = new ArrayList<>(projects);
			}

			if (!toUpdate.isEmpty()) {
				requestClasspathUpdate(toUpdate);
			}
		}
	}

	private void requestClasspathUpdate(final Collection<IJavaProject> changedProjects) {
		WorkspaceJob job = new WorkspaceJob(Messages.ProjectClasspathPreferenceChangeListener_jobName) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				SubMonitor progress = SubMonitor.convert(monitor, Messages.DSAnnotationPreferenceListener_taskName,
						changedProjects.size());
				for (IJavaProject project : changedProjects) {
					ProjectClasspathPreferenceChangeListener.updateClasspathContainer(project, progress.newChild(1));
				}

				return Status.OK_STATUS;
			};
		};
		
		job.setSystem(true);

		ISchedulingRule[] rules = changedProjects.stream().map(IJavaProject::getProject)
				.toArray(size -> new ISchedulingRule[size]);
		job.setRule(new MultiRule(rules));

		Display display = Display.getCurrent();
		if (display != null) {
			PlatformUI.getWorkbench().getProgressService().showInDialog(display.getActiveShell(), job);
		}

		job.schedule();
	}

	public static void dispose() {
		DSLibPluginModelListener instance = getInstance(false);
		if (instance != null) {
			PluginModelManager.getInstance().removePluginModelListener(instance);
		}
	}
}
