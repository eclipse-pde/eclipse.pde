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

import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;

import aQute.bnd.osgi.Jar;

/**
 * This jar behaves like a pde-build on the java project that is including all
 * compiled class files from the binaries, and evaluate the bin includes
 */
public class PdeBuildJar extends Jar {

	public PdeBuildJar(IJavaProject javaProject) throws CoreException {
		this(javaProject, false);
	}

	public PdeBuildJar(IJavaProject javaProject, boolean includeTest) throws CoreException {
		super(javaProject.getProject().getName());
		IProject project = javaProject.getProject();
		IFile buildFile = project.getFile(ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
		if (buildFile.exists()) {
			IBuild build = new WorkspaceBuildModel(buildFile).getBuild();
			IBuildEntry[] buildEntries = build.getBuildEntries();
			for (IBuildEntry entry : buildEntries) {
				String name = entry.getName();
				if (name.startsWith(IBuildEntry.OUTPUT_PREFIX)) {
					String folder = entry.getFirstToken();
					if (folder != null) {
						IFolder outputFolder = project.getFolder(folder);
						if (outputFolder.exists()) {
							// TODO if the library is not '.' then it should
							// actually become an embedded jar!
							include(outputFolder, ""); //$NON-NLS-1$
						}
					}
				}
			}
			IBuildEntry entry = build.getEntry(IBuildEntry.BIN_INCLUDES);
			if (entry != null) {
				// TODO
			}
		}
		if (includeTest) {
			// TODO
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			IPath outputLocation = javaProject.getOutputLocation();
			for (IClasspathEntry entry : classpath) {
				System.out.println(entry);
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.isTest()) {
					IPath testOutput = Objects.requireNonNullElse(entry.getOutputLocation(), outputLocation);
					System.out.println(testOutput);
					IFolder otherOutputFolder = project.getWorkspace().getRoot().getFolder(testOutput);
					include(otherOutputFolder, ""); //$NON-NLS-1$
				}
			}
		}

	}

	private void include(IFolder folder, String prefix) throws CoreException {
		for (IResource resource : folder.members()) {
			if (resource instanceof IFile file) {
				putResource(prefix + file.getName(), new FileResource(file));
			} else if (resource instanceof IFolder subfolder) {
				include(subfolder, prefix + subfolder.getName() + "/"); //$NON-NLS-1$
			}
		}
	}

}
