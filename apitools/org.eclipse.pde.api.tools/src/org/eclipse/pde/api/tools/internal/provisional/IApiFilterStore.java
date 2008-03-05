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

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

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
	 * @param filters filters to add to this store
	 */
	public void addFilters(IApiProblemFilter[] filters);
	
	/**
	 * Creates an {@link IApiProblemFilter} for the given {@link IApiProblem} and adds that 
	 * filter to the store.
	 * If the problem creates a filter that already exists in the store, or the problem is <code>null</code> 
	 * no work is done.
	 * 
	 * @param problems problems to create a filter for and add to this store
	 */
	public void addFilters(IApiProblem[] problems);
	
	/**
	 * Returns all filters for the specified resource
	 * 
	 * @param resource the resource to get filters for
	 * @return all filters for the given resource or an empty array, never <code>null</code>
	 */
	public IApiProblemFilter[] getFilters(IResource resource);
	
	/**
	 * Returns all of the resources that have filters contained in this
	 * filter store. If there are no resources with filters or the store has not
	 * been initialized yet an empty array is returned, never <code>null</code>.
	 * 
	 * The returned resources are not guaranteed to exist.
	 * 
	 * @return the resources that have filters
	 */
	public IResource[] getResources();
	
	/**
	 * Removes the specified filter from this filter store.
	 * 
	 * @param filters the filters to remove
	 * @return true if all of the filters were removed, false otherwise
	 */
	public boolean removeFilters(IApiProblemFilter[] filters);
	
	/**
	 * Returns whether a problem contained in the specified element of the given type and
	 * kind is filtered based on all the filters in this store.
	 * 
	 * @param problem the problem we want to know is filtered or not
	 * @return whether the problem is filtered
	 */
	public boolean isFiltered(IApiProblem problem);
	
	/**
	 * Disposes the filter store.
	 */
	public void dispose();
}
