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
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

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
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ExecutionEnvironmentAnalyzer;
import org.eclipse.pde.internal.core.JavadocLocationManager;
import org.eclipse.pde.internal.core.PDECore;
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
				
		IBuild build = getBuild(project);

		// add own libraries/source
		addSourceAndLibraries(project, model, build, clear, result);
	
		// add JRE and set compliance options
		String ee = getExecutionEnvironment(model.getBundleDescription(), build);	
		result.add(createJREEntry(ee));
		setComplianceOptions(JavaCore.create(project), ExecutionEnvironmentAnalyzer.getCompliance(ee));

		// add pde container
		result.add(createContainerEntry());

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

	public static void addSourceAndLibraries(IProject project, IPluginModelBase model, IBuild build, boolean clear, 
			ArrayList result) throws CoreException {
		
		HashSet paths = new HashSet();

		// keep existing source folders
		if (!clear) {
			IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					if (paths.add(entry.getPath()))
						result.add(entry);
				}
			}
		}

		IClasspathAttribute[] attrs = getClasspathAttributes(project, model);
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IBuildEntry buildEntry = build == null ? null : build.getEntry("source." + libraries[i].getName()); //$NON-NLS-1$
			if (buildEntry != null) {
				addSourceFolder(buildEntry, project, paths, result);
			} else {
				if (libraries[i].getName().equals(".")) //$NON-NLS-1$
					addJARdPlugin(project, ClasspathUtilCore.getFilename(model), attrs, result);
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
			} else if (ClasspathUtilCore.hasBundleStructure(model)) {
				addJARdPlugin(project, ClasspathUtilCore.getFilename(model), attrs, result);
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
	
	private static String getExecutionEnvironment(BundleDescription bundleDescription, IBuild build) {
		String ee = null;
		
		if (build != null) {
			// try build.properties first
			IBuildEntry entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_JRE_COMPILATION_PROFILE);
			if (entry != null) {
				String[] tokens = entry.getTokens();
				if (tokens.length > 0)
					ee = tokens[0];
			}
		}
		// try the Bundle-RequiredExecutionEnvironment header
		if (ee == null && bundleDescription != null) {
			String[] envs = bundleDescription.getExecutionEnvironments();
			if (envs.length > 0)
				ee = envs[0];
		}
		return ee;
	}
	
	public static void setComplianceOptions(IJavaProject project, String compliance) {
		Map map = project.getOptions(false);		
		if (compliance == null) {
			if (map.size() > 0) {
				map.remove(JavaCore.COMPILER_COMPLIANCE);
				map.remove(JavaCore.COMPILER_SOURCE);
				map.remove(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
				map.remove(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER);
				map.remove(JavaCore.COMPILER_PB_ENUM_IDENTIFIER);	
			} else {
				return;
			}
		} else if (JavaCore.VERSION_1_5.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
		} else if (JavaCore.VERSION_1_4.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);
		} else if (JavaCore.VERSION_1_3.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_1);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
		}
		project.setOptions(map);		
	}
	
	public static IClasspathEntry createJREEntry(String ee) {
		IPath path = new Path(JavaRuntime.JRE_CONTAINER);		
		if (ee != null) {
			path = path.append(JavaRuntime.EXTENSION_POINT_EXECUTION_ENVIRONMENTS);
			path = path.append(ee);
		}
		return JavaCore.newContainerEntry(path);
	}
	
	public static IClasspathEntry createContainerEntry() {
		return JavaCore.newContainerEntry(new Path(PDECore.CLASSPATH_CONTAINER_ID));
	}

}
