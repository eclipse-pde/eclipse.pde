/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

public class PluginJavaSearchUtil {
	
	public static IPlugin[] getPluginImports(IPluginImport dep) {
		return getPluginImports(dep.getId());
	}

	public static IPlugin[] getPluginImports(String pluginImportID) {
		HashSet set = new HashSet();
		collectAllPrerequisites(PDECore.getDefault().findPlugin(pluginImportID), set);
		return (IPlugin[]) set.toArray(new IPlugin[set.size()]);
	}
	
	public static void collectAllPrerequisites(IPlugin plugin, HashSet set) {
		if (plugin == null || !set.add(plugin))
			return;
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isReexported()) {
				IPlugin child = PDECore.getDefault().findPlugin(imports[i].getId());
				if (child != null)
					collectAllPrerequisites(child, set);
			}
		}
	}
	
	public static boolean provideExtensionPoint(IPluginModelBase model, IPlugin[] plugins) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			String extPoint = extensions[i].getPoint();
			for (int j = 0; j < plugins.length; j++) {
				IPluginExtensionPoint[] points = plugins[j].getExtensionPoints();			
				for (int k = 0; k < points.length; k++) {
					if (extPoint.equals(points[k].getFullId()))
						return true;
				}
			}
		}
		return false;
	}
	
	public static IPackageFragment[] collectPackageFragments(
		IPluginBase[] plugins,
		IJavaProject parentProject)
		throws JavaModelException {
		ArrayList result = new ArrayList();
		IPackageFragmentRoot[] roots = parentProject.getAllPackageFragmentRoots();

		for (int i = 0; i < plugins.length; i++) {
			IPluginModelBase model = (IPluginModelBase)plugins[i].getModel();
			IResource resource = model.getUnderlyingResource();
			if (resource == null) {
				ArrayList libraryPaths = new ArrayList();
				addLibraryPaths(model, libraryPaths);
				for (int j = 0; j < roots.length; j++) {
					if (libraryPaths.contains(roots[j].getPath())) {
						extractPackageFragments(roots[j], result);
					}
				}
			} else {
				IProject project = resource.getProject();
				for (int j = 0; j < roots.length; j++) {
					IJavaProject jProject = (IJavaProject) roots[j].getParent();
					if (jProject.getProject().equals(project)) {
						extractPackageFragments(roots[j], result);
					}
				}
			}
		}
		return (IPackageFragment[]) result.toArray(new IPackageFragment[result.size()]);
	}

	private static void extractPackageFragments(IPackageFragmentRoot root, ArrayList result) {
		try {
			IJavaElement[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				IPackageFragment fragment = (IPackageFragment) children[i];
				if (fragment.getChildren().length > 0)
					result.add(fragment);
			}
		} catch (JavaModelException e) {
		}
	}

	private static void addLibraryPaths(IPluginModelBase model, ArrayList libraryPaths) {
		IPluginBase plugin = model.getPluginBase();
		
		IFragment[] fragments = new IFragment[0];
		if (plugin instanceof IPlugin)
			fragments = PDECore.getDefault().findFragmentsFor(plugin.getId(), plugin.getVersion());

		File file = new File(model.getInstallLocation());
		if (file.isFile()) {
			libraryPaths.add(new Path(file.getAbsolutePath()));
		} else {
			IPluginLibrary[] libraries = plugin.getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				String libraryName =
					ClasspathUtilCore.expandLibraryName(libraries[i].getName());
				String path =
					plugin.getModel().getInstallLocation() + Path.SEPARATOR + libraryName;
				if (new File(path).exists()) {
					libraryPaths.add(new Path(path));
				} else if (model instanceof IPluginModelBase){
					findLibraryInFragments(fragments, libraryName, libraryPaths);
				}
			}
		}
		if (plugin instanceof IPlugin && ((IPlugin)plugin).hasExtensibleAPI()) {
			for (int i = 0; i < fragments.length; i++) {
				addLibraryPaths((IPluginModelBase)fragments[i].getModel(), libraryPaths);
			}
		}
	}

	private static void findLibraryInFragments(
		IFragment[] fragments,
		String libraryName,
		ArrayList libraryPaths) {
		for (int i = 0; i < fragments.length; i++) {
			String path =
				fragments[i].getModel().getInstallLocation()
					+ Path.SEPARATOR
					+ libraryName;
			if (new File(path).exists()) {
				libraryPaths.add(new Path(path));
				break;
			}
		}
	}
	
	public static IJavaSearchScope createSeachScope(IJavaProject jProject) throws JavaModelException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		ArrayList filteredRoots = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getResource() != null
				&& roots[i].getResource().getProject().equals(jProject.getProject())) {
				filteredRoots.add(roots[i]);
			}
		}
		return SearchEngine.createJavaSearchScope(
			(IJavaElement[]) filteredRoots.toArray(
				new IJavaElement[filteredRoots.size()]));
	}


}
