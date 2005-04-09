/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.pde.internal.ui.*;

public class MainMethodSearchEngine{
	
	private class MethodCollector extends SearchRequestor {
		private List fResult;
		private int fStyle;

		public MethodCollector(int style) {
			fResult = new ArrayList(200);
			fStyle= style;
		}

		public List getResult() {
			return fResult;
		}

		private boolean considerExternalJars() {
			return (fStyle & IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS) != 0;
		}
				
		private boolean considerBinaries() {
			return (fStyle & IJavaElementSearchConstants.CONSIDER_BINARIES) != 0;
		}		

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
		 */
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			Object enclosingElement = match.getElement();
			if (enclosingElement instanceof IMethod) { // defensive code
				try {
					IMethod curr= (IMethod) enclosingElement;
					if (curr.isMainMethod()) {
						if (!considerExternalJars()) {
							IPackageFragmentRoot root= getPackageFragmentRoot(curr);
							if (root == null || root.isArchive()) {
								return;
							}
						}
						if (!considerBinaries() && curr.isBinary()) {
							return;
						}
						IType declaringType = curr.getDeclaringType();
						fResult.add(declaringType);
					}
				} catch (JavaModelException e) {
					PDEPlugin.log(e.getStatus());
				}
			}
		}
	}

	/**
	 * Searches for all main methods in the given scope.
	 * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
	 * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
	 * 
	 * @param pm progress monitor
	 * @param scope search scope
	 * @param style search style
	 * @param includeSubtypes whether to consider types that inherit a main method
	 */	
	public IType[] searchMainMethods(IProgressMonitor pm, IJavaSearchScope scope, int style, boolean includeSubtypes) {
		pm.beginTask(PDEUIMessages.MainMethodSearchEngine_search, 100);  //$NON-NLS-1$
		int searchTicks = 100;
		if (includeSubtypes) {
			searchTicks = 25;
		}
		
		SearchPattern pattern = SearchPattern.createPattern("main(String[]) void", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE); //$NON-NLS-1$
		SearchParticipant[] participants = new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		MethodCollector collector = new MethodCollector(style);
		IProgressMonitor searchMonitor = new SubProgressMonitor(pm, searchTicks);
		try {
			new SearchEngine().search(pattern, participants, scope, collector, searchMonitor);
		} catch (CoreException ce) {
			PDEPlugin.log(ce);
		}

		List result = collector.getResult();
		if (includeSubtypes) {
			IProgressMonitor subtypesMonitor = new SubProgressMonitor(pm, 75);
			subtypesMonitor.beginTask(PDEUIMessages.MainMethodSearchEngine_search, result.size()); //$NON-NLS-1$
			Set set = addSubtypes(result, subtypesMonitor, scope);
			return (IType[]) set.toArray(new IType[set.size()]);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private Set addSubtypes(List types, IProgressMonitor monitor, IJavaSearchScope scope) {
		Iterator iterator = types.iterator();
		Set result = new HashSet(types.size());
		while (iterator.hasNext()) {
			IType type = (IType) iterator.next();
			if (result.add(type)) {
				ITypeHierarchy hierarchy = null;
				try {
					hierarchy = type.newTypeHierarchy(monitor);
					IType[] subtypes = hierarchy.getAllSubtypes(type);
					for (int i = 0; i < subtypes.length; i++) {
						IType t = subtypes[i];
						if (scope.encloses(t)) {
							result.add(t);
						}
					}				
				} catch (JavaModelException e) {
					PDEPlugin.log(e);
				}
			}
			monitor.worked(1);
		}
		return result;
	}
	
	
	/**
	 * Returns the package fragment root of <code>IJavaElement</code>. If the given
	 * element is already a package fragment root, the element itself is returned.
	 */
	public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
		return (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
	}	
	
	/**
	 * Searches for all main methods in the given scope.
	 * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
	 * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
	 * 
	 * @param includeSubtypes whether to consider types that inherit a main method
	 */
	public IType[] searchMainMethods(IRunnableContext context, final IJavaSearchScope scope, final int style, final boolean includeSubtypes) throws InvocationTargetException, InterruptedException  {
		int allFlags=  IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS | IJavaElementSearchConstants.CONSIDER_BINARIES;
		Assert.isTrue((style | allFlags) == allFlags);
		
		final IType[][] res= new IType[1][];
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				res[0]= searchMainMethods(pm, scope, style, includeSubtypes);
			}
		};
		context.run(true, true, runnable);
		
		return res[0];
	}
			
}
