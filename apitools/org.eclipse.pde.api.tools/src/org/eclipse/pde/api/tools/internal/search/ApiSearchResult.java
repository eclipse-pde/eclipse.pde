/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchResult;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;

/**
 * Search result implementation.
 * 
 * @since 1.0
 */
public class ApiSearchResult implements IApiSearchResult {

	/**
	 * Search condition
	 */
	private IApiSearchCriteria fCondition;
	
	/**
	 * Matching references
	 */
	private IReference[] fReferences;
	
	/**
	 * Constructs a new search result.
	 * 
	 * @param condition
	 * @param references
	 */
	public ApiSearchResult(IApiSearchCriteria condition, IReference[] references) {
		fCondition = condition;
		fReferences = references;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchResult#getReferences()
	 */
	public IReference[] getReferences() {
		return fReferences;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchResult#getSearchCriteria()
	 */
	public IApiSearchCriteria getSearchCriteria() {
		return fCondition;
	}

}
