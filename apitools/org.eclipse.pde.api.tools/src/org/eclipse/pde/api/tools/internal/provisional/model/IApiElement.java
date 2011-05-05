/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.model;



/**
 * Describes an element that can appear in the API Tools model.
 * 
 * @since 1.0.0
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IApiElement {

	/**
	 * Constant representing an {@link org.eclipse.pde.api.tools.internal.provisional.IApiComponent}
	 */
	public int COMPONENT = 1;
	
	/**
	 * Constant representing an {@link IApiType}
	 */
	public int TYPE = 2;
	
	/**
	 * Constant representing an {@link org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer}
	 */
	public int API_TYPE_CONTAINER = 3;
	
	/**
	 * Constant representing an {@link IApiBaseline}
	 */
	public int BASELINE = 4;
	
	/**
	 * Constant representing an {@link IApiField}
	 */
	public int FIELD = 5;
	
	/**
	 * Constant representing an {@link IApiMethod}
	 */
	public int METHOD = 6;
	
	/**
	 * Constant representing an {@link org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot}
	 */
	public int API_TYPE_ROOT = 7;
	
	/**
	 * Returns the name of this element
	 *
	 * @return the element name
	 */
	public String getName();

	/**
	 * Returns this element's kind encoded as an integer.
	 *
	 * @return the kind of element; one of the constants declared in <code>IApiElement</code>
	 * @see IApiElement
	 */
	public int getType();
	
	/**
	 * Returns the immediate parent {@link IApiElement} of this element,
	 * or <code>null</code> if this element has no parent
	 * 
	 * @return the immediate parent of this element or <code>null</code>
	 */
	public IApiElement getParent();
	
	/**
	 * Returns the first ancestor of this API element that has the given type.
	 * Returns <code>null</code> if no such ancestor can be found.
	 *
	 * @param ancestorType the given type
	 * @return the first ancestor of this API element that has the given type, 
	 * or <code>null</code> if no such an ancestor can be found
	 */
	public IApiElement getAncestor(int ancestorType);	
	
	/**
	 * Returns the API component this type originated from or <code>null</code>
	 * if unknown.
	 * 
	 * @return API component this type originated from or <code>null</code>
	 */
	public IApiComponent getApiComponent();
}
