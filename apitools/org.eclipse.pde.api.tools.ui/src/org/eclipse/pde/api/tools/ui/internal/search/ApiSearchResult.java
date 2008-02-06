/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.search;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;

/**
 * API search result for the search framework.
 * 
 * @since 1.0.0
 */
public class ApiSearchResult implements ISearchResult {
	
	/**
	 * Search result listeners
	 */
	private ListenerList fListeners = new ListenerList();
	/**
	 * Associated query.
	 */
	private ApiSearchQuery fQuery = null;
	
	private String fLabel = null;
	
	/**
	 * Constructs a new API search result with the given label based on the given
	 * underlying query.
	 * 
	 * @param label
	 * @param query
	 */
	public ApiSearchResult(String label, ApiSearchQuery query) {
		fQuery = query;
		fLabel = label;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#addListener(org.eclipse.search.ui.ISearchResultListener)
	 */
	public void addListener(ISearchResultListener l) {
		fListeners.add(l);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_OBJ_API_SEARCH);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	public String getLabel() {
		return fLabel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return fQuery;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	public String getTooltip() {
		// TODO:
		return getLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#removeListener(org.eclipse.search.ui.ISearchResultListener)
	 */
	public void removeListener(ISearchResultListener l) {
		fListeners.remove(l);
	}
	
	void notify(IReference[] references) {
		// TODO:
		System.out.println(references.length + " search matches");
		for (int i = 0; i < references.length; i++) {
			System.out.println(references[i]);
		}
	}

}
