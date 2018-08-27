/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.search.*;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

public class PluginSearchQuery implements ISearchQuery {

	private SearchResult fSearchResult;

	private PluginSearchInput fSearchInput;

	public PluginSearchQuery(PluginSearchInput input) {
		fSearchInput = input;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		final AbstractTextSearchResult result = (AbstractTextSearchResult) getSearchResult();
		result.removeAll();
		ISearchResultCollector collector = match -> {
			if (match instanceof ISourceObject) {
				ISourceObject object1 = (ISourceObject) match;
				result.addMatch(new Match(match, Match.UNIT_LINE, object1.getStartLine() - 1, 1));
			}
			if (match instanceof IFeaturePlugin) {
				IFeaturePlugin object2 = (IFeaturePlugin) match;
				result.addMatch(new Match(object2, Match.UNIT_LINE, -1, 1));
			}
		};
		PluginSearchOperation op = new PluginSearchOperation(fSearchInput, collector);
		op.execute(monitor);
		monitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public String getLabel() {
		return fSearchInput.getSearchString();
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public ISearchResult getSearchResult() {
		if (fSearchResult == null)
			fSearchResult = new SearchResult(this);
		return fSearchResult;
	}

}
