/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 541067
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;

/**
 * A handle to a target stored in the workspace as a <code>.target</code> file.
 *
 * @since 3.5
 */
public class WorkspaceFileTargetHandle extends AbstractTargetHandle {

	private final IFile fFile;

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

	@Override
	public String getMemento() throws CoreException {
		try {
			URI uri = new URI(SCHEME, fFile.getFullPath().toPortableString(), null);
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new CoreException(Status.error(Messages.WorkspaceFileTargetHandle_0, e));
		}
	}

	@Override
	public void save(ITargetDefinition definition) throws CoreException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		((TargetDefinition) definition).write(outputStream);
		ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
		if (!fFile.exists()) {
			fFile.create(stream, false, null);
		} else {
			// validate edit
			if (fFile.isReadOnly()) {
				IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { fFile }, null);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
			}
			fFile.setContents(stream, true, false, null);
		}
	}

	@Override
	protected InputStream getInputStream() throws CoreException {
		return fFile.getContents();
	}

	@Override
	public boolean exists() {
		return fFile.exists();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WorkspaceFileTargetHandle) {
			WorkspaceFileTargetHandle handle = (WorkspaceFileTargetHandle) obj;
			return fFile.equals(handle.fFile);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fFile.hashCode() + getClass().hashCode();
	}

	@Override
	void delete() throws CoreException {
		if (fFile.exists()) {
			fFile.delete(false, null);
		}
		P2TargetUtils.deleteProfile(this);
	}


	@Override
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
