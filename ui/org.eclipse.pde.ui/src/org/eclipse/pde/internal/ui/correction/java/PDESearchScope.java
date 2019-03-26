/*******************************************************************************
 *  Copyright (c) 2018 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import java.util.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ClasspathUtilCore.ClasspathLibrary;

class PDESearchScope implements IJavaSearchScope {

	private final IPath[] fEnclosingPaths;

	/**
	 * Returns a search scope containing all workspace projects and the target
	 * platform content.
	 */
	public static IJavaSearchScope create() {
		IPath[] workspacePaths = SearchEngine.createWorkspaceScope().enclosingProjectsAndJars();

		LinkedHashSet<IPath> allPaths = new LinkedHashSet<>(Arrays.asList(workspacePaths));

		for (IPluginModelBase model : PluginRegistry.getActiveModels()) {
			addPaths(allPaths, model);
		}

		return new PDESearchScope(allPaths.toArray(new IPath[0]));
	}

	private static void addPaths(Collection<IPath> allPaths, IPluginModelBase model) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null && resource.isAccessible()) {
			return; // already added by workspace scope
		}

		for (ClasspathLibrary library : ClasspathUtilCore.collectLibraries(model)) {
			allPaths.add(library.getPath());
		}
	}

	private PDESearchScope(IPath[] paths) {
		fEnclosingPaths = paths;
	}

	@Override
	public boolean encloses(String resourcePath) {
		return true;
	}

	@Override
	public boolean encloses(IJavaElement element) {
		return true;
	}

	@Override
	public IPath[] enclosingProjectsAndJars() {
		return fEnclosingPaths;
	}

	@Deprecated
	@Override
	public boolean includesBinaries() {
		return true;
	}

	@Deprecated
	@Override
	public boolean includesClasspaths() {
		return true;
	}

	@Deprecated
	@Override
	public void setIncludesBinaries(boolean includesBinaries) {
	}

	@Deprecated
	@Override
	public void setIncludesClasspaths(boolean includesClasspaths) {
	}

}
