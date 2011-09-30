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
package org.eclipse.pde.api.tools.internal.model;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * A class file corresponding to a resource in the workspace.
 * 
 * @since 1.0
 */
public class ResourceApiTypeRoot extends AbstractApiTypeRoot {
	
	/**
	 * Corresponding file
	 */
	private IFile fFile;

	/**
	 * Constructs an {@link IApiTypeRoot} on the underlying file.
	 * 
	 * @param parent the {@link IApiElement} parent or <code>null</code> if none
	 * @param file underlying resource
	 * @param component API component the class file originates from
	 */
	public ResourceApiTypeRoot(IApiElement parent, IFile file, String typeName) {
		super(parent, typeName);
		fFile = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.AbstractApiTypeRoot#getContents()
	 */
	public byte[] getContents() throws CoreException {
		InputStream stream = fFile.getContents(true);
		try {
			return Util.getInputStreamAsByteArray(stream, -1);
		}
		catch (IOException ioe) {
			abort("Unable to read class file: " + getTypeName(), ioe); //$NON-NLS-1$
			return null;
		}
		finally {
			try {
				stream.close();
			} catch (IOException e) {
				ApiPlugin.log(e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot#getTypeName()
	 */
	public String getTypeName() {
		return getName();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getTypeName();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getName().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiTypeRoot) {
			IApiTypeRoot file = (IApiTypeRoot) obj;
			return getName().equals(file.getTypeName());
		}
		return super.equals(obj);
	}
}
