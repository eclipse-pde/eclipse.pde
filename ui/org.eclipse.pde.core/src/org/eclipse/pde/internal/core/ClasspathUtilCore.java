/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;

public class ClasspathUtilCore {

	public static void setClasspath(
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {

		Vector result = new Vector();
		monitor.beginTask("", 3); //$NON-NLS-1$

		// add own libraries/source
		addSourceAndLibraries(model, result);
		monitor.worked(1);

		result.add(createContainerEntry());
		monitor.worked(1);

		// add JRE
		result.add(createJREEntry());
		monitor.worked(1);

		IClasspathEntry[] entries =
			(IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);

		IJavaProject javaProject =
			JavaCore.create(model.getUnderlyingResource().getProject());
		IJavaModelStatus validation =
			JavaConventions.validateClasspath(
				javaProject,
				entries,
				javaProject.getOutputLocation());
		if (!validation.isOK()) {
			PDECore.logErrorMessage(validation.getMessage());
			throw new CoreException(validation);
		}
		javaProject.setRawClasspath(entries, monitor);
		monitor.done();
	}

	private static void computePluginEntries(
		IPluginModelBase model,
		Vector result,
		IProgressMonitor monitor) {
		try {
			HashSet alreadyAdded = new HashSet();
			if (model.isFragmentModel()) {
				addParentPlugin(
					(IFragment) model.getPluginBase(),
					result,
					alreadyAdded);
			}

			// add dependencies
			IPluginImport[] dependencies = model.getPluginBase().getImports();
			for (int i = 0; i < dependencies.length; i++) {
				IPluginImport dependency = dependencies[i];
				IPlugin plugin =
					PDECore.getDefault().findPlugin(
						dependency.getId(),
						dependency.getVersion(),
						dependency.getMatch());
				if (plugin != null) {
					addDependency(
						plugin,
						dependency.isReexported(),
						result,
						alreadyAdded);
				}
				if (monitor != null)
					monitor.worked(1);
			}

			addExtraClasspathEntries(model, result);

			// add implicit dependencies
			addImplicitDependencies(
				model.getPluginBase().getId(),
				model.getPluginBase().getSchemaVersion(),
				result,
				alreadyAdded);
			if (monitor != null)
				monitor.worked(1);
		} catch (CoreException e) {
		}

	}

	private static void addExtraClasspathEntries(IPluginModelBase model, Vector result)
		throws CoreException {
		IBuild build = getBuild(model);
		IBuildEntry entry = (build == null) ? null : build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
		if (entry == null)
			return;

		String[] tokens = entry.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			String device = new Path(tokens[i]).getDevice();
			IPath path = null;
			if (device == null) {
				path = new Path(model.getUnderlyingResource().getProject().getName());
				path = path.append(tokens[i]);
			} else if (device.equals("platform:")) { //$NON-NLS-1$
				path = new Path(tokens[i]);
				if (path.segmentCount() > 1 && path.segment(0).equals("plugin")) { //$NON-NLS-1$
					path = path.setDevice(null);
					path = path.removeFirstSegments(1);
				}
			}
			if (path != null) {
				IResource resource = PDECore.getWorkspace().getRoot().findMember(path);
				if (resource != null && resource instanceof IFile) {
					IClasspathEntry newEntry =
						JavaCore.newLibraryEntry(resource.getFullPath(), null, null);
					IProject project = resource.getProject();
					if (project.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jProject = JavaCore.create(project);
						IClasspathEntry[] entries = jProject.getRawClasspath();
						for (int j = 0; j < entries.length; j++) {
							if (entries[j].getEntryKind() == IClasspathEntry.CPE_LIBRARY
									&& entries[j].getContentKind() == IPackageFragmentRoot.K_BINARY
									&& entries[j].getPath().equals(resource.getFullPath())) {
								newEntry = JavaCore.newLibraryEntry(
										entries[j].getPath(), 
										entries[j].getSourceAttachmentPath(), 
										entries[j].getSourceAttachmentRootPath());
								break;
							}
						}
					}
					if (!result.contains(newEntry))
						result.add(newEntry);
				}
			}
		}
	}

	public static IClasspathEntry[] computePluginEntries(IPluginModelBase model) {
		Vector result = new Vector();
		computePluginEntries(model, result, null);
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}

