/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
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

	private final URI fURI;
	private File fFile;

	/**
	 * Constructs a new target handle to the remote file, based on its URI.
	 */
	protected ExternalFileTargetHandle(URI uri) {
		fURI = uri;
		fFile = URIUtil.toFile(fURI);
	}



	@Override
	void delete() throws CoreException {
		// We can not delete a file lying outside the workspace
	}



	@Override
	protected InputStream getInputStream() throws CoreException {
		try {
			return fURI.toURL().openStream();
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}
		return null;
	}


	@Override
	void save(ITargetDefinition definition) throws CoreException {
		try {
			OutputStream stream = new BufferedOutputStream(new FileOutputStream(fFile));
			((TargetDefinition) definition).write(stream);
			stream.close();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID,
					NLS.bind(Messages.LocalTargetHandle_4, fFile.getName()), e));
		}
	}


	@Override
	public boolean exists() {
		return fFile != null && fFile.exists();
	}


	@Override
	public String getMemento() throws CoreException {
		return fURI.toString();
	}

	@Override
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


	@Override
	public String toString() {
		return fURI.toString();
	}

}
