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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;


public class DependencyExtentSearchOperation extends WorkspaceModifyOperation {
	
	private static final String KEY_DEPENDENCY = "DependencyExtent.singular";
	private static final String KEY_DEPENDENCIES = "DependencyExtent.plural";
	private static final String KEY_SEARCHING = "DependencyExtent.searching";

	IPluginImport object;
	IProject parentProject;
	IPluginBase[] models = new IPluginBase[0];
	IPackageFragment[] packageFragments = new IPackageFragment[0];
	DependencyExtentSearchResultCollector resultCollector;
	
	class SearchResultCollector implements IJavaSearchResultCollector {
	
		protected IProgressMonitor monitor;
		HashSet result = new HashSet();
		
		
		public SearchResultCollector(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		
		public void accept(
			IResource resource,
			int start,
			int end,
			IJavaElement enclosingElement,
			int accuracy)
			throws CoreException {
			if (accuracy == IJavaSearchConstants.EXACT_MATCH) {
				result.add(enclosingElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT));
			}
		}
	
		public void aboutToStart() {
		}
	
		public void done() {}
	
		public IProgressMonitor getProgressMonitor() {
			return monitor;
		}
		
		public IJavaElement[] getResult() {
			return (IJavaElement[])result.toArray(new IJavaElement[result.size()]);
		}
		
	}

	public DependencyExtentSearchOperation(IPluginImport object) {
		this.object = object;
		parentProject = object.getModel().getUnderlyingResource().getProject();
	}

	protected void execute(IProgressMonitor monitor)
		throws CoreException, InvocationTargetException, InterruptedException {
		resultCollector =
			new DependencyExtentSearchResultCollector(this, monitor);

		try {
			HashSet set = new HashSet();
			PluginJavaSearchUtil.collectAllPrerequisites(
				PDECore.getDefault().findPlugin(object.getId()),
				set);
			models = (IPluginBase[]) set.toArray(new IPluginBase[set.size()]);

			if (parentProject.hasNature(JavaCore.NATURE_ID))
				packageFragments = PluginJavaSearchUtil.collectPackageFragments(models,parentProject);

			monitor.setTaskName(PDEPlugin.getResourceString(KEY_SEARCHING));
			monitor.beginTask("",packageFragments.length + 1);
			resultCollector.searchStarted();
			
			findExtensionPoints(monitor);

			if (packageFragments.length > 0)
				doJavaSearch(monitor);

		} catch (JavaModelException e) {
			PDEPlugin.log(e.getStatus());
		
		} finally {
			resultCollector.done();
		}
	}

	private void findExtensionPoints(IProgressMonitor monitor) {
		HashSet ids = new HashSet();
		IPluginExtension[] extensions = object.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (ids.add(extensions[i].getPoint())) {
				IPluginExtensionPoint point =
					getExtensionPoint(extensions[i].getPoint());
				if (point != null) {
					resultCollector.accept(point);
				}
			}
		}
		monitor.worked(1);
	}
	
	private IPluginExtensionPoint getExtensionPoint(String targetId) {
		for (int i = 0; i < models.length; i++) {
			IPluginExtensionPoint[] extPoints = models[i].getExtensionPoints();
			for (int j = 0; j < extPoints.length; j++) {
				if (extPoints[j].getFullId().equals(targetId))
					return extPoints[j];
			}
		}
		return null;
	}
			
		
	private void doJavaSearch(IProgressMonitor monitor)
		throws JavaModelException {
		SearchEngine searchEngine = new SearchEngine();
		IJavaSearchScope scope = getSearchScope();

		for (int i = 0; i < packageFragments.length; i++) {
			IPackageFragment packageFragment = packageFragments[i];
			if (!packageFragment.hasSubpackages()) {
				SearchResultCollector collector =
					new SearchResultCollector(monitor);
				searchEngine.search(
					PDEPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(
						packageFragment.getElementName() + ".*",
						IJavaSearchConstants.TYPE,
						IJavaSearchConstants.REFERENCES,
						true),
					scope,
					collector);
				IJavaElement[] enclosingElements = collector.getResult();
				if (enclosingElements.length > 0) {
					searchForTypes(
						packageFragment,
						searchEngine,
						SearchEngine.createJavaSearchScope(enclosingElements),
						monitor);
				}
			} else {
				searchForTypes(packageFragment, searchEngine, scope, monitor);
			}
			monitor.worked(1);
		}
	}
	

	private void searchForTypes(
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
				SearchResultCollector collector =
					new SearchResultCollector(monitor);
				searchEngine.search(
					PDEPlugin.getWorkspace(),
					SearchEngine.createSearchPattern(
						types[j],
						IJavaSearchConstants.REFERENCES),
					scope,
					collector);
				if (collector.getResult().length > 0) {
					resultCollector.accept(types[j]);
				}
			}
		}
	}


	private IJavaSearchScope getSearchScope() throws JavaModelException {
		IPackageFragmentRoot[] roots = JavaCore.create(parentProject).getPackageFragmentRoots();
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
	
	public String getPluralLabel() {
		return object.getId() + " - {0} " + PDEPlugin.getResourceString(KEY_DEPENDENCIES);
	}
	
	public String getSingularLabel() {
		return object.getId() + " - 1 " + PDEPlugin.getResourceString(KEY_DEPENDENCY);
	}
	
	public IProject getProject() {
		return parentProject;
	}
	
	
}
