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
		if (!set.add(plugin))
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
			IPluginBase preReq = plugins[i];
			IResource resource = preReq.getModel().getUnderlyingResource();
			if (resource == null) {
				ArrayList libraryPaths = getLibraryPaths(preReq);
				for (int j = 0; j < roots.length; j++) {
					if (libraryPaths.contains(roots[j].getPath())) {
						extractFragments(roots[j], result);
					}
				}
			} else {
				IProject project = resource.getProject();
				for (int j = 0; j < roots.length; j++) {
					IJavaProject jProject = (IJavaProject) roots[j].getParent();
					if (jProject.getProject().equals(project)) {
						extractFragments(roots[j], result);
					}
				}
			}
		}
		return (IPackageFragment[]) result.toArray(new IPackageFragment[result.size()]);

	}

	private static void extractFragments(IPackageFragmentRoot root, ArrayList result) {
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

	private static ArrayList getLibraryPaths(IPluginBase plugin) {
		ArrayList libraryPaths = new ArrayList();
		IFragment[] fragments =
			PDECore.getDefault().findFragmentsFor(plugin.getId(), plugin.getVersion());

		IPluginLibrary[] libraries = plugin.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			String libraryName =
				ClasspathUtilCore.expandLibraryName(libraries[i].getName());
			String path =
				plugin.getModel().getInstallLocation() + Path.SEPARATOR + libraryName;
			if (new File(path).exists()) {
				libraryPaths.add(new Path(path));
			} else {
				findLibraryInFragments(fragments, libraryName, libraryPaths);
			}
		}
		return libraryPaths;
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
