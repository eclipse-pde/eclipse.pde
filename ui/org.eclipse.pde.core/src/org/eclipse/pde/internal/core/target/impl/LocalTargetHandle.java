/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.impl;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;

/**
 * A handle to a target stored with workspace metadata.
 * 
 * @since 3.5
 */
class LocalTargetHandle extends AbstractTargetHandle {

	/**
	 * Time stamp when target was created.
	 */
	private long fTimeStamp;

	/**
	 * The last time stamp handed out.
	 */
	private static long fgLastStamp = -1;

	/**
	 * URI scheme for local targets
	 */
	static final String SCHEME = "local"; //$NON-NLS-1$

	/**
	 * Path to the local directory where API descriptions are cached
	 * per project.
	 */
	static final IPath LOCAL_TARGET_CONTAINER_PATH = PDECore.getDefault().getStateLocation().append(".local_targets"); //$NON-NLS-1$

	/**
	 * Reconstructs a handle from the specified URI.
	 * 
	 * @param uri URI
	 * @return handle to a target in local metadata
	 * @exception if unable to restore
	 */
	static ITargetHandle restoreHandle(URI uri) throws CoreException {
		String part = uri.getSchemeSpecificPart();
		try {
			long stamp = Long.parseLong(part);
			return new LocalTargetHandle(stamp);
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_0, e));
		}
	}

	/**
	 * Constructs a new target handle to a local file, based on a time stamp.
	 */
	LocalTargetHandle() {
		fTimeStamp = System.currentTimeMillis();
		if (fTimeStamp == fgLastStamp) {
			fTimeStamp++;
		}
		fgLastStamp = fTimeStamp;
	}

	/**
	 * Reconstructs a handle.
	 * 
	 * @param stamp time stamp
	 */
	private LocalTargetHandle(long stamp) {
		fTimeStamp = stamp;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#getInputStream()
	 */
	protected InputStream getInputStream() throws CoreException {
		try {
			return new BufferedInputStream(new FileInputStream(getFile()));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_1, e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetHandle#getMemento()
	 */
	public String getMemento() throws CoreException {
		try {
			URI uri = new URI(SCHEME, Long.toString(fTimeStamp), null);
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_2, e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetHandle#exists()
	 */
	public boolean exists() {
		return getFile().exists();
	}

	/**
	 * Returns the local file associated with this target definition.
	 * 
	 * @return target file
	 */
	private File getFile() {
		return LOCAL_TARGET_CONTAINER_PATH.append(Long.toString(fTimeStamp)).toFile();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof LocalTargetHandle) {
			LocalTargetHandle handle = (LocalTargetHandle) obj;
			return handle.fTimeStamp == fTimeStamp;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) fTimeStamp;
	}
}
