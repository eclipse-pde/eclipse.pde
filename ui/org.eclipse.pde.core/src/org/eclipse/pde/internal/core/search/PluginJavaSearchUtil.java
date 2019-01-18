/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.search;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDEManager;

public class PluginJavaSearchUtil {

	public static IPluginModelBase[] getPluginImports(IPluginImport dep) {
		HashSet<IPluginModelBase> set = new HashSet<>();
		VersionRange range = new VersionRange(dep.getVersion());
		collectAllPrerequisites(PluginRegistry.findModel(dep.getId(), range, null), set);
		return set.toArray(new IPluginModelBase[set.size()]);
	}

	public static IPluginModelBase[] getPluginImports(String pluginImportID) {
		HashSet<IPluginModelBase> set = new HashSet<>();
		collectAllPrerequisites(PluginRegistry.findModel(pluginImportID), set);
		return set.toArray(new IPluginModelBase[set.size()]);
	}

	public static void collectAllPrerequisites(IPluginModelBase model, HashSet<IPluginModelBase> set) {
		if (model == null || !set.add(model)) {
			return;
		}
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (IPluginImport pluginImport : imports) {
			if (pluginImport.isReexported()) {
				IPluginModelBase child = PluginRegistry.findModel(pluginImport.getId());
				if (child != null) {
					collectAllPrerequisites(child, set);
				}
			}
		}
	}

	public static IPackageFragment[] collectPackageFragments(IPluginModelBase[] models, IJavaProject parentProject, boolean filterEmptyPackages) throws JavaModelException {
		ArrayList<IPackageFragment> result = new ArrayList<>();
		IPackageFragmentRoot[] roots = parentProject.getAllPackageFragmentRoots();

		for (IPluginModelBase model : models) {
			IResource resource = model.getUnderlyingResource();
			if (resource == null) {
				ArrayList<Path> libraryPaths = new ArrayList<>();
				addLibraryPaths(model, libraryPaths);
				for (IPackageFragmentRoot root : roots) {
					if (libraryPaths.contains(root.getPath())) {
						extractPackageFragments(root, result, filterEmptyPackages);
					}
				}
			} else {
				IProject project = resource.getProject();
				for (IPackageFragmentRoot root : roots) {
					IJavaProject jProject = (IJavaProject) root.getParent();
					if (jProject.getProject().equals(project)) {
						extractPackageFragments(root, result, filterEmptyPackages);
					}
				}
			}
		}
		return result.toArray(new IPackageFragment[result.size()]);
	}

	private static void extractPackageFragments(IPackageFragmentRoot root, ArrayList<IPackageFragment> result, boolean filterEmpty) {
		try {
			IJavaElement[] children = root.getChildren();
			for (IJavaElement element : children) {
				IPackageFragment fragment = (IPackageFragment) element;
				if (!filterEmpty || fragment.hasChildren()) {
					result.add(fragment);
				}
			}
		} catch (JavaModelException e) {
		}
	}

	private static void addLibraryPaths(IPluginModelBase model, ArrayList<Path> libraryPaths) {
		IPluginBase plugin = model.getPluginBase();

		IFragmentModel[] fragments = new IFragmentModel[0];
		if (plugin instanceof IPlugin) {
			fragments = PDEManager.findFragmentsFor(model);
		}

		File file = new File(model.getInstallLocation());
		if (file.isFile()) {
			libraryPaths.add(new Path(file.getAbsolutePath()));
		} else {
			IPluginLibrary[] libraries = plugin.getLibraries();
			for (IPluginLibrary library : libraries) {
				String libraryName = ClasspathUtilCore.expandLibraryName(library.getName());
				String path = plugin.getModel().getInstallLocation() + IPath.SEPARATOR + libraryName;
				if (new File(path).exists()) {
					libraryPaths.add(new Path(path));
				} else {
					findLibraryInFragments(fragments, libraryName, libraryPaths);
				}
			}
		}
		if (ClasspathUtilCore.hasExtensibleAPI(model)) {
			for (IFragmentModel fragment : fragments) {
				addLibraryPaths(fragment, libraryPaths);
			}
		}
	}

	private static void findLibraryInFragments(IFragmentModel[] fragments, String libraryName, ArrayList<Path> libraryPaths) {
		for (IFragmentModel fragment : fragments) {
			String path = fragment.getInstallLocation() + IPath.SEPARATOR + libraryName;
			if (new File(path).exists()) {
				libraryPaths.add(new Path(path));
				break;
			}
		}
	}

	public static IJavaSearchScope createSeachScope(IJavaProject jProject) throws JavaModelException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		ArrayList<IPackageFragmentRoot> filteredRoots = new ArrayList<>();
		for (IPackageFragmentRoot root : roots) {
			if (root.getResource() != null && root.getResource().getProject().equals(jProject.getProject())) {
				filteredRoots.add(root);
			}
		}
		return SearchEngine.createJavaSearchScope(filteredRoots.toArray(new IJavaElement[filteredRoots.size()]));
	}

}
