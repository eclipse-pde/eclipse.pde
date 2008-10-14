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
package org.eclipse.pde.api.tools.internal.provisional;

import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;

/**
 * Handle to bytes in a class file.
 * 
 * @since 1.0.0
 */
public interface IClassFile {
	
	public interface IModificationStamp {
		/**
		 * Returns a non-negative modification stamp.
		 * <p>
		 * A modification stamp gets updated each time a class file is modified.
		 * If a modification stamp is the same, the class file has not changed.
		 * Conversely, if a modification stamp is different, some aspect of it
		 * has been modified at least once (possibly several times).
		 * The magnitude or sign of the numerical difference between two modification stamps 
		 * is not significant.
		 * 
		 * @return modification stamp
		 */		
		public long getModificationStamp();
		/**
		 * Returns the contents used to generate the stamp or <code>null</code>
		 * if not available.
		 * 
		 * @return contents or <code>null</code>
		 */
		public byte[] getContents();
	}

	/**
	 * Returns the fully qualified name of the type contained in this class file.
	 * Package names are dot separated and type names are '$'-separated.
	 * 
	 * @return fully qualified type name
	 */
	public String getTypeName();
	
	/**
	 * Returns a URI to this class file, or <code>null</code> if it does
	 * not exist.
	 * 
	 * @return URI to this class file or <code>null</code>
	 */
	public URI getURI();
	
	/**
	 * Returns a non-negative modification stamp.
	 * <p>
	 * A modification stamp gets updated each time a class file is modified.
	 * If a modification stamp is the same, the class file has not changed.
	 * Conversely, if a modification stamp is different, some aspect of it
	 * has been modified at least once (possibly several times).
	 * The magnitude or sign of the numerical difference between two modification stamps 
	 * is not significant.
	 * 
	 * @return modification stamp
	 */
	public IModificationStamp getModificationStamp();
	
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
