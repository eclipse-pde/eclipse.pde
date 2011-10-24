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
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;

/**
 * A handle to a target stored with workspace metadata.
 * 
 * @since 3.5
 */
public class LocalTargetHandle extends AbstractTargetHandle {

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
	 * Returns the next unique ID for target handle.
	 * 
	 * @return time stamp ID
	 */
	static synchronized long nextTimeStamp() {
		long stamp = System.currentTimeMillis();
		if (stamp == fgLastStamp) {
			stamp++;
		}
		fgLastStamp = stamp;
		return stamp;
	}

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
			Path path = new Path(part);
			String name = path.lastSegment();
			if (name.endsWith(ICoreConstants.TARGET_FILE_EXTENSION)) {
				String lng = name.substring(0, name.length() - ICoreConstants.TARGET_FILE_EXTENSION.length() - 1);
				long stamp = Long.parseLong(lng);
				return new LocalTargetHandle(stamp);
			}
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_0, null));
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_0, e));
		}
	}

	/**
	 * Constructs a new target handle to a local file, based on a time stamp.
	 */
	LocalTargetHandle() {
		fTimeStamp = nextTimeStamp();
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
	 * @see org.eclipse.pde.core.target.ITargetHandle#getMemento()
	 */
	public String getMemento() throws CoreException {
		try {
			URI uri = new URI(SCHEME, getFile().getName(), null);
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_2, e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetHandle#exists()
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
		StringBuffer name = new StringBuffer();
		name.append(Long.toString(fTimeStamp));
		name.append('.');
		name.append(ICoreConstants.TARGET_FILE_EXTENSION);
		return LOCAL_TARGET_CONTAINER_PATH.append(name.toString()).toFile();
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#delete()
	 */
	void delete() throws CoreException {
		File file = getFile();
		if (file.exists()) {
			file.delete();
			if (file.exists()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.LocalTargetHandle_3, file.getName())));
			}
		}
		P2TargetUtils.deleteProfile(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#getOutputStream()
	 */
	protected OutputStream getOutputStream() throws CoreException {
		try {
			File file = getFile();
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			return new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_1, e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.LocalTargetHandle_5, e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractTargetHandle#save(org.eclipse.pde.core.target.ITargetDefinition)
	 */
	void save(ITargetDefinition definition) throws CoreException {
		OutputStream stream = getOutputStream();
		((TargetDefinition) definition).write(stream);
		try {
			stream.close();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.LocalTargetHandle_4, getFile().getName()), e));
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getFile().getName();
	}
}