	private static void addDependency(
		IPluginBase plugin,
		boolean isExported,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {

		if (!alreadyAdded.add(plugin))
			return;

		IResource resource = plugin.getModel().getUnderlyingResource();
		if (resource != null) {
			IProject project = resource.getProject();
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IClasspathEntry entry =
					JavaCore.newProjectEntry(project.getFullPath(), isExported);
				if (!result.contains(entry))
					result.add(entry);
			}
			if (plugin instanceof IPlugin && ((IPlugin)plugin).hasExtensibleAPI()) {
				IFragment[] fragments = PDECore.getDefault().findFragmentsFor(plugin.getId(), plugin.getVersion());
				for (int i = 0; i < fragments.length; i++) {
					addDependency(fragments[i], isExported, result, alreadyAdded);
				}
			}
			return;
		}

		String location = plugin.getModel().getInstallLocation();
		
		// handle Plugin-in-a-JAR
		if (new File(location).isFile() && location.endsWith(".jar")) { //$NON-NLS-1$
			IClasspathEntry entry =
				JavaCore.newLibraryEntry(
						new Path(location),
						new Path(location),
						null,
						isExported);
			if (entry != null && !result.contains(entry)) {
				result.add(entry);
			}			
		} else {
			IPluginLibrary[] libraries = plugin.getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				if (IPluginLibrary.RESOURCE.equals(libraries[i].getType())
					|| !libraries[i].isExported())
					continue;
				IClasspathEntry entry =
					createLibraryEntry(libraries[i], isExported);
				if (entry != null && !result.contains(entry)) {
					result.add(entry);
				}
			}
		}
		
