/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class ClasspathComputer {

	public static void setClasspath(IPluginModelBase model,
			IProgressMonitor monitor) throws CoreException {

		Vector result = new Vector();
		monitor.beginTask("", 3); //$NON-NLS-1$

		// add own libraries/source
		addSourceAndLibraries(model, result);
		monitor.worked(1);

		result.add(ClasspathUtilCore.createContainerEntry());
		monitor.worked(1);

		// add JRE
		result.add(ClasspathUtilCore.createJREEntry());
		monitor.worked(1);

		IClasspathEntry[] entries = (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);

		IJavaProject javaProject = JavaCore.create(model.getUnderlyingResource().getProject());
		IJavaModelStatus validation = JavaConventions.validateClasspath(
				javaProject, entries, javaProject.getOutputLocation());
		if (!validation.isOK()) {
			PDECore.logErrorMessage(validation.getMessage());
			throw new CoreException(validation);
		}
		javaProject.setRawClasspath(entries, monitor);
		monitor.done();
	}

	private static void addSourceAndLibraries(IPluginModelBase model,
			Vector result) throws CoreException {

		IProject project = model.getUnderlyingResource().getProject();

		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		IBuild build = ClasspathUtilCore.getBuild(model);
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			if (IPluginLibrary.RESOURCE.equals(library.getType()))
				continue;
			IBuildEntry buildEntry = build == null ? null : build
					.getEntry("source." + library.getName()); //$NON-NLS-1$
			if (buildEntry != null) {
				String[] folders = buildEntry.getTokens();
				for (int k = 0; k < folders.length; k++) {
					IPath path = project.getFullPath().append(folders[k]);
					if (project.findMember(folders[k]) != null) {
						IClasspathEntry entry = JavaCore.newSourceEntry(path);
						if (!result.contains(entry))
							result.add(entry);
					} else {
						addSourceFolder(folders[k], project, result);
					}
				}
			} else {
				IClasspathEntry entry = ClasspathUtilCore.createLibraryEntry(library, library
						.isExported(), false);
				if (entry != null && !result.contains(entry))
					result.add(entry);
			}
		}

		// keep existing source folders if they don't nest with new folders
		IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				if (!result.contains(entry)) {
					boolean doAdd = true;
					for (int j = 0; j < result.size(); j++) {
						IPath path = ((IClasspathEntry) result.get(j))
								.getPath();
						if (path.isPrefixOf(entry.getPath())
								|| entry.getPath().isPrefixOf(path)) {
							doAdd = false;
							break;
						}
					}
					if (doAdd)
						result.add(entry);
				}
			}
		}
	}

	protected static void addSourceFolder(String name, IProject project,
			Vector result) throws CoreException {
		CoreUtility.createFolder(project.getFolder(name), true, true, null);
		IClasspathEntry entry = JavaCore.newSourceEntry(project.getFullPath()
				.append(name));
		if (!result.contains(entry))
			result.add(entry);
	}

}
