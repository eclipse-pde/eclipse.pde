/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
 * A container which can contain a set of source or class files.
 * 
 * @since 1.0.0
 */
public interface IApiTypeContainer extends IApiElement {

	/**
	 * Returns the names of all packages in this container in dot
	 * separated format. Does not include empty packages.
	 * 
	 * @return names of all packages in this container
	 * @exception if unable to retrieve package names
	 */
	public String[] getPackageNames() throws CoreException;

	/**
	 * Returns the {@link IApiTypeRoot} with the given fully qualified name
	 * or <code>null</code> if none.
	 * 
	 * @param qualifiedName fully qualified type name. Package names
	 * are dot separated and type names are '$'-separated.
	 * @return {@link IApiTypeRoot} or <code>null</code>
	 * @exception if an exception occurs retrieving the class file
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException;

	/**
	 * Returns the {@link IApiTypeRoot} with the given fully qualified name
	 * coming from the component with the given id or <code>null</code> if none.
	 * 
	 * @param qualifiedName fully qualified type name. Package names
	 * are dot separated and type names are '$'-separated.
	 * @param id the API component id to consider
	 * @return {@link IApiTypeRoot} or <code>null</code>
	 * @exception if an exception occurs retrieving the class file
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException;

	/**
	 * Visits all {@link IApiTypeRoot} in this container.
	 * 
	 * @param visitor class file visitor.
	 * @exception CoreException if unable to visit this container
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException;
	
	/**
	 * Closes this {@link IApiTypeContainer}. The container may still be used after closing,
	 * but clients should close the container when they are done with it to free
	 * system resources.
	 * 
	 * @throws CoreException if closing fails
	 */
	public void close() throws CoreException;
}
