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
import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.search.ui.IActionGroupFactory;
import org.eclipse.search.ui.IGroupByKeyComputer;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.ui.actions.ActionGroup;

class JavaSearchCollector implements IJavaSearchResultCollector {
	private IProgressMonitor monitor;
	private ISearchResultView resultView;
	private JavaSearchOperation operation;
	
	class SearchActionGroupFactory implements IActionGroupFactory {
		public ActionGroup createActionGroup(ISearchResultView searchView) {
			return new SearchActionGroup();
		}
	}
	
	class SearchActionGroup extends ActionGroup {
	}
	
	class GroupByKeyComputer implements IGroupByKeyComputer {
		public Object computeGroupByKey(IMarker marker) {
			return marker;
		}
	}
	
	public JavaSearchCollector(JavaSearchOperation op, IProgressMonitor monitor) {
		this.operation = op;
		this.monitor = monitor;
	}
	
	public void aboutToStart() {
		resultView = SearchUI.getSearchResultView();
		resultView.searchStarted(
			new SearchActionGroupFactory(),
			operation.getSingularLabel(),
			operation.getPluralLabel(),
			null,
			"org.eclipse.pde.internal.ui.search.javaSearch",
			new DependencyExtentLabelProvider(),
			new SearchGoToAction(),
			new GroupByKeyComputer(),
			operation);
	}
	
	public void accept(
		IResource resource,
		int start,
		int end,
		IJavaElement enclosingElement,
		int accuracy)
		throws CoreException {
		if (accuracy == IJavaSearchConstants.EXACT_MATCH) {
			HashMap attributes= new HashMap(3);
			JavaCore.addJavaElementMarkerAttributes(attributes, enclosingElement);
			attributes.put(IMarker.CHAR_START, new Integer(Math.max(start, 0)));
			attributes.put(IMarker.CHAR_END, new Integer(Math.max(end, 0)));
			IMarker marker = resource.createMarker(SearchUI.SEARCH_MARKER);
			marker.setAttributes(attributes);
			resultView.addMatch(enclosingElement.getElementName(),enclosingElement,resource, marker);
		}
	}

	public void done() {
		if (resultView != null)
			resultView.searchFinished();
	}

	public IProgressMonitor getProgressMonitor() {
		return monitor;
	}

}