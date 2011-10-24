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
package org.eclipse.pde.internal.core.target;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.PDECore;

/**
 * A handle to a target stored in the workspace as a <code>.target</code> file.
 * 
 * @since 3.5
 */
public class WorkspaceFileTargetHandle extends AbstractTargetHandle {

	private IFile fFile;

	/**
	 * Scheme for resource target handle
	 */
	static final String SCHEME = "resource"; //$NON-NLS-1$

	/**
	 * Returns a handle for the given URI.
	 * 
	 * @param uri URI
	 * @return target handle
	 */
	static ITargetHandle restoreHandle(URI uri) {
		String part = uri.getSchemeSpecificPart();
		Path path = new Path(part);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		return new WorkspaceFileTargetHandle(file);
	}

	/**
	 * Constructs a handle to a target in the given file.
	 * 
	 * @param file underlying file - may or may not exist
	 */
	public WorkspaceFileTargetHandle(IFile file) {
		fFile = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetHandle#getMemento()
	 */
	public String getMemento() throws CoreException {
		try {
			URI uri = new URI(SCHEME, fFile.getFullPath().toPortableString(), null);
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.WorkspaceFileTargetHandle_0, e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#getInputStream()
	 */
	protected InputStream getInputStream() throws CoreException {
		return fFile.getContents();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetHandle#exists()
	 */
	public boolean exists() {
		return fFile.exists();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof WorkspaceFileTargetHandle) {
			WorkspaceFileTargetHandle handle = (WorkspaceFileTargetHandle) obj;
			return fFile.equals(handle.fFile);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fFile.hashCode() + getClass().hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#delete()
	 */
	void delete() throws CoreException {
		if (fFile.exists()) {
			fFile.delete(false, null);
		}
		P2TargetUtils.deleteProfile(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#save(org.eclipse.pde.core.target.ITargetDefinition)
	 */
	public void save(ITargetDefinition definition) throws CoreException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		((TargetDefinition) definition).write(outputStream);
		ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
		if (!fFile.exists()) {
			fFile.create(stream, false, null);
		} else {
			// validate edit
			if (fFile.isReadOnly()) {
				IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {fFile}, null);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
			}
			fFile.setContents(stream, true, false, null);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fFile.getName();
	}

	/**
	 * Returns the target file.
	 * 
	 * @return target file
	 */
	public IFile getTargetFile() {
		return fFile;
	}
}
