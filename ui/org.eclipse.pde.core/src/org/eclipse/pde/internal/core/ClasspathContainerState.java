/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - extracted code from ClasspathComputer
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.util.PDEClasspathContainerSaveHelper;

public class ClasspathContainerState {

	/**
	 * Job used to update class path containers.
	 */
	private static final UpdateClasspathsJob fUpdateJob = new UpdateClasspathsJob();

	static final IResourceChangeListener CHANGE_LISTENER = new IResourceChangeListener() {

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResource resource = event.getResource();
			if (resource instanceof IProject project) {
				if (PDECore.DEBUG_STATE) {
					PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
							String.format("Project %s was deleted.", project.getName())); //$NON-NLS-1$
				}
				getStateFile(project).delete();
			}
		}
	};

	/**
	 * Job to update class path containers asynchronously. Avoids blocking the
	 * UI thread. The job is given a workspace lock so other jobs can't run on a
	 * stale classpath.
	 */
	private static final class UpdateClasspathsJob extends Job {

		private static final int WORK = 10_000;
		private final Queue<UpdateRequest> workQueue = new ConcurrentLinkedQueue<>();

		/**
		 * Constructs a new job.
		 */
		public UpdateClasspathsJob() {
			super(PDECoreMessages.PluginModelManager_1);
			// The job is given a workspace lock so other jobs can't run on a
			// stale classpath (bug 354993)
			setRule(ResourcesPlugin.getWorkspace().getRoot());
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == PluginModelManager.class || family == ClasspathComputer.class;
		}

		@Override
		protected IStatus run(IProgressMonitor jobMonitor) {
			SubMonitor monitor = SubMonitor.convert(jobMonitor, PDECoreMessages.PluginModelManager_1, WORK);
			PluginModelManager.getInstance().initialize(monitor.split(10));
			PluginModelManager modelManager = PluginModelManager.getInstance();
			Map<IJavaProject, IClasspathContainer> updateProjects = new LinkedHashMap<>();
			Map<IProject, IStatus> errorsPerProject = new LinkedHashMap<>();
			UpdateRequest request;
			while (!monitor.isCanceled() && (request = workQueue.poll()) != null) {
				monitor.setWorkRemaining(WORK);
				IProject project = request.project();
				if (project.exists() && project.isOpen()) {
					IPluginModelBase model = modelManager.findModel(project);
					if (model != null && PluginProject.isJavaProject(project)) {
						IJavaProject javaProject = JavaCore.create(project);
						RequiredPluginsClasspathContainer classpathContainer = new RequiredPluginsClasspathContainer(
								model, project);
						try {
							if (!isUpToDate(project, classpathContainer.computeEntries(), request.container())) {
								updateProjects.put(javaProject, classpathContainer);
								errorsPerProject.remove(project);
								saveState(project, classpathContainer);
							}
						} catch (CoreException e) {
							errorsPerProject.put(project, e.getStatus());
						}
						monitor.worked(1);
					}
				}
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (!updateProjects.isEmpty()) {
				int i = 0;
				int n = updateProjects.size();
				IJavaProject[] javaProjects = new IJavaProject[n];
				IClasspathContainer[] container = new IClasspathContainer[n];
				for (Entry<IJavaProject, IClasspathContainer> entry : updateProjects.entrySet()) {
					javaProjects[i] = entry.getKey();
					container[i] = entry.getValue();
					i++;
				}
				try {
					setProjectContainers(javaProjects, container, monitor);
				} catch (JavaModelException e) {
					return e.getStatus();
				}
			}
			IStatus[] errors = errorsPerProject.values().toArray(IStatus[]::new);
			if (errors.length == 0) {
				return Status.OK_STATUS;
			}
			if (errors.length == 1) {
				return errors[0];
			}
			MultiStatus overallStatus = new MultiStatus(ClasspathComputer.class, 0,
					PDECoreMessages.ClasspathComputer_failed);
			for (IStatus status : errors) {
				overallStatus.add(status);
			}
			return overallStatus;
		}

		/**
		 * Queues more projects/containers.
		 */
		void addAll(Collection<IProject> tocheck) {
			for (IProject project : tocheck) {
				workQueue.add(new UpdateRequest(project, null));
			}
			schedule();
		}

		void add(IProject project, IClasspathContainer classpathContainer) {
			if (project == null) {
				return;
			}
			workQueue.add(new UpdateRequest(project, classpathContainer));
			schedule();
		}

	}

	private static boolean isUpToDate(IProject project, IClasspathEntry[] currentEntries,
			IClasspathContainer previousClasspathContainer) {
		if (previousClasspathContainer == null) {
			if (PDECore.DEBUG_STATE) {
				PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
						String.format("%s need update because it has no state to compare", project.getName())); //$NON-NLS-1$
			}
			return false;
		}
		IClasspathEntry[] previousEntries = previousClasspathContainer.getClasspathEntries();
		if (previousEntries == null || previousEntries.length != currentEntries.length) {
			if (PDECore.DEBUG_STATE) {
				PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
						String.format("%s need update because entries do not match in size!", //$NON-NLS-1$
								project.getName()));
			}
			return false;
		}
		for (int i = 0; i < previousEntries.length; i++) {
			IClasspathEntry previous = previousEntries[i];
			IClasspathEntry current = currentEntries[i];
			if (!Objects.equals(current, previous)) {
				if (PDECore.DEBUG_STATE) {
					PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
							String.format("%s need update because entry at position %d is different:\n\t%s\n\t%s", //$NON-NLS-1$
									project.getName(), i, current, previous));
				}
				return false;
			}
		}
		return true;
	}

	private static void saveState(IProject project, RequiredPluginsClasspathContainer classpathContainer) {
		synchronized (project) {
			try {
				File stateFile = getStateFile(project);
				stateFile.getParentFile().mkdirs();
				try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(stateFile))) {
					PDEClasspathContainerSaveHelper.writeContainer(classpathContainer, stream);
				}
			} catch (Exception e) {
				// can't write then...
				if (PDECore.DEBUG_STATE) {
					PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
							String.format("Writing project state for %s failed!", project.getName()), //$NON-NLS-1$
							e);
				}
			}
		}
	}

	static IClasspathContainer readState(IProject project) {
		synchronized (project) {
			try {
				File stateFile = getStateFile(project);
				try (InputStream stream = new FileInputStream(stateFile)) {
					IClasspathContainer container = Objects
							.requireNonNull(PDEClasspathContainerSaveHelper.readContainer(stream));
					if (PDECore.DEBUG_STATE) {
						PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
								String.format("%s is restored from previous state.", project.getName())); //$NON-NLS-1$
					}
					return container;
				}
			} catch (Exception e) {
				if (PDECore.DEBUG_STATE) {
					if (e instanceof FileNotFoundException) {
						PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
								String.format("%s has no saved state!", project.getName())); //$NON-NLS-1$
					} else {
						PDECore.TRACE.trace(PDECore.KEY_DEBUG_STATE,
								String.format("Restoring project state for %s failed!", project.getName()), e); //$NON-NLS-1$
					}
				}
				return PDEClasspathContainerSaveHelper.emptyContainer();
			}
		}
	}

	static void setProjectContainers(IJavaProject[] javaProjects, IClasspathContainer[] container,
			IProgressMonitor monitor) throws JavaModelException {
		JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, javaProjects, container, monitor);
	}

	private static File getStateFile(IProject project) {
		return PDECore.getDefault().getStateLocation().append("cpc").append(project.getName()) //$NON-NLS-1$
				.toFile();
	}

	public static void requestClasspathUpdate(IProject project) {
		if (project == null) {
			return;
		}
		requestClasspathUpdate(List.of(project));
	}

	public static void requestClasspathUpdate(Collection<IProject> updateProjects) {
		if (updateProjects == null || updateProjects.isEmpty()) {
			return;
		}
		fUpdateJob.addAll(updateProjects);
	}

	static void requestClasspathUpdate(IProject project, IClasspathContainer savedState) {
		fUpdateJob.add(project, savedState);
	}

	private static record UpdateRequest(IProject project, IClasspathContainer container) {

	}
}
