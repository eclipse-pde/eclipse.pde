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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.core.project.PDEProject;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class BndBuilder extends IncrementalProjectBuilder {

	private static final String CLASS_EXTENSION = ".class"; //$NON-NLS-1$

	// This is currently disabled as it sometimes lead to jar not generated as
	// JDT is clearing the outputfolder while the build is running, need to
	// investigate if we can avoid this and it actually has benefits to build
	// everything async.
	private static final boolean USE_JOB = false;

	private static final Predicate<IResource> CLASS_FILTER = resource -> {
		if (resource instanceof IFile) {
			return resource.getName().endsWith(CLASS_EXTENSION);
		}
		return true;
	};

	private final Map<IProject, Job> buildJobMap = new ConcurrentHashMap<>();

	public static final String BUILDER_ID = "org.eclipse.pde.BndBuilder";//$NON-NLS-1$

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		if (BndProject.isBndProject(project) && (requireBuild(project) || hasRelevantDelta(getDelta(project)))) {
			if (USE_JOB) {
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
			} else {
				buildProjectJar(project, monitor);
			}
		}
		return new IProject[] { project };
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		IFile file = getProject().getFile(BndProject.INSTRUCTIONS_FILE);
		if (file.exists()) {
			file.deleteMarkers(PDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
		}
	}

	private static final class BndBuild implements ICoreRunnable {

		private final IProject project;
		private final Job oldJob;

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
			buildProjectJar(project, monitor);
		}

	}

	private static void buildProjectJar(IProject project, IProgressMonitor monitor) {
		try {
			Optional<Project> bndProject = BndProjectManager.getBndProject(project);
			if (bndProject.isEmpty()) {
				return;
			}
			if (monitor.isCanceled()) {
				return;
			}
			try (Project bnd = bndProject.get(); ProjectBuilder builder = new ProjectBuilder(bnd) {
				@Override
				public void addClasspath(aQute.bnd.osgi.Jar jar) {
					try {
						// If the output exits, the ProjectBuilder adds the
						// output as a classpath jar, this on the other hand
						// later confuses BND because it thinks there is a
						// splitpackage and tries to copy the class into the jar
						// again...
						if (Objects.equals(jar.getSource(), bnd.getOutput())) {
							jar.close();
							return;
						}
					} catch (Exception e) {
						// can't do anything useful...
					}
					super.addClasspath(jar);
				}
			}) {
				builder.setBase(bnd.getBase());
				ProjectJar jar = new ProjectJar(project, CLASS_FILTER);
				builder.setJar(jar);
				// build the main jar
				builder.build();
				new BndErrorReporter(project, bnd, project.getFile(BndProject.INSTRUCTIONS_FILE))
						.validateContent(monitor);
				// now build sub jars
				List<Builder> subBuilders = builder.getSubBuilders();
				for (Builder subBuilder : subBuilders) {
					if (subBuilder == builder) {
						continue;
					}
					File outputFile = subBuilder.getOutputFile(null);
					if (outputFile != null) {
						Jar subJar = subBuilder.build();
						subJar.write(outputFile);
						for (IFile file : project.getWorkspace().getRoot()
								.findFilesForLocationURI(outputFile.toURI())) {
							file.refreshLocal(IResource.DEPTH_ZERO, monitor);
						}
						File propertiesFile = subBuilder.getPropertiesFile();
						if (propertiesFile != null) {
							for (IFile file : project.getWorkspace().getRoot()
									.findFilesForLocationURI(propertiesFile.toURI())) {
								new BndErrorReporter(project, subBuilder, file).validateContent(monitor);
							}
						}
					}
				}
			}
			if (monitor.isCanceled()) {
				return;
			}
		} catch (Exception e) {
			PDECore.log(e);
		}
	}

	private static boolean requireBuild(IProject project) {
		// If there is no manifest file yet, always generate one
		return !PDEProject.getManifest(project).exists();
	}

	private static boolean hasRelevantDelta(IResourceDelta delta) throws CoreException {
		if (delta != null) {
			AtomicBoolean result = new AtomicBoolean();
			delta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (resource instanceof IFile file) {
						String name = file.getName();
						if (name.endsWith(CLASS_EXTENSION) || file.getName().equals(BndProject.INSTRUCTIONS_FILE)
								|| name.equals(ICoreConstants.MANIFEST_FILENAME)) {
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
