/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.search;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;

/**
 * A default search requestor to use for API tools
 * 
 * @since 1.0.0
 */
public interface IApiSearchRequestor {

	/**
	 * Search mask that will cause the engine to consider
	 * API references when searching
	 * 
	 * @see #includesApi()
	 */
	public static final int INCLUDE_API = 0x0001;
	/**
	 * Search mask that will cause the engine to consider
	 * internal references when searching
	 * 
	 * @see #includesInternal()
	 */
	public static final int INCLUDE_INTERNAL = 0x0002;	
	
	/**
	 * Returns the {@link IApiScope} to be searched
	 * 
	 * @return the {@link IApiScope} to be searched
	 */
	public IApiScope getScope();
	
	/**
	 * Returns whether this requestor cares about the given {@link IApiComponent} or not.
	 * This allows the requestor to direct the {@link ApiSearchEngine} to ignore certain components.
	 * 
	 * @param component the component to examine
	 * @return true if this requestor cares about the given {@link IApiComponent}, false otherwise.
	 */
	public boolean acceptComponent(IApiComponent component);
	
	/**
	 * Returns whether this requestor cares about the given {@link IApiMember} or not.
	 * This allows the requestor to direct the {@link ApiSearchEngine} to ignore certain
	 * members.
	 * 
	 * @param member the member to examine
	 * @return true if this requestor cares about the given {@link IApiMember}, false otherwise
	 */
	public boolean acceptMember(IApiMember member);
	
	/**
	 * Returns whether the given {@link IReference} should be accepted
	 * by this requestor.
	 * 
	 * @param reference
	 * @return true if the reference should be accepted, false otherwise
	 */
	public boolean acceptReference(IReference reference);
	
	/**
	 * Returns the or'd listing of {@link IReference} kinds to look for.
	 * 
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.ReferenceModifiers for 
	 * a complete listing of reference kinds
	 * 
	 * @return the listing of {@link IReference} kinds to consider during the search this requestor
	 * is used for.
	 */
	public int getReferenceKinds();
	
	/**
	 * Returns true if the current search mask includes considering references to API
	 * elements.
	 * 
	 * @return true if references to API elements should be considered, false otherwise
	 */
	public boolean includesAPI();
	
	/**
	 * Returns true if the current search mask includes considering references to internal
	 * elements.
	 * 
	 * @return true if reference to internal elements should be considered, false otherwise
	 */
	public boolean includesInternal();
}
