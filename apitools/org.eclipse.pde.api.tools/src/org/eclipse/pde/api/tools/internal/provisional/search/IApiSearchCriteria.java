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

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Describes the criteria for a search.
 * 
 * @since 1.0.0
 */
public interface IApiSearchCriteria {

	/**
	 * Returns a bit mask describing all kinds of references being searched for.
	 *  
	 * @return bit mask of {@link ReferenceModifiers}
	 */
	public int getReferenceKinds();
	
	/**
	 * Returns whether the given <b>unresolved</b> reference is a potential matches for this
	 * search criteria. The reference is unresolved. 
	 * 
	 * @param reference candidate search result
	 * @return whether the reference is a potential match
	 */
	public boolean isPotentialMatch(IReference reference);
	
	/**
	 * Returns whether the given reference matches this search criteria.
	 * References passed to this method have already passed the potential match
	 * test, and have since been resolved.
	 * 
	 * @param reference potential search result
	 * @return whether the reference matches the search criteria
	 */
	public boolean isMatch(IReference reference);

	/**
	 * Restricts matching to references referring to the specified component.
	 *  
	 * @param componentId API component identifier
	 */
	public void addReferencedComponentRestriction(String componentId);
	
	/**
	 * Restricts matching to references referring to or contained within the
	 * specified elements in the specified components.
	 * 
	 * @param componentId API component identifier
	 * @param elements elements within the component
	 */
	public void addReferencedElementRestriction(String componentId, IElementDescriptor[] elements);
	
	/**
	 * Restricts matching to references that have a name matching the given
	 * regular expression and element type.
	 * 
	 * @param regEx regular expression
	 * @param elementType element type defined by {@link IElementDescriptor}
	 */
	public void addReferencedPatternRestriction(String regEx, int elementType);

	/**
	 * Sets the kinds of references to consider. By default all reference kinds are
	 * considered.
	 * 
	 * @param referenceMask bit mask of the kinds of references from the original referencing
	 *  location to consider as specified by {@link ReferenceModifiers}	
	 */
	public void setReferenceKinds(int referenceMask);
	
	/**
	 * Sets the visibility and API use restrictions to consider on referenced elements.
	 * By default, all visibilities and restrictions are considered.
	 * 
	 * @param visibilityMask bit mask of the visibilities of the referenced location
	 * 	to consider as specified by {@link VisibilityModifiers}
	 * @param restrictionMask bit mask of the API restrictions of the referenced location
	 * 	to consider as specified by {@link RestrictionModifiers}
	 */
	public void setReferencedRestrictions(int visibilityMask, int restrictionMask);	
	
	/**
	 * Sets the Java visibility (access modifiers) of referencing locations to consider.
	 * For example, only consider references that stem from public or protected members/types.
	 * By default, all visibilities are considered.
	 * 
	 * @param modifiers modifiers as defined by {@link Flags}
	 */
	public void setSourceModifiers(int modifiers);

	/**
	 * Sets the visibility and API use restrictions to consider on source elements.
	 * By default, all visibilities and restrictions are considered.
	 * 
	 * @param visibilityMask bit mask of the visibilities of the source location
	 * 	to consider as specified by {@link VisibilityModifiers}
	 * @param restrictionMask bit mask of the API restrictions of the source location
	 * 	to consider as specified by {@link RestrictionModifiers}
	 */
	public void setSourceRestrictions(int visibilityMask, int restrictionMask);	
	
	/**
	 * Filter references originating from the specified component.
	 * 
	 * @param componentId API component identifier
	 */
	public void addSourceFilter(String componentId);
	
	/**
	 * Sets arbitrary user data on this search criteria.
	 * 
	 * @param object user data or <code>null</code>
	 */
	public void setUserData(Object object);
	
	/**
	 * Returns the user data set on this search criteria, or <code>null</code>.
	 * 
	 * @return user data or <code>null</code>
	 */
	public Object getUserData();
}
