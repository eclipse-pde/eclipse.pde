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

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.internal.ui.PDEPlugin;


class JavaSearchOperation implements IWorkspaceRunnable, IRunnableWithProgress {
	IJavaElement element;
	IProject parentProject;
	private static final String KEY_MATCH = "Search.singleMatch";
	private static final String KEY_MATCHES = "Search.multipleMatches";
	
	public JavaSearchOperation(IJavaElement element, IProject parentProject) {
		this.element = element;
		this.parentProject = parentProject;
	}

	public void run(IProgressMonitor monitor) {
		doJavaSearch(monitor);
	}
	
	private void doJavaSearch(IProgressMonitor monitor) {
		try {
			SearchEngine searchEngine = new SearchEngine();
			searchEngine.search(
				PDEPlugin.getWorkspace(),
				element,
				IJavaSearchConstants.REFERENCES,
				getSearchScope(),
				new JavaSearchCollector(this, monitor));
		} catch (JavaModelException e) {
		}
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
	
	public String getPluralLabel() {
		return element.getElementName() + " - {0} " + PDEPlugin.getResourceString(KEY_MATCHES);
	}

	public String getSingularLabel() {
		return element.getElementName() + " - 1 " + PDEPlugin.getResourceString(KEY_MATCH);
	}

}