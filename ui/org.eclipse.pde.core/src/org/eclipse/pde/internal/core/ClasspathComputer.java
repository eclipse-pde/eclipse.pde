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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class ClasspathComputer {
	
	public static void setClasspath(IProject project, IPluginModelBase model) throws CoreException {
		IClasspathEntry[] entries = getClasspath(project, model, false);
		JavaCore.create(project).setRawClasspath(entries, null);
	}
	
	public static IClasspathEntry[] getClasspath(IProject project, IPluginModelBase model, boolean clear) throws CoreException {

		ArrayList result = new ArrayList();
		
		// add own libraries/source
		addSourceAndLibraries(project, model, clear, result);
	
		// add pde container
		result.add(ClasspathUtilCore.createContainerEntry());

		// add JRE
		result.add(ClasspathUtilCore.createJREEntry());

		IClasspathEntry[] entries = (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
		IJavaProject javaProject = JavaCore.create(project);
		IJavaModelStatus validation = 
			JavaConventions.validateClasspath(
								javaProject, 
								entries, 
								javaProject.getOutputLocation());
		if (!validation.isOK()) {
			PDECore.logErrorMessage(validation.getMessage());
			throw new CoreException(validation);
		}
		return (IClasspathEntry[])result.toArray(new IClasspathEntry[result.size()]);
	}

	private static void addSourceAndLibraries(IProject project, IPluginModelBase model, boolean clear, 
			ArrayList result) throws CoreException {
		
		HashSet paths = new HashSet();

		// keep existing source folders
		if (!clear) {
			IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getPath().segmentCount() > 1) {
					if (paths.add(entry.getPath()))
						result.add(entry);
				}
			}
		}

		IBuild build = getBuild(project);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IBuildEntry buildEntry = build == null ? null : build.getEntry("source." + libraries[i].getName()); //$NON-NLS-1$
			if (buildEntry != null) {
				addSourceFolder(buildEntry, project, paths, result);
			} else {
				if (libraries[i].getName().equals(".")) //$NON-NLS-1$
					addJARdPlugin(project, getFilename(model), result);
				else
					addLibraryEntry(project, libraries[i], libraries[i].isExported(), result);
			}
		}
		if (libraries.length == 0) {
			if (build != null) {
				IBuildEntry buildEntry = build == null ? null : build.getEntry("source.."); //$NON-NLS-1$
				if (buildEntry != null) {
					addSourceFolder(buildEntry, project, paths, result);
				}
			} else if (isBundle(model)) {
				addJARdPlugin(project, getFilename(model), result);
			}
		}
	}
	
	private static void addSourceFolder(IBuildEntry buildEntry, IProject project, HashSet paths, ArrayList result) throws CoreException {
		String[] folders = buildEntry.getTokens();
		for (int j = 0; j < folders.length; j++) {
			String folder = folders[j];
			IPath path = project.getFullPath().append(folder);
			if (paths.add(path)) {
				if (project.findMember(folder) == null) 
					CoreUtility.createFolder(project.getFolder(folder));							
				result.add(JavaCore.newSourceEntry(path));
			} 
		}	
	}
	
	public static String getFilename(IPluginModelBase model) {
		StringBuffer buffer = new StringBuffer();
		String id = model.getPluginBase().getId();
		if (id != null)
			buffer.append(id);
		buffer.append("_"); //$NON-NLS-1$
		String version = model.getPluginBase().getVersion();
		if (version != null)
			buffer.append(version);
		buffer.append(".jar"); //$NON-NLS-1$
		return buffer.toString();
	}

	protected static IBuild getBuild(IProject project) throws CoreException {
		IFile buildFile = project.getFile("build.properties"); //$NON-NLS-1$
		IBuildModel buildModel = null;
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		return (buildModel != null) ? buildModel.getBuild() : null;
	}
	
	private static void addLibraryEntry(IProject project, IPluginLibrary library, boolean exported, ArrayList result) {
		String name = ClasspathUtilCore.expandLibraryName(library.getName());
		IResource jarFile = project.findMember(name);
		if (jarFile != null) {
			IResource resource = project.findMember(getSourceZipName(name));
			if (resource == null)
				resource = project.findMember(new Path(getSourceZipName(name)).lastSegment());
			IPath srcAttachment = resource != null ? resource.getFullPath() : null;
			IClasspathEntry entry = JavaCore.newLibraryEntry(jarFile.getFullPath(), srcAttachment, null, exported);
			if (!result.contains(entry))
				result.add(entry);
		}
	}

	private static void addJARdPlugin(IProject project, String filename, ArrayList result) {		
		String name = ClasspathUtilCore.expandLibraryName(filename);
		IResource jarFile = project.findMember(name);
		if (jarFile != null) {
			IResource resource = project.findMember(getSourceZipName(name));
			IPath srcAttachment = resource != null ? resource.getFullPath() : jarFile.getFullPath();
			IClasspathEntry entry =
				JavaCore.newLibraryEntry(jarFile.getFullPath(), srcAttachment, null, true);
			if (!result.contains(entry))
				result.add(entry);
		}
	}

	public static String getSourceZipName(String libraryName) {
		int dot = libraryName.lastIndexOf('.');
		return (dot != -1) ? libraryName.substring(0, dot) + "src.zip" : libraryName;	 //$NON-NLS-1$
	}
	
	public static boolean isBundle(IPluginModelBase model) {
		if (model instanceof IBundlePluginModelBase)
			return true;
		if (model.getUnderlyingResource() == null) {
			File file = new File(model.getInstallLocation());
			if (file.isDirectory())
				return new File(file, "META-INF/MANIFEST.MF").exists(); //$NON-NLS-1$
			ZipFile jarFile = null;
			try {
				jarFile = new ZipFile(file, ZipFile.OPEN_READ);
				return jarFile.getEntry("META-INF/MANIFEST.MF") != null; //$NON-NLS-1$
			} catch (IOException e) {
			} finally {
				try {
					if (jarFile != null)
						jarFile.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}


}
