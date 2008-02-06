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

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;

/**
 * Handle to bytes in a class file.
 * 
 * @since 1.0.0
 */
public interface IClassFile {

	/**
	 * Returns the fully qualified name of the type contained in this class file.
	 * Package names are dot separated and type names are '$'-separated.
	 * 
	 * @return fully qualified type name
	 */
	public String getTypeName();
	
	/**
	 * Returns the bytes of this class file. 
	 *
	 * @return class file bytes
	 * @exception CoreException if unable to obtain the bytes
	 */
	public byte[] getContents() throws CoreException;
	
	/**
	 * Returns an input stream for reading this class file. Clients are responsible
	 * for closing the input stream.
	 * 
	 * @return input stream
	 */
	public InputStream getInputStream() throws CoreException;
}
