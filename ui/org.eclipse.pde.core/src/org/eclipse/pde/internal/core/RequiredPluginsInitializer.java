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

import java.lang.StackWalker.Option;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RequiredPluginsInitializer extends ClasspathContainerInitializer {

	private static final AtomicBoolean WARNING_LOGGED = new AtomicBoolean();

	private static final Map<IJavaProject, Job> JOB_MAP = new ConcurrentHashMap<>();

	private static final Job initPDEJob = Job.create(PDECoreMessages.PluginModelManager_InitializingPluginModels,
			monitor -> {
				if (!PDECore.getDefault().getModelManager().isInitialized()) {
					PDECore.getDefault().getModelManager().targetReloaded(monitor);
				}
			});

	private static StackWalker walker = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	@Override
	public void initialize(IPath containerPath, IJavaProject javaProject) throws CoreException {
		if (isCalledWhileWorkbenchStartup()) {
			// See https://github.com/eclipse-pde/eclipse.pde/issues/1481
			if (WARNING_LOGGED.compareAndSet(false, true)) {
				ILog.get().warn(
						"RequiredPluginsInitializer called from within the Application startup thread this will badly impact your IDE performance!", //$NON-NLS-1$
						new RuntimeException("Called from Application startup thread thread here")); //$NON-NLS-1$
			}
			JOB_MAP.compute(javaProject, (jp, oldjob) -> {
				if (oldjob != null) {
					oldjob.cancel();
				}
				Job job = Job.create(PDECoreMessages.PluginModelManager_InitializingPluginModels, m -> {
					if (oldjob != null) {
						try {
							oldjob.join();
						} catch (InterruptedException e) {
						}
					}
					setClasspath(jp);
				});
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						JOB_MAP.remove(jp);
					}
				});
				job.schedule();
				return job;
			});
			return;
		}
		Job job = JOB_MAP.get(javaProject);
		if (job != null) {
			try {
				job.join();
			} catch (InterruptedException e) {
			}
		}
		setClasspath(javaProject);
	}

	protected void setClasspath(IJavaProject javaProject) throws JavaModelException {
		IProject project = javaProject.getProject();
		// The first project to be built may initialize the PDE models, potentially long running, so allow cancellation
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		if (!manager.isInitialized()) {
			initPDEJob.schedule();
			try {
				initPDEJob.join();
			} catch (InterruptedException e) {
			}
		}
		if (project.exists() && project.isOpen()) {
			IPluginModelBase model = manager.findModel(project);
			JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, new IJavaProject[] { javaProject },
					new IClasspathContainer[] { new RequiredPluginsClasspathContainer(model, project) }, null);
		}
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

	private static boolean isCalledWhileWorkbenchStartup() {

		return walker.walk(frames -> {
			return frames.anyMatch(sf -> {
				return "org.eclipse.e4.ui.internal.workbench.E4Workbench".equals(sf.getClassName()) //$NON-NLS-1$
						&& "createAndRunUI".equals(sf.getMethodName()); //$NON-NLS-1$
			});
		});
	}

}
