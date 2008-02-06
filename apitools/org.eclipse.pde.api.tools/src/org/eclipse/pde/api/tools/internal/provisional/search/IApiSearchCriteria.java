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
	 * Returns whether references within the same API component should be considered.
	 * Returns <code>false</code> if only references between API components should be
	 * considered, otherwise <code>true</code>.
	 *  
	 * @return whether references contained within a component should be considered 
	 */
	public boolean isConsiderComponentLocalReferences();
	
	/**
	 * Sets whether references within the same API component should be considered.
	 * Specify <code>false</code> when only references between API components should be
	 * considered, otherwise <code>true</code>.
	 * <p>
	 * When only references between API components are to be considered, the 
	 * <code>localRefs</code> parameter should be set to <code>false</code>.
	 * For example, when searching for implementors of an interface
	 * specified as <code>NO_IMPLMENENT</code>, one usually only wants to consider
	 * implementors external to the component defining the interface (i.e. the
	 * component defining the interface provides an expected/legal implementation
	 * of the interface). If you want to consider references within a component
	 * as well, <code>includeComponentLocalRefs</code> should be specified as <code>true</code>.
	 * </p>
	 *  
	 * @param localRefs whether references contained within a component should be considered 
	 */
	public void setConsiderComponentLocalReferences(boolean localRefs);
	
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
	public void addComponentRestriction(String componentId);
	
	/**
	 * Restricts matching to references referring to or contained within the
	 * specified elements in the specified components.
	 * 
	 * @param componentId API component identifier
	 * @param elements elements within the component
	 */
	public void addElementRestriction(String componentId, IElementDescriptor[] elements);
	
	/**
	 * Restricts matching to references that have a name matching the given
	 * regular expression and element type.
	 * 
	 * @param regEx regular expression
	 * @param elementType element type defined by {@link IElementDescriptor}
	 */
	public void addPatternRestriction(String regEx, int elementType);

	/**
	 * Sets the kinds of references and corresponding visibility restriction and
	 * API usage restrictions to consider.
	 * 
	 * @param referenceMask bit mask of the kinds of references to consider
	 *  as specified by {@link ReferenceModifiers}	
	 * @param visibilityMask bit mask of the visibilities to consider 
	 *  as specified by {@link VisibilityModifiers}
	 * @param restrictionMask bit mask of the API restrictions to consider
	 *  as specified by {@link RestrictionModifiers}
	 */
	public void setReferenceKinds(int referenceMask, int visibilityMask, int restrictionMask);
}
