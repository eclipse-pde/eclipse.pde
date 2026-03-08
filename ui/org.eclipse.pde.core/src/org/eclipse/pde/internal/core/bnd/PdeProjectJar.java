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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.natures.PluginProject;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;

/**
 * A {@link PdeProjectJar} packages a project into a jar like it would be
 * performed during a build. This can then be used to perform some operations as
 * if the project was packed as a jar, e.g. calculate manifests or alike. If the
 * result of such operation has to be used in the project directly, use
 * {@link ProjectJar} instead that explodes all additional data into the output
 * folder.
 * <p>
 * The jar content is driven by the {@code bin.includes} property in
 * {@code build.properties}:
 * <ul>
 * <li>{@code .} includes the compiled output (from {@code output.} entries) at
 * the jar root</li>
 * <li>Library names (e.g. {@code lib.jar}) with matching {@code output.}
 * entries become embedded inner jars</li>
 * <li>All other entries are included as files/folders from the project
 * root</li>
 * </ul>
 */
public class PdeProjectJar extends Jar {

	private static final String DOT = "."; //$NON-NLS-1$

	public PdeProjectJar(IProject project) throws CoreException {
		super(project.getName());
		IFile buildFile = project.getFile(ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
		if (!buildFile.exists()) {
			return;
		}
		IBuild build = new WorkspaceBuildModel(buildFile).getBuild();
		// Build a map from library name to its output build entry
		Map<String, IBuildEntry> outputEntries = new HashMap<>();
		for (IBuildEntry entry : build.getBuildEntries()) {
			String name = entry.getName();
			if (name.startsWith(IBuildEntry.OUTPUT_PREFIX)) {
				String library = name.substring(IBuildEntry.OUTPUT_PREFIX.length());
				outputEntries.put(library, entry);
			}
		}
		// bin.includes defines what goes into the jar
		IBuildEntry binIncludes = build.getEntry(IBuildEntry.BIN_INCLUDES);
		if (binIncludes == null) {
			return;
		}
		for (String token : binIncludes.getTokens()) {
			if (DOT.equals(token)) {
				includeDotEntry(project, outputEntries.get(DOT));
			} else {
				IBuildEntry outputEntry = outputEntries.get(token);
				if (outputEntry != null) {
					includeLibraryEntry(project, token, outputEntry);
				} else {
					includeFileEntry(project, token);
				}
			}
		}
	}

	/**
	 * Handles the {@code .} (dot) entry in {@code bin.includes}. The dot entry
	 * means the compiled classes should be included at the jar root.
	 * <p>
	 * If an {@code output.} entry exists, its folders are included. Otherwise,
	 * for Java projects the default JDT output folder is used.
	 */
	private void includeDotEntry(IProject project, IBuildEntry outputEntry) throws CoreException {
		if (outputEntry != null) {
			includeOutputFolders(project, outputEntry);
		} else if (PluginProject.isJavaProject(project)) {
			// '.' is in bin.includes but no output. entry exists;
			// fall back to the default Java output folder
			IJavaProject javaProject = JavaCore.create(project);
			IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
			IPath defaultOutput = javaProject.getOutputLocation();
			IFolder defaultOutputFolder = workspaceRoot.getFolder(defaultOutput);
			FileResource.addResources(this, defaultOutputFolder, null);
		}
	}

	/**
	 * Handles a library entry (e.g. {@code lib.jar}) in {@code bin.includes}
	 * that has a matching {@code output.lib.jar} entry. The compiled output is
	 * packaged as an embedded inner jar.
	 */
	private void includeLibraryEntry(IProject project, String libraryName, IBuildEntry outputEntry)
			throws CoreException {
		Jar innerJar = new Jar(libraryName);
		includeOutputFolders(project, outputEntry, innerJar);
		putResource(libraryName, new JarResource(innerJar));
	}

	/**
	 * Includes all output folders from the given entry into this jar.
	 */
	private void includeOutputFolders(IProject project, IBuildEntry outputEntry) throws CoreException {
		includeOutputFolders(project, outputEntry, this);
	}

	/**
	 * Includes all output folders from the given entry into the specified target
	 * jar.
	 */
	private static void includeOutputFolders(IProject project, IBuildEntry outputEntry, Jar targetJar)
			throws CoreException {
		for (String folder : outputEntry.getTokens()) {
			String folderPath = folder.endsWith("/") ? folder.substring(0, folder.length() - 1) : folder; //$NON-NLS-1$
			IFolder outputFolder = project.getFolder(folderPath);
			if (outputFolder.exists()) {
				FileResource.addResources(targetJar, outputFolder, null);
			}
		}
	}

	/**
	 * Handles a regular file or folder entry from {@code bin.includes} that
	 * does not have a matching {@code output.} entry. The file or folder is
	 * included directly from the project root.
	 */
	private void includeFileEntry(IProject project, String token) throws CoreException {
		String path = token.endsWith("/") ? token.substring(0, token.length() - 1) : token; //$NON-NLS-1$
		IResource resource = project.findMember(path);
		if (resource instanceof IFile file) {
			putResource(token, new FileResource(file));
		} else if (resource instanceof IFolder folder) {
			String prefix = token.endsWith("/") ? token : token + "/"; //$NON-NLS-1$ //$NON-NLS-2$
			includeFolder(folder, prefix);
		}
	}

	private void includeFolder(IFolder folder, String prefix) throws CoreException {
		if (!folder.exists()) {
			return;
		}
		for (IResource resource : folder.members()) {
			if (resource instanceof IFile file) {
				putResource(prefix + file.getName(), new FileResource(file));
			} else if (resource instanceof IFolder subfolder) {
				includeFolder(subfolder, prefix + subfolder.getName() + "/"); //$NON-NLS-1$
			}
		}
	}

}
