package org.eclipse.pde.internal.ui.search;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;

public class PluginJavaSearchUtil {

	public static void collectAllPrerequisites(IPlugin plugin, HashSet set) {
		if (!set.add(plugin))
			return;

		if (plugin.getModel() instanceof WorkspacePluginModelBase) {
			IFragment[] fragments =
				PDECore.getDefault().getWorkspaceModelManager().getFragmentsFor(
					plugin.getId(),
					plugin.getVersion());
			for (int i = 0; i < fragments.length; i++) {
				set.add(fragments[i]);
			}
		}

		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isReexported()) {
				IPlugin child = PDECore.getDefault().findPlugin(imports[i].getId());
				if (child != null)
					collectAllPrerequisites(child, set);
			}
		}
	}

	public static IPackageFragment[] collectPackageFragments(
		IPluginBase[] models,
		IProject parentProject)
		throws JavaModelException {
		ArrayList result = new ArrayList();
		IPackageFragmentRoot[] roots =
			JavaCore.create(parentProject).getAllPackageFragmentRoots();

		for (int i = 0; i < models.length; i++) {
			IPluginBase preReq = models[i];
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
			PDECore.getDefault().getExternalModelManager().getFragmentsFor(
				plugin.getId(),
				plugin.getVersion());

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

}
