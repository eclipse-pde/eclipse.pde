/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.bnd.ui;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class Workspaces {

	private static ServiceTracker<Workspace, Workspace> tracker;

	public static Optional<Workspace> getWorkspace(IProject project) {
		if (project != null) {
			Project bndProject = Adapters.adapt(project, Project.class);
			if (bndProject != null) {
				return Optional.ofNullable(bndProject.getWorkspace());
			} else {
				
				if(isCnf(project)) {
					
					try {
						File wsDir = project.getLocation().toFile().getParentFile();
						return Optional.ofNullable(Workspace.getWorkspace(wsDir));
					} catch (Exception e) {
					}
				}
				else {
					IFile bndFile = project.getFile(Project.BNDFILE);
					if (bndFile.exists()) {
						IPath location = bndFile.getLocation();
						if (location != null) {
							File file = location.toFile();
							if (file != null) {
								try {
									Project nativeProject = Workspace.getProject(file.getParentFile());
									if (nativeProject != null) {
										return Optional.ofNullable(nativeProject.getWorkspace());
									}
								} catch (Exception e) {
								}
							}
						}
					}
				}
				
			}
		}
		return Optional.empty();
	}
	
	private static boolean isCnf(IProject project) {
		IPath projectPath = project.getLocation();
		if (projectPath != null) {
			return Project.BNDCNF.equals(projectPath.lastSegment());
		}
		return false;
	}

	public static synchronized Optional<Workspace> getGlobalWorkspace() {
		// TODO the UI should support display multiple workspaces and give the user a
		// choice instead of simply using the highest ranked!
		if (tracker == null) {
			Bundle bundle = FrameworkUtil.getBundle(Workspaces.class);
			if (bundle == null) {
				return Optional.empty();
			}
			BundleContext bundleContext = bundle.getBundleContext();
			if (bundleContext == null) {
				return Optional.empty();
			}
			tracker = new ServiceTracker<>(bundleContext, Workspace.class, null);
			tracker.open();
		}
		return Optional.ofNullable(tracker.getService());
	}

	public static String getName(Workspace workspace) {
		if (workspace == null) {
			return null;
		}
		String name = workspace.get("workspaceName");
		if (name != null && !name.isBlank()) {
			return name;
		}
		return workspace.getBase().getName();
	}

	public static String getDescription(Workspace workspace) {
		if (workspace == null) {
			return null;
		}
		String name = workspace.get("workspaceDescription");
		if (name != null && !name.isBlank()) {
			return name;
		}
		return String.format("Workspace at location %s", workspace.getBase().getAbsolutePath());
	}
}
