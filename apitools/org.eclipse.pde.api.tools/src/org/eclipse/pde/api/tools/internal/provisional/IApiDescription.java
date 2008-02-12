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
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;


/**
 * Describes the API of an API component. Annotates types as public API,
 * for private use, etc.
 * 
 * @noimplement This interface is not to be implemented by clients
 * 
 * @since 1.0.0
 */
public interface IApiDescription {
	
	/**
	 * Sets the visibility for the specified element in the context of the specified component.
	 *  
	 * @param component the component the visibility applies to or <code>null</code> for all components
	 * @param element the element the visibility applies to 
	 * @param visibility element visibility. See {@linkplain VisibilityModifiers} for
	 * supported modifiers 
	 */
	public void setVisibility(String component, IElementDescriptor element, int visibility);
	
	/**
	 * Sets the restrictions for the specified element in the context of the specified component.
	 *  
	 * @param component the component the visibility applies to or <code>null</code> for all components
	 * @param element the element the restrictions apply to 
	 * @param restrictions the restrictions to place on the element. See {@linkplain RestrictionModifiers} for
	 * supported modifiers 
	 */
	public void setRestrictions(String component, IElementDescriptor element, int restrictions);
	
	/**
	 * Returns annotations for the specified element when referenced from the specified component. 
	 * <p>
	 * The parent package annotation will be returned if the requested {@link IElementDescriptor} 
	 * does not exist in the API description. 
	 * If no inherited annotations can be derived <code>null</code> is returned.
	 * </p>
	 * <p>
	 * If there is no component specific API for the specified element, the general
	 * annotations for the element are returned.
	 * </p>
	 * 
	 * @param component the component the element was referenced from or <code>null</code>
	 * @param element element to resolve API description for
	 * @return API annotations or <code>null</code> 
	 */
	public IApiAnnotations resolveAnnotations(String component, IElementDescriptor element);

	/**
	 * Traverses this description with the given visitor.
	 * 
	 * @param visitor description visitor
	 */
	public void accept(ApiDescriptionVisitor visitor);
	
	/**
	 * Removes the {@link IElementDescriptor} from this description
	 * 
	 * @param element the element to remove
	 * @return true if the element was removed, false otherwise
	 */
	public boolean removeElement(IElementDescriptor element);
	
	/**
	 * Returns true if this API description contains annotated elements, false otherwise.
	 *
	 * @return true if this API description contains annotated elements, false otherwise.
	 */
	public boolean containsAnnotatedElements();
}
