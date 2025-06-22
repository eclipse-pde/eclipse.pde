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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;

import aQute.bnd.osgi.Jar;

/**
 * A {@link PdeProjectJar} packages a project into a jar like it would be
 * performed during a build. This can then be used to perform some operations as
 * if the project was packed as a jar, e.g. calculate manifests or alike. If the
 * result of such operation has to be used in the project directly, use
 * {@link ProjectJar} instead that explodes all additional data into the output
 * folder.
 */
public class PdeProjectJar extends Jar {

	public PdeProjectJar(IProject project) throws CoreException {
		super(project.getName());
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
				// TODO adding bin included here!
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
