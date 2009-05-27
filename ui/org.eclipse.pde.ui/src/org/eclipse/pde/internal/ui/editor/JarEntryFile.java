/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.IPDEUIConstants;

public class JarEntryFile extends PlatformObject implements IStorage {

	private ZipFile fZipFile;
	private String fEntryName;

	public JarEntryFile(ZipFile zipFile, String entryName) {
		fZipFile = zipFile;
		fEntryName = entryName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException {
		try {
			ZipEntry zipEntry = fZipFile.getEntry(fEntryName);
			return fZipFile.getInputStream(zipEntry);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath() {
		return new Path(fEntryName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName() {
		return getFullPath().lastSegment();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#isReadOnly()
	 */
	public boolean isReadOnly() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.equals(ZipFile.class))
			return fZipFile;
		if (adapter.equals(File.class))
			return new File(fZipFile.getName());
		return super.getAdapter(adapter);
	}

	public String toString() {
		return "JarEntryFile[" + fZipFile.getName() + "::" + fEntryName + "]"; //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-1$
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof JarEntryFile))
			return false;
		return toString().equals(obj.toString());
	}

}
