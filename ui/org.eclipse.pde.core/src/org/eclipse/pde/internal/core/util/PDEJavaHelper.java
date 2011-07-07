/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

public class PDEJavaHelper {

	/*static class Requestor extends TypeNameRequestor {
		int count = 0;
		
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName,
				char[][] enclosingTypeNames, String path) {
			count += 1;
		}
		
		public boolean hasMatches() {
			return count > 0;
		}
	}*/

	public static boolean isDiscouraged(String fullyQualifiedName, IJavaProject project, BundleDescription desc) {
		// allow classes within the project itself
		try {
			IType type = project.findType(fullyQualifiedName.replace('$', '.'));
			if (type != null && type.exists()) {
				HashMap map = PDEJavaHelper.getPackageFragmentsHash(project, Collections.EMPTY_LIST, false);
				if (map.containsValue(type.getPackageFragment())) {
					return false;
				}
			}
		} catch (JavaModelException e) {
		}

		// just grab the package
		int dot = fullyQualifiedName.lastIndexOf('.');
		if (dot != -1) // check for the default package case
			fullyQualifiedName = fullyQualifiedName.substring(0, dot);
		else
			fullyQualifiedName = "."; //$NON-NLS-1$

		State state = desc.getContainingState();
		StateHelper helper = state.getStateHelper();
		ExportPackageDescription[] exports = helper.getVisiblePackages(desc);
		for (int i = 0; i < exports.length; i++) {
			BundleDescription exporter = exports[i].getExporter();
			if (exporter == null)
				continue;

			if (fullyQualifiedName.equals(exports[i].getName()) && helper.getAccessCode(desc, exports[i]) == StateHelper.ACCESS_DISCOURAGED)
				return true;

		}

		return false;
	}

	public static boolean isOnClasspath(String fullyQualifiedName, IJavaProject project) {
		if (fullyQualifiedName.indexOf('$') != -1)
			fullyQualifiedName = fullyQualifiedName.replace('$', '.');
		try {
			IType type = project.findType(fullyQualifiedName);
			return type != null && type.exists();
		} catch (JavaModelException e) {
		}
		return false;
		/*try {
			Requestor requestor = new Requestor();
			new SearchEngine().searchAllTypeNames(
					fullyQualifiedName.substring(0, fullyQualifiedName.lastIndexOf('.')).toCharArray(), 
					SearchPattern.R_EXACT_MATCH|SearchPattern.R_CASE_SENSITIVE, 
					fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1).toCharArray(), 
					SearchPattern.R_EXACT_MATCH|SearchPattern.R_CASE_SENSITIVE, 
					IJavaSearchConstants.TYPE, 
					SearchEngine.createJavaSearchScope(new IJavaElement[] {project}), 
					requestor,
					IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, 
					new NullProgressMonitor());
			return requestor.hasMatches();
		} catch (JavaModelException e) {
		}
		return false;*/
	}

	public static IJavaSearchScope getSearchScope(IJavaProject project) {
		return SearchEngine.createJavaSearchScope(getNonJRERoots(project));
	}

	public static IJavaSearchScope getSearchScope(IProject project) {
		return getSearchScope(JavaCore.create(project));
	}

