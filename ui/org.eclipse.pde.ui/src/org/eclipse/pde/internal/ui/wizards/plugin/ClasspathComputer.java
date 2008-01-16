/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.team.core.RepositoryProvider;

public class ClasspathComputer {

	private static Hashtable fSeverityTable = null;
	private static final int SEVERITY_ERROR = 3;
	private static final int SEVERITY_WARNING = 2;
	private static final int SEVERITY_IGNORE = 1;

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
		String ee = getExecutionEnvironment(model.getBundleDescription());
		result.add(createJREEntry(ee));
		setComplianceOptions(JavaCore.create(project), ExecutionEnvironmentAnalyzer.getCompliance(ee));

		// add pde container
		result.add(createContainerEntry());

		IClasspathEntry[] entries = (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
		IJavaProject javaProject = JavaCore.create(project);
		IJavaModelStatus validation = JavaConventions.validateClasspath(javaProject, entries, javaProject.getOutputLocation());
		if (!validation.isOK()) {
			PDECore.logErrorMessage(validation.getMessage());
			throw new CoreException(validation);
		}
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}

	public static void addSourceAndLibraries(IProject project, IPluginModelBase model, IBuild build, boolean clear, ArrayList result) throws CoreException {

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
					addLibraryEntry(project, libraries[i], attrs, result);
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
				attributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadoc)};
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
				if (project.findMember(folder) == null) {
					CoreUtility.createFolder(project.getFolder(folder));
				} else {
					IPackageFragmentRoot root = JavaCore.create(project).getPackageFragmentRoot(path.toString());
					if (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY) {
						result.add(root.getRawClasspathEntry());
						continue;
					}
				}
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

	private static void addLibraryEntry(IProject project, IPluginLibrary library, IClasspathAttribute[] attrs, ArrayList result) throws JavaModelException {
		String name = ClasspathUtilCore.expandLibraryName(library.getName());
		IResource jarFile = project.findMember(name);
		if (jarFile == null)
			return;

		IPackageFragmentRoot root = JavaCore.create(project).getPackageFragmentRoot(jarFile);
		if (root.exists() && root.getKind() == IPackageFragmentRoot.K_BINARY) {
			IClasspathEntry oldEntry = root.getRawClasspathEntry();
			if (oldEntry.getSourceAttachmentPath() != null && !result.contains(oldEntry)) {
				result.add(oldEntry);
				return;
			}
		}

		IClasspathEntry entry = createClasspathEntry(project, jarFile, name, attrs, library.isExported());
		if (!result.contains(entry))
			result.add(entry);
	}

	private static void addJARdPlugin(IProject project, String filename, IClasspathAttribute[] attrs, ArrayList result) {
		String name = ClasspathUtilCore.expandLibraryName(filename);
		IResource jarFile = project.findMember(name);
		if (jarFile != null) {
			IClasspathEntry entry = createClasspathEntry(project, jarFile, filename, attrs, true);
			if (!result.contains(entry))
				result.add(entry);
		}
	}

	private static IClasspathEntry createClasspathEntry(IProject project, IResource library, String fileName, IClasspathAttribute[] attrs, boolean isExported) {
		String sourceZipName = ClasspathUtilCore.getSourceZipName(fileName);
		IResource resource = project.findMember(sourceZipName);
		// if zip file does not exist, see if a directory with the source does.  This in necessary how we import source for individual source bundles.
		if (resource == null && sourceZipName.endsWith(".zip")) { //$NON-NLS-1$
			resource = project.findMember(sourceZipName.substring(0, sourceZipName.length() - 4));
			if (resource == null)
				// if we can't find the the source for a library, then try to find the common source location set up to share source from one jar to all libraries.
				// see PluginImportOperation.linkSourceArchives
				resource = project.getFile(project.getName() + "src.zip"); //$NON-NLS-1$
		}
		IPath srcAttachment = resource != null ? resource.getFullPath() : library.getFullPath();
		return JavaCore.newLibraryEntry(library.getFullPath(), srcAttachment, null, new IAccessRule[0], attrs, isExported);
	}

	private static String getExecutionEnvironment(BundleDescription bundleDescription) {
		if (bundleDescription != null) {
			String[] envs = bundleDescription.getExecutionEnvironments();
			if (envs.length > 0)
				return envs[0];
		}
		return null;
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
		} else if (JavaCore.VERSION_1_6.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
			map.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
			map.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
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
			updateSeverityComplianceOption(map, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.WARNING);
			updateSeverityComplianceOption(map, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.WARNING);
		} else if (JavaCore.VERSION_1_3.equals(compliance)) {
			map.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3);
			map.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_1);
			updateSeverityComplianceOption(map, JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.IGNORE);
			updateSeverityComplianceOption(map, JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.IGNORE);
		}
		project.setOptions(map);
	}

	private static void updateSeverityComplianceOption(Map map, String key, String value) {
		Integer current_value = null;
		Integer new_value = null;
		String current_string_value = null;
		int current_int_value = 0;
		int new_int_value = 0;
		// Initialize the severity table (only once)
		if (fSeverityTable == null) {
			fSeverityTable = new Hashtable(SEVERITY_ERROR);
			fSeverityTable.put(JavaCore.IGNORE, new Integer(SEVERITY_IGNORE));
			fSeverityTable.put(JavaCore.WARNING, new Integer(SEVERITY_WARNING));
			fSeverityTable.put(JavaCore.ERROR, new Integer(SEVERITY_ERROR));
		}
		// Get the current severity
		current_string_value = (String) map.get(key);
		if (current_string_value != null) {
			current_value = (Integer) fSeverityTable.get(current_string_value);
			if (current_value != null) {
				current_int_value = current_value.intValue();
			}
		}
		// Get the new severity
		new_value = (Integer) fSeverityTable.get(value);
		if (new_value != null) {
			new_int_value = new_value.intValue();
		}
		// If the current severity is not higher than the new severity, replace it
		if (new_int_value > current_int_value) {
			map.put(key, value);
		}
	}

	public static IClasspathEntry createJREEntry(String ee) {
		IPath path = null;
		if (ee != null) {
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment env = manager.getEnvironment(ee);
			if (env != null)
				path = JavaRuntime.newJREContainerPath(env);
		}
		if (path == null)
			path = JavaRuntime.newDefaultJREContainerPath();
		return JavaCore.newContainerEntry(path);
	}

	public static IClasspathEntry createContainerEntry() {
		return JavaCore.newContainerEntry(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH);
	}

}
