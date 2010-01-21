/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.project;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.project.IBundleClasspathEntry;

/**
 * Defines relationship between a source folder and/or classfile folder and bundle
 * classpath entry.
 */
public class BundleClasspathSpecification implements IBundleClasspathEntry {

	private IPath fSource;
	private IPath fBinary;
	private IPath fEntry;

	/**
	 * Constructs a relationship. Must specify one of <code>sourceFolder</code> or
	 * <code>binaryFolder</code>.
	 * 
	 * @param sourceFolder source folder or <code>null</code>
	 * @param binaryFolder binary folder or <code>null</code>
	 * @param entry entry on the Bundle-Classpath header
	 */
	public BundleClasspathSpecification(IPath sourceFolder, IPath binaryFolder, IPath entry) {
		fSource = sourceFolder;
		fBinary = binaryFolder;
		fEntry = entry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleClasspathSpecification#getSourceFolder()
	 */
	public IPath getSourcePath() {
		return fSource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleClasspathSpecification#getClassfileFolder()
	 */
	public IPath getBinaryPath() {
		return fBinary;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleClasspathSpecification#getBundleClasspathEntry()
	 */
	public IPath getLibrary() {
		return fEntry;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IBundleClasspathEntry) {
			IBundleClasspathEntry spec = (IBundleClasspathEntry) obj;
			return equalOrNull(getSourcePath(), spec.getSourcePath()) && equalOrNull(getBinaryPath(), spec.getBinaryPath()) && equalOrNull(getLibrary(), spec.getLibrary());
		}
		return false;
	}

	private boolean equalOrNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return o1 == null;
		}
		return o1.equals(o2);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int code = getClass().hashCode();
		if (fSource != null) {
			code += fSource.hashCode();
		}
		if (fBinary != null) {
			code += fBinary.hashCode();
		}
		if (fEntry != null) {
			code += fEntry.hashCode();
		}
		return code;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Bundle-Claspath: ["); //$NON-NLS-1$
		buf.append("src="); //$NON-NLS-1$
		buf.append(fSource);
		buf.append(" bin="); //$NON-NLS-1$
		buf.append(fBinary);
		buf.append(" jar="); //$NON-NLS-1$
		buf.append(fEntry);
		buf.append("]"); //$NON-NLS-1$
		return buf.toString();
	}
}
