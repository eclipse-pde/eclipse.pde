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
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Stores API problem filters for an API component.
 * 
 * @since 1.0.0
 */
public interface IApiFilterStore {

	/**
	 * Adds the given filter to this filter store.
	 * If the filter to add is <code>null</code> or already exists in the store
	 * no work is done.
	 * 
	 * @param filter filter to add to this store
	 */
	public void addFilter(IApiProblemFilter filter);
	
	/**
	 * Returns all filters contained in this filter store.
	 * 
	 * @return all filters
	 */
	public IApiProblemFilter[] getFilters();
	
	/**
	 * Removes the specified filter from this filter store.
	 * 
	 * @param filter the filter to remove
	 * @return true if the filter was removed, false otherwise
	 */
	public boolean removeFilter(IApiProblemFilter filter);
	
	/**
	 * Returns whether a problem contained in the specified element of the given type and
	 * kind is filtered based on all the filters in this store.
	 * 
	 * @param element element the problem pertains to
	 * @param kinds problem kinds 
	 * @return whether the problem is filtered
	 */
	public boolean isFiltered(IElementDescriptor element, String[] kinds);
}
