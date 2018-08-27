/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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

	// when class file is changed, the object is changed too
	// can store the contents in the class field for optimisation.
	byte[] fContents = null;

	private long modifiedTimeStamp = IResource.NULL_STAMP;

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

	@Override
	public byte[] getContents() throws CoreException {
		if (fContents != null && fFile.getModificationStamp() == modifiedTimeStamp && modifiedTimeStamp != IResource.NULL_STAMP) {
			return fContents;
		}
		modifiedTimeStamp = fFile.getModificationStamp();
		InputStream stream = fFile.getContents(true);
		try {
			fContents = Util.getInputStreamAsByteArray(stream, -1);
			return fContents;
		} catch (IOException ioe) {
			abort("Unable to read class file: " + getTypeName(), ioe); //$NON-NLS-1$
			return null;
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				ApiPlugin.log(e);
			}
		}
	}

	@Override
	public String getTypeName() {
		return getName();
	}

	@Override
	public String toString() {
		return getTypeName();
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiTypeRoot) {
			IApiTypeRoot file = (IApiTypeRoot) obj;
			return getName().equals(file.getTypeName());
		}
		return super.equals(obj);
	}
}
