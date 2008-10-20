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
package org.eclipse.pde.api.tools.internal.provisional.model;


/**
 * Describes an element that can appear in the API tooling model.
 * <p>
 * Methods annotated as "handle-only" do not require underlying elements to exist.
 * Methods that require underlying elements to exist throw
 * a <code>CoreException</code> when an underlying element is missing.
 * </p>
 * 
 * @since 1.0.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IApiElement {

	/**
	 * Constant representing a package.
	 */
	public int PACKAGE = 1;
	
	/**
	 * Constant representing a reference type.
	 */
	public int TYPE = 2;		
	
	/**
	 * Constant representing a package container 
	 */
	public int PACKAGE_ROOT = 3;
	
	/**
	 * Constant representing an API baseline
	 */
	public int BASELINE = 4;
	
	/**
	 * Constant representing a field.
	 */
	public int FIELD = 5;	
	
	/**
	 * Constant representing a method.
	 */
	public int METHOD = 6;	
	
	/**
	 * Constant representing an {@link IResource}
	 */
	public int RESOURCE = 7;
	
	/**
	 * Constant representing an API component
	 */
	public int COMPONENT = 8;
	
	/**
	 * Returns the name of this element. This is a handle-only method.
	 *
	 * @return the element name
	 */
	public String getName();

	/**
	 * Returns this element's kind encoded as an integer.
	 * This is a handle-only method.
	 *
	 * @return the kind of element; one of the constants declared in <code>IApiElement</code>
	 * @see IApiElement
	 */
	public int getType();
	
	/**
	 * Returns the immediate parent {@link IApiElement} of this element,
	 * or <code>null</code> if this parent has no parent.
	 * This is a handle-only method.
	 * 
	 * @return the immediate parent of this element or <code>null</code>
	 */
	public IApiElement getParent();
	
	/**
	 * Returns the first ancestor of this API element that has the given type.
	 * Returns <code>null</code> if no such ancestor can be found.
	 * This is a handle-only method.
	 *
	 * @param ancestorType the given type
	 * @return the first ancestor of this API element that has the given type, null if no such an ancestor can be found
	 */
	public IApiElement getAncestor(int ancestorType);	
	
	/**
	 * Returns a signature-like description of the element. Can be used to persist and restore 
	 * {@link IApiElement}s.
	 * 
	 * @return a signature-like description of this element
	 */
	public String getDescriptor();
}
