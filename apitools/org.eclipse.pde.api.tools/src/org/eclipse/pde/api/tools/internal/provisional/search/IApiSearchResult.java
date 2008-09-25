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
package org.eclipse.pde.api.tools.internal.provisional.search;

/**
 * References that match a specific search criteria.
 * 
 * @since 1.0
 */
public interface IApiSearchResult {
	
	/**
	 * Returns the search criteria for this result.
	 * 
	 * @return search criteria
	 */
	public IApiSearchCriteria getSearchCriteria();
	
	/**
	 * Returns the references that matched this result's search criteria.
	 * 
	 * @return matching references
	 */
	public IReference[] getReferences();

}
