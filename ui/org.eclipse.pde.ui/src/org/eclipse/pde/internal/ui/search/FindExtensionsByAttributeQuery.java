/*******************************************************************************
 *  Copyright (c) 2011, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sascha Becher <s.becher@qualitype.de> - bug 360894
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.internal.core.search.ISearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

/**
 * @author Sascha Becher
 */
public class FindExtensionsByAttributeQuery implements ISearchQuery {

	private SearchResult fSearchResult;

	private PluginSearchInput fSearchInput;

	public FindExtensionsByAttributeQuery(PluginSearchInput input) {
		fSearchInput = input;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		final AbstractTextSearchResult result = (AbstractTextSearchResult) getSearchResult();
		result.removeAll();
		ISearchResultCollector collector = new ISearchResultCollector() {
			public void accept(Object match) {
				if (match instanceof IPlugin) {
					IPlugin plugin = (IPlugin) match;
					result.addMatch(new AttributesMatch(plugin, fSearchInput.getSearchString()));
				}
			}
		};
		ExtensionElementSearchOperation op = new ExtensionElementSearchOperation(fSearchInput, collector);
		op.execute(monitor);
		monitor.done();
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return fSearchInput.getSearchString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	public boolean canRunInBackground() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		if (fSearchResult == null)
			fSearchResult = new SearchResult(this) {
				public int getMatchCount() {
					// returns overall count of plugins found when searching
					return getElements().length;
				}
			};
		return fSearchResult;
	}

}