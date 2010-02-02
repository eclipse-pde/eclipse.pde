/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;


/**
 * Describes the API of an API component. Annotates types as public API,
 * for private use, etc.
 * 
 * @noimplement This interface is not to be implemented by clients
 * 
 * @since 1.0
 */
public interface IApiDescription {
	
	/**
	 * Status code indicating an element was not found when attempting to
	 * set its visibility or usage restrictions.
	 */
	public static final int ELEMENT_NOT_FOUND = 100;
	
	/**
	 * Sets the visibility for the specified element in the context of the specified component
	 * and returns a status describing whether the operation succeeded.
	 * @param element the element the visibility applies to 
	 * @param visibility element visibility. See {@linkplain VisibilityModifiers} for
	 * supported modifiers
	 * 
	 * @return status of the operation 
	 */
	public IStatus setVisibility(IElementDescriptor element, int visibility);
		
	/**
	 * Sets the restrictions for the specified element and returns a status describing whether the operation
	 * succeeded.
	 * 
	 * @param element the element the restrictions apply to 
	 * @param restrictions the restrictions to place on the element. See {@linkplain RestrictionModifiers} for
	 * supported modifiers 
	 *  
	 * @return status of the operation
	 */
	public IStatus setRestrictions(IElementDescriptor element, int restrictions);
	
	/**
	 * Sets the superclass for the specified element and returns a status describing whether the operation
	 * succeeded.
	 * 
	 * @param element the element the restrictions apply to 
	 * @param superclass the superclass name of the element
	 *  
	 * @return status of the operation
	 */
	public IStatus setSuperclass(IElementDescriptor element, String superclass);

	/**
	 * Sets the superclass for the specified element and returns a status describing whether the operation
	 * succeeded.
	 * 
	 * @param element the element the restrictions apply to 
	 * @param superclass the superclass name of the element
	 *  
	 * @return status of the operation
	 */
	public IStatus setSuperinterfaces(IElementDescriptor element, String superinterfaces);
	/**
	 * Sets the interface flag for the specified element and returns a status describing whether the operation
	 * succeeded.
	 * 
	 * @param element the element the restrictions apply to 
	 * @param interfaceFlag the interface flag of the element
	 *  
	 * @return status of the operation
	 */
	public IStatus setInterface(IElementDescriptor element, boolean interfaceFlag);

	/**
	 * Sets the visibility for the specified element in the context of the specified component
	 * and returns a status describing whether the operation succeeded.
	 * @param element the element the visibility applies to 
	 * @param visibility element visibility. See {@linkplain VisibilityModifiers} for
	 * supported modifiers
	 * 
	 * @return status of the operation 
	 */
	public IStatus setAddedProfile(IElementDescriptor element, int addedProfile);
		
	/**
	 * Sets the restrictions for the specified element and returns a status describing whether the operation
	 * succeeded.
	 * 
	 * @param element the element the restrictions apply to 
	 * @param restrictions the restrictions to place on the element. See {@linkplain RestrictionModifiers} for
	 * supported modifiers 
	 *  
	 * @return status of the operation
	 */
	public IStatus setRemovedProfile(IElementDescriptor element, int removedProfile);

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
	 * @param element element to resolve API description for
	 * 
	 * @return API annotations or <code>null</code> 
	 */
	public IApiAnnotations resolveAnnotations(IElementDescriptor element);

	/**
	 * Returns the access level the given element has for the given package. If there is no 
	 * special access for the given element <code>null</code> is returned.
	 * 
	 * @param element the element to resolve access for
	 * @param pelement the package being accessed by the given element
	 * 
	 * @return API access the given element has to the given package or <code>null</code> if no special access
	 * has been defined
	 */
	public IApiAccess resolveAccessLevel(IElementDescriptor element, IPackageDescriptor pelement);
	
	/**
	 * Sets the access level that the given element has to the given package
	 * 
	 * @param element the element that has access to the given package
	 * @param pelement the package that the given element will have the given access to
	 * @param access the desired access level to the given package from the given element
	 */
	public void setAccessLevel(IElementDescriptor element, IPackageDescriptor pelement, int access);
	
	/**
	 * Traverses this description with the given visitor.
	 * 
	 * @param visitor description visitor
	 * @param monitor
	 */
	public void accept(ApiDescriptionVisitor visitor, IProgressMonitor monitor);
	
	/**
	 * Traverses this given element contained in this description, if present.
	 * 
	 * @param visitor visitor
	 * @param element element to visit
	 * @param monitor progress monitor or <code>null</code>
	 * @return whether the element was visited - <code>true</code> if present and
	 *  <code>false</code> if not present
	 */
	public boolean accept(ApiDescriptionVisitor visitor, IElementDescriptor element, IProgressMonitor monitor);
	
}
