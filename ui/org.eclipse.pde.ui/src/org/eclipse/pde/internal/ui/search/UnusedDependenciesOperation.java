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
package org.eclipse.pde.internal.ui.search;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class UnusedDependenciesOperation implements IRunnableWithProgress {
	private IPluginModelBase model;
	private IProject parentProject;
	HashSet unused = new HashSet();

	class SearchResultCollector implements IJavaSearchResultCollector {
		int count = 0;

		public void accept(
			IResource resource,
			int start,
			int end,
			IJavaElement enclosingElement,
			int accuracy)
			throws CoreException {
			if (accuracy == IJavaSearchConstants.EXACT_MATCH) {
				count += 1;
			}
		}

		public void aboutToStart() {}

		public void done() {}

		public IProgressMonitor getProgressMonitor() {
			return null;
		}

		public boolean isEmpty() {
			return count == 0;
		}
	}

	public UnusedDependenciesOperation(IPluginModelBase model) {
		this.model = model;
		this.parentProject = model.getUnderlyingResource().getProject();
	}

	public void run(IProgressMonitor monitor) {
		try {
			IPluginImport[] imports = model.getPluginBase().getImports();
			if (imports.length == 0)
				return;

			monitor.setTaskName(
				PDEPlugin.getResourceString("UnusedDependencies.analyze")); //$NON-NLS-1$
			monitor.beginTask("", imports.length); //$NON-NLS-1$
			for (int i = 0; i < imports.length; i++) {
				if (!isUsed(imports[i], new SubProgressMonitor(monitor, 1)))
					unused.add(imports[i]);
				monitor.setTaskName(
					PDEPlugin.getResourceString("UnusedDependencies.analyze") //$NON-NLS-1$
						+ unused.size()
						+ " " //$NON-NLS-1$
						+ PDEPlugin.getResourceString("UnusedDependencies.unused") //$NON-NLS-1$
						+ " " //$NON-NLS-1$
						+ (unused.size() == 1
							? PDEPlugin.getResourceString("DependencyExtent.singular") //$NON-NLS-1$
							: PDEPlugin.getResourceString("DependencyExtent.plural")) //$NON-NLS-1$
						+ " " //$NON-NLS-1$
						+ PDEPlugin.getResourceString("DependencyExtent.found")); //$NON-NLS-1$
			}
		} finally {
			monitor.done();
		}

	}

	private boolean isUsed(IPluginImport dependency, IProgressMonitor monitor) {
		try {
			HashSet set = new HashSet();
			PluginJavaSearchUtil.collectAllPrerequisites(
				PDECore.getDefault().findPlugin(dependency.getId()),
				set);
			IPluginBase[] models =
				(IPluginBase[]) set.toArray(new IPluginBase[set.size()]);

			IPackageFragment[] packageFragments = new IPackageFragment[0];
			if (parentProject.hasNature(JavaCore.NATURE_ID))
				packageFragments =
					PluginJavaSearchUtil.collectPackageFragments(models, parentProject);

			monitor.beginTask("", packageFragments.length + 1); //$NON-NLS-1$

			if (providesExtensionPoint(models))
				return true;
			monitor.worked(1);
			
			if (packageFragments.length > 0)
				return doJavaSearch(packageFragments, new SubProgressMonitor(monitor, packageFragments.length));

		} catch (JavaModelException e) {
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
		return false;
	}

	public IPluginImport[] getUnusedDependencies() {
		return (IPluginImport[]) unused.toArray(new IPluginImport[unused.size()]);
	}

	private boolean providesExtensionPoint(IPluginBase[] models) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			for (int j = 0; j < models.length; j++) {
				if (providesExtensionPoint(models[j], extensions[i].getPoint()))
					return true;
			}
		}
		return false;
	}
	
	private boolean providesExtensionPoint(IPluginBase model, String targetID) {
		IPluginExtensionPoint[] extPoints = model.getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			if (extPoints[i].getFullId().equals(targetID))
				return true;
		}
		return false;
	}

	private boolean doJavaSearch(
		IPackageFragment[] packageFragments,
		IProgressMonitor monitor)
		throws JavaModelException {
		SearchEngine searchEngine = new SearchEngine();
		IJavaSearchScope scope = getSearchScope();

		for (int i = 0; i < packageFragments.length; i++) {
			IPackageFragment packageFragment = packageFragments[i];
			boolean used = false;
			if (!packageFragment.hasSubpackages()) {
				SearchResultCollector collector = new SearchResultCollector();
				searchEngine.search(
					PDEPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(
						packageFragment.getElementName() + ".*", //$NON-NLS-1$
						IJavaSearchConstants.TYPE,
						IJavaSearchConstants.REFERENCES,
						true),
					scope,
					collector);
				used = !collector.isEmpty();
			} else {
				used = searchForTypes(packageFragment, searchEngine, scope, monitor);
			}
			monitor.worked(1);
			if (used)
				return true;
		}
		return false;
	}

	private boolean searchForTypes(
		IPackageFragment fragment,
		SearchEngine searchEngine,
		IJavaSearchScope scope,
		IProgressMonitor monitor)
		throws JavaModelException {
		IJavaElement[] children = fragment.getChildren();
		for (int i = 0; i < children.length; i++) {
			IJavaElement child = children[i];
			IType[] types = new IType[0];
			if (child instanceof IClassFile)
				types = new IType[] {((IClassFile) child).getType()};
			else if (child instanceof ICompilationUnit)
				types = ((ICompilationUnit) child).getAllTypes();

			for (int j = 0; j < types.length; j++) {
				SearchResultCollector collector = new SearchResultCollector();
				searchEngine.search(
					PDEPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(
						types[j],
						IJavaSearchConstants.REFERENCES),
					scope,
					collector);
				if (!collector.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private IJavaSearchScope getSearchScope() throws JavaModelException {
		IPackageFragmentRoot[] roots =
			JavaCore.create(parentProject).getPackageFragmentRoots();
		ArrayList filteredRoots = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getResource() != null
				&& roots[i].getResource().getProject().equals(parentProject)) {
				filteredRoots.add(roots[i]);
			}
		}
		return SearchEngine.createJavaSearchScope(
			(IJavaElement[]) filteredRoots.toArray(
				new IJavaElement[filteredRoots.size()]));
	}

}