	public static IPackageFragmentRoot[] getNonJRERoots(IJavaProject project) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (!isJRELibrary(roots[i])) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
	}

	public static boolean isJRELibrary(IPackageFragmentRoot root) {
		try {
			IPath path = root.getRawClasspathEntry().getPath();
			if (path.equals(new Path(JavaRuntime.JRE_CONTAINER)) || path.equals(new Path(JavaRuntime.JRELIB_VARIABLE))) {
				return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	/**
	 * @param packageName - the name of the package
	 * @param pluginID - the id of the containing plug-in - can be null if <code>project</code> is not null
	 * @param project - if null will search for an external package fragment, otherwise will search in project
	 */
	public static IPackageFragment getPackageFragment(String packageName, String pluginID, IProject project) {
		if (project == null)
			return getExternalPackageFragment(packageName, pluginID);

		IJavaProject jp = JavaCore.create(project);
		if (jp != null)
			try {
				IPackageFragmentRoot[] roots = jp.getAllPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					IPackageFragment frag = roots[i].getPackageFragment(packageName);
					if (frag.exists()) {
						return frag;
					}
				}
			} catch (JavaModelException e) {
			}
		return null;
	}

	private static IPackageFragment getExternalPackageFragment(String packageName, String pluginID) {
		if (pluginID == null)
			return null;
		IPluginModelBase base = null;
		try {
			IPluginModelBase plugin = PluginRegistry.findModel(pluginID);
			if (plugin == null)
				return null;
			ImportPackageSpecification[] packages = plugin.getBundleDescription().getImportPackages();
			for (int i = 0; i < packages.length; i++)
				if (packages[i].getName().equals(packageName)) {
					ExportPackageDescription desc = (ExportPackageDescription) packages[i].getSupplier();
					if (desc != null)
						base = PluginRegistry.findModel(desc.getExporter().getSymbolicName());
					break;
				}
			if (base == null)
				return null;
			IResource res = base.getUnderlyingResource();
			if (res != null) {
				IJavaProject jp = JavaCore.create(res.getProject());
				if (jp != null)
					try {
						IPackageFragmentRoot[] roots = jp.getAllPackageFragmentRoots();
						for (int i = 0; i < roots.length; i++) {
							IPackageFragment frag = roots[i].getPackageFragment(packageName);
							if (frag.exists())
								return frag;
						}
					} catch (JavaModelException e) {
					}
			}
			IProject proj = PDECore.getWorkspace().getRoot().getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
			if (proj == null)
				return searchWorkspaceForPackage(packageName, base);
			IJavaProject jp = JavaCore.create(proj);
			IPath path = new Path(base.getInstallLocation());
			// if model is in jar form
			if (!path.toFile().isDirectory()) {
				IPackageFragmentRoot root = jp.findPackageFragmentRoot(path);
				if (root != null) {
					IPackageFragment frag = root.getPackageFragment(packageName);
					if (frag.exists())
						return frag;
				}
				// else model is in folder form, try to find model's libraries on filesystem
			} else {
				IPluginLibrary[] libs = base.getPluginBase().getLibraries();
				for (int i = 0; i < libs.length; i++) {
					if (IPluginLibrary.RESOURCE.equals(libs[i].getType()))
						continue;
					String libName = ClasspathUtilCore.expandLibraryName(libs[i].getName());
					IPackageFragmentRoot root = jp.findPackageFragmentRoot(path.append(libName));
					if (root != null) {
						IPackageFragment frag = root.getPackageFragment(packageName);
						if (frag.exists())
							return frag;
					}
				}
			}
		} catch (JavaModelException e) {
		}
		return searchWorkspaceForPackage(packageName, base);
	}

	private static IPackageFragment searchWorkspaceForPackage(String packageName, IPluginModelBase base) {
		IPluginLibrary[] libs = base.getPluginBase().getLibraries();
		ArrayList libPaths = new ArrayList();
		IPath path = new Path(base.getInstallLocation());
		if (libs.length == 0) {
			libPaths.add(path);
		}
		for (int i = 0; i < libs.length; i++) {
			if (IPluginLibrary.RESOURCE.equals(libs[i].getType()))
				continue;
			String libName = ClasspathUtilCore.expandLibraryName(libs[i].getName());
			libPaths.add(path.append(libName));
		}
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			try {
				if (!projects[i].hasNature(JavaCore.NATURE_ID) || !projects[i].isOpen())
					continue;
				IJavaProject jp = JavaCore.create(projects[i]);
				ListIterator li = libPaths.listIterator();
				while (li.hasNext()) {
					IPackageFragmentRoot root = jp.findPackageFragmentRoot((IPath) li.next());
					if (root != null) {
						IPackageFragment frag = root.getPackageFragment(packageName);
						if (frag.exists())
							return frag;
					}
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

	public static IPackageFragment[] getPackageFragments(IJavaProject jProject, Collection existingPackages, boolean allowJava) {
		HashMap map = getPackageFragmentsHash(jProject, existingPackages, allowJava);
		return (IPackageFragment[]) map.values().toArray(new IPackageFragment[map.size()]);
	}

	public static HashMap getPackageFragmentsHash(IJavaProject jProject, Collection existingPackages, boolean allowJava) {
		HashMap map = new HashMap();
		try {
			IPackageFragmentRoot[] roots = getRoots(jProject);
			for (int i = 0; i < roots.length; i++) {
				IJavaElement[] children = roots[i].getChildren();
				for (int j = 0; j < children.length; j++) {
					IPackageFragment fragment = (IPackageFragment) children[j];
					String name = fragment.getElementName();
					if (name.length() == 0)
						name = "."; //$NON-NLS-1$
					if ((fragment.hasChildren() || fragment.getNonJavaResources().length > 0) && !existingPackages.contains(name)) {
						if (!name.equals("java") || !name.startsWith("java.") || allowJava) //$NON-NLS-1$ //$NON-NLS-2$
							map.put(fragment.getElementName(), fragment);
					}
				}
			}
		} catch (JavaModelException e) {
		}
		return map;
	}

	private static IPackageFragmentRoot[] getRoots(IJavaProject jProject) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE || jProject.getProject().equals(roots[i].getCorrespondingResource()) || (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
	}

	public static String getJavaSourceLevel(IProject project) {
		return getJavaLevel(project, JavaCore.COMPILER_SOURCE);
	}

	public static String getJavaComplianceLevel(IProject project) {
		return getJavaLevel(project, JavaCore.COMPILER_COMPLIANCE);
	}

	/**
	 * Precedence order from high to low:  (1) Project specific option;
	 * (2) General preference option; (3) Default option; (4) Java 1.3
	 * @param project
	 * @param optionName
	 */
	public static String getJavaLevel(IProject project, String optionName) {
		// Returns the corresponding java project
		// No need to check for null, will return null		
		IJavaProject javaProject = JavaCore.create(project);
		String value = null;
		// Preferred to use the project
		if ((javaProject != null) && javaProject.exists()) {
			// Get the project specific option if one exists. Rolls up to the 
			// general preference option if no project specific option exists.
			value = javaProject.getOption(optionName, true);
			if (value != null) {
				return value;
			}
		}
		// Get the general preference option
		value = new PDEPreferencesManager(JavaCore.PLUGIN_ID).getString(optionName);
		if (value != null) {
			return value;
		}
		// Get the default option
		value = JavaCore.getOption(optionName);
		if (value != null) {
			return value;
		}
		// Return the default
		return JavaCore.VERSION_1_3;
	}

}
