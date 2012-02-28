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
package org.eclipse.pde.internal.ui.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.search.FindExtensionsByAttributeQuery;
import org.eclipse.pde.internal.ui.util.ExtensionsFilterUtil;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;

/**
 * Search in workspace plugins for occurences of either the current filter text or filter related attributes
 * using the ExtensionsPatternFilter search behaviour.
 * 
 * @author Sascha Becher
 */
public class SearchExtensionsAction extends Action {

	private FormFilteredTree fFilteredTree;
	private boolean fUseCurrentFilter;

	/**
	 * @param filteredTree the extensions section tree
	 * @param actionText description
	 * @param useCurrentFilter when <code>true</code> the search is performed with the current filtering applied to the tree, 
	 *        otherwise the filter text is generated from the current selection 
	 */
	public SearchExtensionsAction(FormFilteredTree filteredTree, String actionText, boolean useCurrentFilter) {
		setImageDescriptor(PDEPluginImages.DESC_SEARCH_EXTENSIONS);
		setDisabledImageDescriptor(PDEPluginImages.DESC_SEARCH_EXTENSIONS_DISABLED);
		setText(actionText);
		fFilteredTree = filteredTree;
		fUseCurrentFilter = useCurrentFilter;
	}

	public void run() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(createSearchQuery());
	}

	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		input.setSearchLimit(PluginSearchInput.LIMIT_ALL);
		input.setSearchString(getFilterText());
		input.setSearchScope(new PluginSearchScope(PluginSearchScope.SCOPE_WORKSPACE, PluginSearchScope.EXTERNAL_SCOPE_ALL, null));
		input.setCaseSensitive(false);
		return new FindExtensionsByAttributeQuery(input);
	}

	private String getFilterText() {
		if (fFilteredTree != null) {
			if (fUseCurrentFilter && fFilteredTree.isFiltered()) {
				return fFilteredTree.getFilterControl().getText();
			}
			return ExtensionsFilterUtil.getFilterRelatedPattern(getSelection());
		}
		return new String();
	}

	private IStructuredSelection getSelection() {
		if (fFilteredTree != null && fFilteredTree.getViewer() != null) {
			ISelection selection = fFilteredTree.getViewer().getSelection();
			if (selection != null && selection instanceof IStructuredSelection) {
				return (IStructuredSelection) selection;
			}
		}
		return new StructuredSelection();
	}

}