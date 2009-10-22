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
package org.eclipse.pde.api.tools.internal.provisional.model;

import org.eclipse.core.runtime.CoreException;

/**
 * Handle to the structure of a class, interface, annotation, or enum.
 * 
 * @since 1.0.0
 */
public interface IApiTypeRoot extends IApiElement {
	
	/**
	 * Returns the fully qualified name of the type this storage represents.
	 * Package names are dot separated and type names are '$'-separated.
	 * 
	 * @return fully qualified type name
	 */
	public String getTypeName();
	
	/**
	 * Returns the structure contained in this type storage. I.e. access
	 * to the methods, fields, and member types in the associated type.
	 * 
	 * @return structure of associated type or <code>null</code> if a problem occurs creating the new type
	 * @exception CoreException if unable to retrieve the structure
	 */
	public IApiType getStructure() throws CoreException;
	
	/**
	 * Returns the API component this class file originated from or
	 * <code>null</code> if unknown. Note that the component will only be <code>null</code>
	 * in the case that a class file container was created without an owning
	 * component.
	 * 
	 * @return API component or <code>null</code> if unknown
	 */
	public IApiComponent getApiComponent();
	
	
}
