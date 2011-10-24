/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;

/**
 * A handle to a target stored in a remote file (outside workspace) and accessed using its URI.
 * 
 * @since 3.5
 */
public class ExternalFileTargetHandle extends AbstractTargetHandle {

	/**
	 * URI scheme for remote targets
	 */
	static final String SCHEME = "file"; //$NON-NLS-1$

	/**
	 * Returns a handle for the given URI.
	 * 
	 * @param uri URI
	 * @return target handle
	 */
	static ITargetHandle restoreHandle(URI uri) {
		return new ExternalFileTargetHandle(uri);
	}

	private URI fURI;
	private File fFile;

	/**
	 * Constructs a new target handle to the remote file, based on its URI.
	 */
	protected ExternalFileTargetHandle(URI uri) {
		fURI = uri;
		fFile = URIUtil.toFile(fURI);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#delete()
	 */
	void delete() throws CoreException {
		// We can not delete a file lying outside the workspace
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#getInputStream()
	 */
	protected InputStream getInputStream() throws CoreException {
		try {
			return fURI.toURL().openStream();
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#save(org.eclipse.pde.core.target.ITargetDefinition)
	 */
	void save(ITargetDefinition definition) throws CoreException {
		try {
			OutputStream stream = new BufferedOutputStream(new FileOutputStream(fFile));
			((TargetDefinition) definition).write(stream);
			stream.close();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.LocalTargetHandle_4, fFile.getName()), e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetHandle#exists()
	 */
	public boolean exists() {
		return fFile != null && fFile.exists();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetHandle#getMemento()
	 */
	public String getMemento() throws CoreException {
		return fURI.toString();
	}

	public boolean equals(Object obj) {
		if (obj instanceof ExternalFileTargetHandle) {
			ExternalFileTargetHandle target = (ExternalFileTargetHandle) obj;
			return target.getLocation().equals(fURI);
		}
		return super.equals(obj);
	}

	public URI getLocation() {
		return fURI;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fURI.toString();
	}

}
