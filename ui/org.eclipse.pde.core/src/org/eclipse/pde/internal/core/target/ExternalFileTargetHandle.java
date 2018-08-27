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

import java.io.File;
import java.net.URI;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;

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
	private ITextFileBuffer fFileBuffer;

	/**
	 * Constructs a new target handle to the remote file, based on its URI.
	 */
	protected ExternalFileTargetHandle(URI uri) {
		fURI = uri;
		File file = URIUtil.toFile(fURI);
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		IPath path = Path.fromOSString(file.getAbsolutePath());
		try {
			manager.connect(path, LocationKind.LOCATION, null);
			fFileBuffer = manager.getTextFileBuffer(path, LocationKind.LOCATION);
		} catch (CoreException e) {
			fFileBuffer = null;
		}
	}

	@Override
	void delete() throws CoreException {
		// We can not delete a file lying outside the workspace
	}

	@Override
	protected ITextFileBuffer getTextFileBuffer() throws CoreException {
		return fFileBuffer;
	}

	@Override
	void save(ITargetDefinition definition) throws CoreException {
		((TargetDefinition) definition).write(getTextFileBuffer());
	}

	@Override
	public boolean exists() {
		return fFileBuffer != null && fFileBuffer.getFileStore() != null
				&& fFileBuffer.getFileStore().fetchInfo() != null && fFileBuffer.getFileStore().fetchInfo().exists();
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
