/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.descriptors;


/**
 * Describes an element in an API component.
 * 
 * @since 1.0.0
 */
public interface IElementDescriptor {
	
	/**
	 * Constant representing a package descriptor.
	 */
	public static final int PACKAGE = 1;
	
	/**
	 * Constant representing a reference type descriptor.
	 */
	public static final int TYPE = 2;
	
	/**
	 * Constant representing a field descriptor.
	 */
	public static final int FIELD = 5;
	
	/**
	 * Constant representing a method descriptor.
	 */
	public static final int METHOD = 6;
	
	/**
	 * Constant representing an {@link IResource} descriptor
	 */
	public static final int RESOURCE = 7;
	
	/**
	 * Constant representing an {@link IApiComponent}
	 */
	public static final int COMPONENT = 8;
	
	/**
	 * Returns the parent of this element or <code>null</code> if none.
	 * 
	 * @return
	 */
	public IElementDescriptor getParent();
	
	/**
	 * Returns all parents of this element in a top-down path, including this
	 * element as the last element in the path.
	 * 
	 * @return path top-down path to this element
	 */
	public IElementDescriptor[] getPath();
	
	/**
	 * Returns one of the element type constants defined by this interface.
	 * 
	 * @return element type constant
	 */
	public int getElementType();

}
