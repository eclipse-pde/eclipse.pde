/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public InputStream getContents() throws CoreException {
		try {
			ZipEntry zipEntry = fZipFile.getEntry(fEntryName);
			return fZipFile.getInputStream(zipEntry);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
		}
	}

	@Override
	public IPath getFullPath() {
		return new Path(fEntryName);
	}

	@Override
	public String getName() {
		return getFullPath().lastSegment();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(ZipFile.class))
			return (T) fZipFile;
		if (adapter.equals(File.class))
			return (T) new File(fZipFile.getName());
		return super.getAdapter(adapter);
	}

	@Override
	public String toString() {
		return "JarEntryFile[" + fZipFile.getName() + "::" + fEntryName + "]"; //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JarEntryFile))
			return false;
		return toString().equals(obj.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