		if (plugin instanceof IPlugin && ((IPlugin)plugin).hasExtensibleAPI()) {
			IFragment[] fragments = PDECore.getDefault().findFragmentsFor(plugin.getId(), plugin.getVersion());
			for (int i = 0; i < fragments.length; i++) {
				addDependency(fragments[i], isExported, result, alreadyAdded);
			}
		}

		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport dependency = imports[i];
			if (dependency.isReexported()) {
				IPlugin importedPlugin =
					PDECore.getDefault().findPlugin(
						dependency.getId(),
						dependency.getVersion(),
						dependency.getMatch());
				if (importedPlugin != null)
					addDependency(
						importedPlugin,
						isExported,
						result,
						alreadyAdded);
			}
		}
	}

	private static boolean isOSGiRuntime() {
		return PDECore.getDefault().getModelManager().isOSGiRuntime();
	}

	protected static void addImplicitDependencies(
		String id,
		String schemaVersion,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {
		
		if ((isOSGiRuntime() && schemaVersion != null)
			|| id.equals("org.eclipse.core.boot") //$NON-NLS-1$
			|| id.equals("org.apache.xerces") //$NON-NLS-1$
			|| id.startsWith("org.eclipse.swt")) //$NON-NLS-1$
			return;
		
		if (schemaVersion == null && isOSGiRuntime()) {
			if (!id.equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
				IPlugin plugin =
					PDECore.getDefault().findPlugin(
						"org.eclipse.core.runtime.compatibility"); //$NON-NLS-1$
				if (plugin != null)
					addDependency(plugin, false, result, alreadyAdded);
			}
		} else {
			IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.core.boot"); //$NON-NLS-1$
			if (plugin != null)
				addDependency(plugin, false, result, alreadyAdded);
			if (!id.equals("org.eclipse.core.runtime")) { //$NON-NLS-1$
				plugin = PDECore.getDefault().findPlugin("org.eclipse.core.runtime"); //$NON-NLS-1$
				if (plugin != null)
					addDependency(plugin, false, result, alreadyAdded);
			}
		}
	}
	
	public static IClasspathEntry createJREEntry() {
		return JavaCore.newContainerEntry(new Path(
		"org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$
	}

	public static void addLibraries(
		IPluginModelBase model,
		boolean unconditionallyExport,
		Vector result) {
		String location = model.getInstallLocation();
		if (new File(location).isFile()) { 
			IClasspathEntry entry =
				JavaCore.newLibraryEntry(
						new Path(location),
						new Path(location),
						null,
						unconditionallyExport);
			if (entry != null && !result.contains(entry)) {
				result.add(entry);
			}			
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				IClasspathEntry entry =
					createLibraryEntry(libraries[i], unconditionallyExport);
				if (entry != null && !result.contains(entry))
					result.add(entry);
			}
		}
	}

	private static void addParentPlugin(
		IFragment fragment,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {
		IPlugin parent =
			PDECore.getDefault().findPlugin(
				fragment.getPluginId(),
				fragment.getPluginVersion(),
				fragment.getRule());
		if (parent != null) {
			addDependency(parent, false, result, alreadyAdded);
			IPluginImport[] imports = parent.getImports();
			for (int i = 0; i < imports.length; i++) {
				if (!imports[i].isReexported()) {
					IPlugin plugin =
						PDECore.getDefault().findPlugin(
							imports[i].getId(),
							imports[i].getVersion(),
							imports[i].getMatch());
					if (plugin != null) {
						addDependency(plugin, false, result, alreadyAdded);
					}
				}
			}
		}
	}

	private static void addSourceAndLibraries(IPluginModelBase model, Vector result)
		throws CoreException {

		IProject project = model.getUnderlyingResource().getProject();

		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		IBuild build = getBuild(model);
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			if (IPluginLibrary.RESOURCE.equals(library.getType()))
				continue;
			IBuildEntry buildEntry =
				build == null ? null : build.getEntry("source." + library.getName()); //$NON-NLS-1$
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
				IClasspathEntry entry = createLibraryEntry(library, library.isExported());
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
						IPath path = ((IClasspathEntry)result.get(j)).getPath();
						if (path.isPrefixOf(entry.getPath()) || entry.getPath().isPrefixOf(path)) {
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

	protected static void addSourceFolder(String name, IProject project, Vector result)
		throws CoreException {
		CoreUtility.createFolder(project.getFolder(name), true, true, null);
		IClasspathEntry entry = JavaCore.newSourceEntry(project.getFullPath().append(name));
		if (!result.contains(entry))
			result.add(entry);
	}


	/**
	 * Creates a new instance of the classpath container entry for the given
	 * project.
	 * 
	 * @param project
	 */

	public static IClasspathEntry createContainerEntry() {
		return JavaCore.newContainerEntry(new Path(PDECore.CLASSPATH_CONTAINER_ID));
	}

	private static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		boolean exported) {
		try {
			String name = library.getName();
			String expandedName = expandLibraryName(name);

			IPluginModelBase model = library.getPluginModel();
			IPath path = getPath(model, expandedName);
			if (path == null) {
				if (model.isFragmentModel() || !containsVariables(name))
					return null;
				model = resolveLibraryInFragments(library, expandedName);
				if (model == null)
					return null;
				path = getPath(model, expandedName);
			}
			return JavaCore.newLibraryEntry(
				path,
				getSourceAnnotation(model, expandedName),
				null,
				exported);
		} catch (CoreException e) {
			return null;
		}
	}
	
	public static boolean containsVariables(String name) {
		return name.indexOf("$os$") != -1 //$NON-NLS-1$
			|| name.indexOf("$ws$") != -1 //$NON-NLS-1$
			|| name.indexOf("$nl$") != -1 //$NON-NLS-1$
			|| name.indexOf("$arch$") != -1; //$NON-NLS-1$
	}

	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0)
			return ""; //$NON-NLS-1$
		if (source.indexOf("$ws$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
					"\\$ws\\$", //$NON-NLS-1$
					"ws" + IPath.SEPARATOR + TargetPlatform.getWS()); //$NON-NLS-1$
		if (source.indexOf("$os$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
					"\\$os\\$", //$NON-NLS-1$
					"os" + IPath.SEPARATOR + TargetPlatform.getOS()); //$NON-NLS-1$
		if (source.indexOf("$nl$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
						"\\$nl\\$", //$NON-NLS-1$
						"nl" + IPath.SEPARATOR + TargetPlatform.getNL()); //$NON-NLS-1$
		if (source.indexOf("$arch$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
						"\\$arch\\$", //$NON-NLS-1$
						"arch" + IPath.SEPARATOR + TargetPlatform.getOSArch()); //$NON-NLS-1$
		return source;
	}

	protected static IBuild getBuild(IPluginModelBase model) throws CoreException {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IProject project = model.getUnderlyingResource().getProject();
			IFile buildFile = project.getFile("build.properties"); //$NON-NLS-1$
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}
		return (buildModel != null) ? buildModel.getBuild() : null;
	}

	private static IPath getSourceAnnotation(IPluginModelBase model, String libraryName)
		throws CoreException {
		IPath path = null;
		int dot = libraryName.lastIndexOf('.');
		if (dot != -1) {
			String zipName = libraryName.substring(0, dot) + "src.zip"; //$NON-NLS-1$
			path = getPath(model, zipName);
			if (path == null) {
				SourceLocationManager manager =
					PDECore.getDefault().getSourceLocationManager();
				path =
					manager.findPath(
						model.getPluginBase(),
						new Path(zipName));
			}
		}
		return path;
	}

	private static IPluginModelBase resolveLibraryInFragments(
		IPluginLibrary library,
		String libraryName) {
		IFragment[] fragments =
			PDECore.getDefault().findFragmentsFor(
				library.getPluginBase().getId(),
				library.getPluginBase().getVersion());

		for (int i = 0; i < fragments.length; i++) {
			IPath path = getPath(fragments[i].getPluginModel(), libraryName);
			if (path != null)
				return fragments[i].getPluginModel();
			
			// the following case is to account for cases when a plugin like org.eclipse.swt.win32 is checked out from
			// cvs (i.e. it has no library) and org.eclipse.swt is not in the workspace.
			// we have to find the external swt.win32 fragment to locate the $ws$/swt.jar.
			if (fragments[i].getModel().getUnderlyingResource() != null) {
				ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(fragments[i].getId());
				IPluginModelBase model = entry.getExternalModel();
				if (model != null && model instanceof IFragmentModel) {
					path = getPath(model, libraryName);
					if (path != null)
						return model;
				}
			}
		}
		return null;
	}

	private static IPath getPath(IPluginModelBase model, String libraryName) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IResource jarFile = resource.getProject().findMember(libraryName);
			if (jarFile != null)
				return jarFile.getFullPath();
		} else {
			File file = new File(model.getInstallLocation(), libraryName);
			if (file.exists())
				return new Path(file.getAbsolutePath());
		}
		return null;
	}

}
