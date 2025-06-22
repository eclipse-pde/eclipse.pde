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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.natures.PluginProject;

/**
 * Like a regular {@link PdeProjectJar} but additionally includes any test
 * folders that are not part of the usual bin includes.
 */
public class PdeTestProjectJar extends PdeProjectJar {

	public PdeTestProjectJar(IProject project) throws CoreException {
		super(project);
		if (PluginProject.isJavaProject(project)) {
			IJavaProject javaProject = JavaCore.create(project);
			IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
			IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
			for (IClasspathEntry cp : classpath) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE && cp.isTest()) {
					IPath location = cp.getOutputLocation();
					if (location != null) {
						IFolder otherOutputFolder = workspaceRoot.getFolder(location);
						FileResource.addResources(this, otherOutputFolder, null);
					}
				}
			}
		}
	}

}
