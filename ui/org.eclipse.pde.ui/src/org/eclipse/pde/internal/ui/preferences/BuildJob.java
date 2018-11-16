/*******************************************************************************
 *  Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public final class BuildJob extends Job {
	private final IProject[] fProjects;
	private int fBuildType;

	private static final String API_TOOL_PLUGIN_ID = "org.eclipse.pde.api.tools"; //$NON-NLS-1$
	private static final String API_TOOL_NATURE = "org.eclipse.pde.api.tools.apiAnalysisNature"; //$NON-NLS-1$

	/**
	 * Constructor
	 *
	 * @param name
	 * @param project
	 */
	BuildJob(String name, IProject[] projects) {
		this(name, projects, IncrementalProjectBuilder.FULL_BUILD);
	}

	BuildJob(String name, IProject[] projects, int buildType) {
		super(name);
		fProjects = projects;
		this.fBuildType = buildType;
	}

	@Override
	public boolean belongsTo(Object family) {
		return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
	}

	/**
	 * Returns if this build job is covered by another build job
	 *
	 * @param other
	 * @return true if covered by another build job, false otherwise
	 */
	public boolean isCoveredBy(BuildJob other) {
		if (other.fProjects == null) {
			return true;
		}
		if (this.fProjects != null) {
			for (int i = 0, max = this.fProjects.length; i < max; i++) {
				if (!other.contains(this.fProjects[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean contains(IProject project) {
		if (project == null) {
			return false;
		}
		for (IProject fProject : this.fProjects) {
			if (project.equals(fProject)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		synchronized (getClass()) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			cancelBuild(ResourcesPlugin.FAMILY_MANUAL_BUILD);
		}
		try {
			if (fProjects != null) {
				SubMonitor localmonitor = SubMonitor.convert(monitor, PDEUIMessages.BuildJob_buildingProjects,
						fProjects.length);
				for (IProject currentProject : fProjects) {
					if (this.fBuildType == IncrementalProjectBuilder.FULL_BUILD) {
						BuildJob.setNullLastBuiltState(currentProject);
					}
					localmonitor.subTask(NLS.bind(PDEUIMessages.BuildJob_buildingProject, currentProject.getName()));

					try {
						HashSet<String> typesToDelete = new HashSet<>();
						IMarker[] findMarkers = currentProject.findMarkers(null, true, IResource.DEPTH_INFINITE);
						if (findMarkers != null) {
							for (IMarker iMarker : findMarkers) {
								if (iMarker.getType().startsWith((API_TOOL_PLUGIN_ID)))
									typesToDelete.add(iMarker.getType());
							}
						}
						for (String typeMarker : typesToDelete) {
							currentProject.deleteMarkers(typeMarker, false, IResource.DEPTH_INFINITE);
						}
					}
					catch (CoreException e) {
					}
					currentProject.build(IncrementalProjectBuilder.FULL_BUILD, localmonitor.split(1));

				}
			}
		} catch (CoreException e) {

		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void cancelBuild(Object jobfamily) {
		Job[] buildJobs = Job.getJobManager().find(jobfamily);
		for (Job curr : buildJobs) {
			if (curr != this && curr instanceof BuildJob) {
				BuildJob job = (BuildJob) curr;
				if (job.isCoveredBy(this)) {
					curr.cancel(); // cancel all other build jobs of our
									// kind
				}
			}
		}
	}

	public static void setNullLastBuiltState(IProject project) {
		try {
			File file = getSerializationFile(project);
			if (file != null && file.exists()) {
				file.delete();
			}
		} catch (SecurityException se) {
			// could not delete file: cannot do much more
		}

	}

	static File getSerializationFile(IProject project) {
		if (!project.exists()) {
			return null;
		}
		IPath workingLocation = project.getWorkingLocation(API_TOOL_PLUGIN_ID);
		return workingLocation.append("state.dat").toFile(); //$NON-NLS-1$
	}

	public static IProject[] getApiProjects() {
		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<IProject> temp = new ArrayList<>();
		IProject project = null;
		for (IProject allProject : allProjects) {
			project = allProject;
			if (project.isAccessible()) {
				try {
					if (project.hasNature(API_TOOL_NATURE)) {
						temp.add(project);
					}
				} catch (CoreException e) {
					// should not happen
				}
			}
		}
		IProject[] projects = null;
		if (!temp.isEmpty()) {
			projects = new IProject[temp.size()];
			temp.toArray(projects);
		}
		return projects;
	}

	public static Job getBuildJob(final IProject[] projects) {
		Assert.isNotNull(projects);
		Job buildJob = new BuildJob(PDEUIMessages.BuildJob_building, projects);
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true);
		return buildJob;
	}
}