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

import java.io.File;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.search.IPluginSearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchOperation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.IGroupByKeyComputer;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;


public class PluginSearchResultCollector
	implements IPluginSearchResultCollector {
		
	class GroupByKeyComputer implements IGroupByKeyComputer {
		public Object computeGroupByKey(IMarker marker) {
			return marker;
		}

	}
		
	private static final String KEY_MATCH = "Search.singleMatch";
	private static final String KEY_MATCHES = "Search.multipleMatches";

	private PluginSearchUIOperation operation;
	private ISearchResultView resultView;
	private static IPluginObject currentMatch;
	private IProgressMonitor monitor;
	private int numMatches = 0;
	
	private static final String pageID =
		"org.eclipse.pde.internal.ui.search.SearchPage";


	public void setOperation(PluginSearchOperation operation) {
		this.operation = (PluginSearchUIOperation) operation;
	}

	public PluginSearchOperation getOperation() {
		return operation;
	}

	public void accept(IPluginObject match) {
		try {
			currentMatch = match;

			IResource resource = match.getModel().getUnderlyingResource();
			if (resource == null) {
				resource = PDEPlugin.getWorkspace().getRoot();
			}

			IMarker marker = resource.createMarker(SearchUI.SEARCH_MARKER);
			if (match instanceof ISourceObject) {
				marker.setAttribute(
					IMarker.LINE_NUMBER,
					((ISourceObject) match).getStartLine());
			}
			if (match.getModel().getUnderlyingResource()==null)
				annotateExternalMarker(marker, match);
			
			resultView.addMatch(null, match, resource, marker);
			
			numMatches += 1;
			
			String text =
				(numMatches > 1)
					? PDEPlugin.getResourceString(KEY_MATCHES)
					: PDEPlugin.getResourceString(KEY_MATCH);

			monitor.subTask(numMatches + " " + text);
			
		} catch (CoreException e) {
		}
	}
	
	private void annotateExternalMarker(IMarker marker, IPluginObject match) throws CoreException {
		IPluginModelBase model = match.getModel();
		String path = model.getInstallLocation();
		String manifest =
				model.isFragmentModel()
			? "fragment.xml"
			: "plugin.xml";
		String fileName = path + File.separator + manifest;
		marker.setAttribute(IPDEUIConstants.MARKER_SYSTEM_FILE_PATH, fileName);
	}

	public void done() {
		if (resultView != null)	
			resultView.searchFinished();
	}

	public void searchStarted() {
		resultView = SearchUI.getSearchResultView();
		resultView.searchStarted(
			new PluginSearchActionGroupFactory(),
			operation.getSingularLabel(),
			operation.getPluralLabel(),
			null,
			pageID,
			new PluginSearchLabelProvider(),
			new SearchGoToAction(),
			new GroupByKeyComputer(),
			operation);
	}
	
	public void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

}
