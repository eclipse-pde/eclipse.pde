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

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
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
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.team.core.RepositoryProvider;

public class ClasspathComputer {
	
	public static void setClasspath(IProject project, IPluginModelBase model) throws CoreException {
		IClasspathEntry[] entries = getClasspath(project, model, false);
		JavaCore.create(project).setRawClasspath(entries, null);
	}
	
	public static IClasspathEntry[] getClasspath(IProject project, IPluginModelBase model, boolean clear) throws CoreException {

		ArrayList result = new ArrayList();
				
		// add own libraries/source
		addSourceAndLibraries(project, model, clear, result);
	
		// add JRE
		result.add(ClasspathUtilCore.createJREEntry());

		// add pde container
		result.add(ClasspathUtilCore.createContainerEntry());

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
		IClasspathAttribute[] attrs = getClasspathAttributes(project, model);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IBuildEntry buildEntry = build == null ? null : build.getEntry("source." + libraries[i].getName()); //$NON-NLS-1$
			if (buildEntry != null) {
				addSourceFolder(buildEntry, project, paths, result);
			} else {
				if (libraries[i].getName().equals(".")) //$NON-NLS-1$
					addJARdPlugin(project, getFilename(model), attrs, result);
				else
					addLibraryEntry(project, libraries[i], libraries[i].isExported(), attrs, result);
			}
		}
		if (libraries.length == 0) {
			if (build != null) {
				IBuildEntry buildEntry = build == null ? null : build.getEntry("source.."); //$NON-NLS-1$
				if (buildEntry != null) {
					addSourceFolder(buildEntry, project, paths, result);
				}
			} else if (ClasspathUtilCore.isBundle(model)) {
				addJARdPlugin(project, getFilename(model), attrs, result);
			}
		}
	}
	
	private static IClasspathAttribute[] getClasspathAttributes(IProject project, IPluginModelBase model) {
		IClasspathAttribute[] attributes = new IClasspathAttribute[0];
		if (!RepositoryProvider.isShared(project)) {			
			JavadocLocationManager manager = PDECore.getDefault().getJavadocLocationManager();
			String javadoc = manager.getJavadocLocation(model);
			if (javadoc != null) {
				attributes = new IClasspathAttribute[] 
				   {JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadoc)};
			}
		}
		return attributes;
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
	
	private static void addLibraryEntry(IProject project, IPluginLibrary library, boolean exported, IClasspathAttribute[] attrs, ArrayList result) {
		String name = ClasspathUtilCore.expandLibraryName(library.getName());
		IResource jarFile = project.findMember(name);
		if (jarFile != null) {
			IResource resource = project.findMember(getSourceZipName(name));
			if (resource == null)
				resource = project.findMember(new Path(getSourceZipName(name)).lastSegment());
			IPath srcAttachment = resource != null ? resource.getFullPath() : null;
			IClasspathEntry entry = JavaCore.newLibraryEntry(jarFile.getFullPath(), srcAttachment, null, new IAccessRule[0], attrs, exported);
			if (!result.contains(entry))
				result.add(entry);
		}
	}

	private static void addJARdPlugin(IProject project, String filename, IClasspathAttribute[] attrs, ArrayList result) {		
		String name = ClasspathUtilCore.expandLibraryName(filename);
		IResource jarFile = project.findMember(name);
		if (jarFile != null) {
			IResource resource = project.findMember(getSourceZipName(name));
			IPath srcAttachment = resource != null ? resource.getFullPath() : jarFile.getFullPath();
			IClasspathEntry entry =
				JavaCore.newLibraryEntry(jarFile.getFullPath(), srcAttachment, null, new IAccessRule[0], attrs, true);
			if (!result.contains(entry))
				result.add(entry);
		}
	}

	public static String getSourceZipName(String libraryName) {
		int dot = libraryName.lastIndexOf('.');
		return (dot != -1) ? libraryName.substring(0, dot) + "src.zip" : libraryName;	 //$NON-NLS-1$
	}
	
}
