/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

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
		boolean useClasspathContainer,
		IProgressMonitor monitor)
		throws CoreException {

		Vector result = new Vector();
		int numUnits = 3;
		if (!useClasspathContainer)
			numUnits += model.getPluginBase().getImports().length;
		monitor.beginTask("", numUnits);

		// add own libraries/source
		addSourceAndLibraries(model, result);
		monitor.worked(1);

		if (useClasspathContainer) {
			result.add(createContainerEntry());
			monitor.worked(1);
		} else {
			computePluginEntries(model, true, result, monitor);
		}

		// add JRE
		addJRE(result);
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
		boolean relative,
		Vector result,
		IProgressMonitor monitor) {
		try {
			HashSet alreadyAdded = new HashSet();
			if (model.isFragmentModel()) {
				addParentPlugin(
					(IFragment) model.getPluginBase(),
					relative,
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
						relative,
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
				relative,
				result,
				alreadyAdded);
			if (monitor != null)
				monitor.worked(1);
		} catch (CoreException e) {
		}

	}

	private static void addExtraClasspathEntries(IPluginModelBase model, Vector result)
		throws CoreException {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IFile buildFile =
				model.getUnderlyingResource().getProject().getFile("build.properties");
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}

		if (buildModel == null)
			return;

		IBuildEntry entry = buildModel.getBuild().getEntry("jars.extra.classpath");
		if (entry == null)
			return;

		String[] tokens = entry.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			String device = new Path(tokens[i]).getDevice();
			IPath path = null;
			if (device == null) {
				path = new Path(model.getUnderlyingResource().getProject().getName());
				path = path.append(tokens[i]);
			} else if (device.equals("platform:")) {
				path = new Path(tokens[i]);
				if (path.segmentCount() > 1 && path.segment(0).equals("plugin")) {
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
						IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
						for (int j = 0; j < roots.length; j++) {
							if (roots[j].getResource() != null
								&& roots[j].getResource().equals(resource)) {
								IPath attPath = roots[j].getSourceAttachmentPath();
								if (attPath != null) {
									newEntry =
										JavaCore.newLibraryEntry(
											resource.getFullPath(),
											attPath,
											roots[j].getSourceAttachmentRootPath());
								}
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
		computePluginEntries(model, false, result, null);
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}

	private static void addDependency(
		IPlugin plugin,
		boolean isExported,
		boolean relative,
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
			return;
		}

		IPluginLibrary[] libraries = plugin.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			if (IPluginLibrary.RESOURCE.equals(libraries[i].getType())
				|| !libraries[i].isExported())
				continue;
			IClasspathEntry entry =
				createLibraryEntry(libraries[i], isExported, relative);
			if (entry != null && !result.contains(entry)) {
				result.add(entry);
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
						relative,
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
		boolean relative,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {
		if (isOSGiRuntime() 
		|| id.equals("org.eclipse.core.boot")
		|| id.equals("org.apache.xerces")
		|| id.startsWith("org.eclipse.swt"))
			return;
		
		IPlugin plugin = PDECore.getDefault().findPlugin("org.eclipse.core.boot");
		if (plugin != null)
			addDependency(plugin, false, relative, result, alreadyAdded);
		if (!id.equals("org.eclipse.core.runtime")) {
			plugin = PDECore.getDefault().findPlugin("org.eclipse.core.runtime");
			if (plugin != null)
				addDependency(plugin, false, relative, result, alreadyAdded);
		}
	}

	protected static void addJRE(Vector result) {
		result.add(
			JavaCore.newContainerEntry(
				new Path("org.eclipse.jdt.launching.JRE_CONTAINER")));
	}

	public static void addLibraries(
		IPluginModelBase model,
		boolean unconditionallyExport,
		boolean relative,
		Vector result) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IClasspathEntry entry =
				createLibraryEntry(libraries[i], unconditionallyExport, relative);
			if (entry != null && !result.contains(entry))
				result.add(entry);
		}
	}

	private static void addParentPlugin(
		IFragment fragment,
		boolean relative,
		Vector result,
		HashSet alreadyAdded)
		throws CoreException {
		IPlugin parent =
			PDECore.getDefault().findPlugin(
				fragment.getPluginId(),
				fragment.getPluginVersion(),
				fragment.getRule());
		if (parent != null) {
			addDependency(parent, false, relative, result, alreadyAdded);
			IPluginImport[] imports = parent.getImports();
			for (int i = 0; i < imports.length; i++) {
				if (!imports[i].isReexported()) {
					IPlugin plugin =
						PDECore.getDefault().findPlugin(
							imports[i].getId(),
							imports[i].getVersion(),
							imports[i].getMatch());
					if (plugin != null) {
						addDependency(plugin, false, relative, result, alreadyAdded);
					}
				}
			}
		}
	}

	private static void addSourceAndLibraries(IPluginModelBase model, Vector result)
		throws CoreException {

		IProject project = model.getUnderlyingResource().getProject();
		IBuildEntry[] buildEntries = getBuildEntries(model, project);

		if (!WorkspaceModelManager.isBinaryPluginProject(project)) {
			// keep existing source folders
			IPackageFragmentRoot[] roots =
				JavaCore.create(project).getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE
					&& root.getPath().segmentCount() > 1) {
					IClasspathEntry entry = JavaCore.newSourceEntry(root.getPath());
					if (!result.contains(entry))
						result.add(entry);
				}
			}
		}

		// add libraries
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			if (IPluginLibrary.RESOURCE.equals(library.getType()))
				continue;
			boolean found = false;
			for (int j = 0; j < buildEntries.length; j++) {
				IBuildEntry buildEntry = buildEntries[j];
				// add corresponding source folder instead of library, if one
				// exists
				if (buildEntry.getName().equals("source." + library.getName())) {
					String[] folders = buildEntry.getTokens();
					for (int k = 0; k < folders.length; k++) {
						IPath path = project.getFullPath().append(folders[k]);
						if (path.toFile().exists()) {
							IClasspathEntry entry = JavaCore.newSourceEntry(path);
							if (!result.contains(entry))
								result.add(entry);
						} else {
							addSourceFolder(folders[k], project, result);
						}
					}
					found = true;
					break;
				}
			}
			// add library, since no source folder was found.
			if (!found) {
				IClasspathEntry entry =
					createLibraryEntry(library, library.isExported(), true);
				if (entry != null && !result.contains(entry))
					result.add(entry);

			}
		}
	}

	protected static void addSourceFolder(String name, IProject project, Vector result)
		throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path);
		IClasspathEntry entry = JavaCore.newSourceEntry(path);
		if (!result.contains(entry))
			result.add(entry);
	}

	private static void ensureFolderExists(IProject project, IPath folderPath)
		throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i = 1; i <= folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
		}
	}

	/**
	 * Creates a new instance of the classpath container entry for the given
	 * project.
	 * 
	 * @param project
	 */

	public static IClasspathEntry createContainerEntry() {
		IPath path = new Path(PDECore.CLASSPATH_CONTAINER_ID);
		return JavaCore.newContainerEntry(path);
	}

	private static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		boolean unconditionallyExport,
		boolean relative) {
		try {
			String expandedName = expandLibraryName(library.getName());
			boolean isExported = unconditionallyExport ? true : library.isFullyExported();

			IPluginModelBase model = library.getPluginModel();
			IPath path = getPath(model, expandedName);
			if (path == null) {
				if (model.isFragmentModel()
					|| !model.getPluginBase().getId().equals("org.eclipse.swt"))
					return null;
				model = resolveLibraryInFragments(library, expandedName);
				if (model == null)
					return null;
				path = getPath(model, expandedName);
			}
			if (relative && model.getUnderlyingResource() == null) {
				return JavaCore.newVariableEntry(
					EclipseHomeInitializer.createEclipseRelativeHome(path.toOSString()),
					getSourceAnnotation(model, expandedName, true),
					null,
					isExported);
			}

			return JavaCore.newLibraryEntry(
				path,
				getSourceAnnotation(model, expandedName, false),
				null,
				isExported);
		} catch (CoreException e) {
			return null;
		}
	}

	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0)
			return "";
		if (source.indexOf("$ws$") != -1)
			source =
				source.replaceAll(
					"\\$ws\\$",
					"ws" + IPath.SEPARATOR + TargetPlatform.getWS());
		if (source.indexOf("$os$") != -1)
			source =
				source.replaceAll(
					"\\$os\\$",
					"os" + IPath.SEPARATOR + TargetPlatform.getOS());
		return source;
	}

	private static IBuildEntry[] getBuildEntries(
		IPluginModelBase model,
		IProject project)
		throws CoreException {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IFile buildFile = project.getFile("build.properties");
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}
		if (buildModel != null)
			return buildModel.getBuild().getBuildEntries();

		return new IBuildEntry[0];
	}

	private static IPath getSourceAnnotation(
		IPluginModelBase model,
		String libraryName,
		boolean relative)
		throws CoreException {
		IPath path = null;
		int dot = libraryName.lastIndexOf('.');
		if (dot != -1) {
			String zipName = libraryName.substring(0, dot) + "src.zip";
			path = getPath(model, zipName);
			if (path == null) {
				IResource resource = model.getUnderlyingResource();
				SourceLocationManager manager =
					PDECore.getDefault().getSourceLocationManager();
				path =
					manager.findVariableRelativePath(
						model.getPluginBase(),
						new Path(zipName));
				if (path != null) {
					if (!relative
						|| (resource != null
							&& !resource.getProject().hasNature(JavaCore.NATURE_ID))
						|| (resource != null && resource.isLinked()))
						path = JavaCore.getResolvedVariablePath(path);
				}
			} else {
				if (relative)
					path =
						EclipseHomeInitializer.createEclipseRelativeHome(
							path.toOSString());
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
			IPath path = new Path(model.getInstallLocation()).append(libraryName);
			if (path.toFile().exists()) {
				return path;
			}
		}
		return null;
	}

}
