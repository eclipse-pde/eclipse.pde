/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.natures.BndProject;

public class BndBuilder extends IncrementalProjectBuilder {

	private static final String CLASS_EXTENSION = ".class"; //$NON-NLS-1$

	private static final Predicate<IResource> classFilter = resource -> {
		if (resource instanceof IFile) {
			return resource.getName().endsWith(CLASS_EXTENSION);
		}
		return true;
	};

	private Map<IProject, Job> buildJobMap = new ConcurrentHashMap<>();

	public static final String BUILDER_ID = "org.eclipse.pde.BndBuilder";//$NON-NLS-1$

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		if (BndProject.isBndProject(project) && hasRelevantDelta(getDelta(project))) {
			Job buildJob = buildJobMap.compute(project, (p, oldJob) -> {
				Job job = Job.create(NLS.bind(PDECoreMessages.BundleBuilder_building, project.getName()),
						new BndBuild(p, oldJob));
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						buildJobMap.remove(p, job);
					}
				});
				return job;
			});
			buildJob.schedule();
		}
		return new IProject[] { project };
	}

	private static final class BndBuild implements ICoreRunnable {

		private IProject project;
		private Job oldJob;

		public BndBuild(IProject project, Job oldJob) {
			this.project = project;
			this.oldJob = oldJob;
		}

		@Override
		public void run(IProgressMonitor monitor) {

			if (oldJob != null) {
				oldJob.cancel();
				try {
					oldJob.join();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			try {
				Optional<Project> bndProject = BndProjectManager.getBndProject(project);
				if (bndProject.isEmpty()) {
					return;
				}
				if (monitor.isCanceled()) {
					return;
				}
				try (Project bnd = bndProject.get(); ProjectBuilder builder = new ProjectBuilder(bnd)) {
					builder.setBase(bnd.getBase());
					ProjectJar jar = new ProjectJar(project, classFilter);
					builder.setJar(jar);
					builder.build();
				}
				if (monitor.isCanceled()) {
					return;
				}
			} catch (Exception e) {
				PDECore.log(e);
			}
		}
	}

	private static boolean hasRelevantDelta(IResourceDelta delta) throws CoreException {
		if (delta != null) {
			AtomicBoolean result = new AtomicBoolean();
			delta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						String name = file.getName();
						if (name.endsWith(CLASS_EXTENSION) || file.getName().equals(BndProject.INSTRUCTIONS_FILE)) {
							result.set(true);
							return false;
						}
					}
					return true;
				}
			});
			return result.get();
		}
		return true;
	}
}
