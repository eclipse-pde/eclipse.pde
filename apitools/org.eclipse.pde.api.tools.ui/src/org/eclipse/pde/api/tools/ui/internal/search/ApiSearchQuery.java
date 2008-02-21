/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

/**
 * An API search query for the search framework.
 * 
 * @since 1.0.0
 */
public class ApiSearchQuery implements ISearchQuery {
	
	/**
	 * Description
	 */
	private String fLabel;
	
	/**
	 * Scope to search
	 */
	private IApiSearchScope fScope;
	
	/**
	 * Search criteria
	 */
	private IApiSearchCriteria fCriteria;
	
	/**
	 * Search result
	 */
	private ApiSearchResult fResult;
	
	/**
	 * Constructs a search query.
	 * 
	 * @param label
	 * @param scope
	 * @param criteria
	 */
	public ApiSearchQuery(String label, IApiSearchScope scope, IApiSearchCriteria criteria) {
		fLabel = label;
		fScope = scope;
		fCriteria = criteria;
		fResult = new ApiSearchResult(label, this);
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
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return fLabel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		return fResult;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		IApiSearchEngine engine = Factory.newSearchEngine();
		try {
			IReference[] references = engine.search(fScope, new IApiSearchCriteria[]{fCriteria}, monitor);
			if (!monitor.isCanceled()) {
				fResult.notify(references);
			} else {
				return Status.CANCEL_STATUS;
			}
		} catch (CoreException e) {
			return e.getStatus();
		} finally {
			try {
				fScope.close();
			} catch (CoreException e) {
				ApiUIPlugin.log(e.getStatus());
			}
		}
		return Status.OK_STATUS;
	}

}
