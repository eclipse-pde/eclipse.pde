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
package org.eclipse.pde.api.tools.internal;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.model.cache.TypeStructureBuilder;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common implementation for class files.
 * 
 * @since 1.0.0
 */
public abstract class AbstractClassFile implements IClassFile {
	
	/**
	 * API component the class file originates from
	 */
	private IApiComponent fComponent;
	
	/**
	 * Constructs a class file from the given component.
	 * 
	 * @param component API component the class file comes from
	 */
	public AbstractClassFile(IApiComponent component) {
		fComponent = component;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFile#getApiComponent()
	 */
	public IApiComponent getApiComponent() {
		return fComponent;
	}

	/**
	 * Returns the bytes of this class file. 
	 *
	 * @return class file bytes
	 * @exception CoreException if unable to obtain the bytes
	 */
	public byte[] getContents() throws CoreException {
		InputStream inputStream = getInputStream();
		try {
			return Util.getInputStreamAsByteArray(inputStream, -1);
		} catch (IOException e) {
			abort("Unable to read class file: " + getTypeName(), e); //$NON-NLS-1$
			return null; // never gets here
		} finally {
			try {
				inputStream.close();
			} catch(IOException e) {
				ApiPlugin.log(e);
			}
		}
	}
	
	/**
	 * Returns an input stream for reading this class file. Clients are responsible
	 * for closing the input stream.
	 * 
	 * @return input stream
	 */
	public abstract InputStream getInputStream() throws CoreException;	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFile#getStructure()
	 */
	public IApiType getStructure() throws CoreException {
		return TypeStructureBuilder.buildTypeStructure(getContents(), fComponent, this);
	}
	
	/**
	 * Throws a core exception.
	 * 
	 * @param message message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR,
				ApiPlugin.PLUGIN_ID, message, e));
	}

}
