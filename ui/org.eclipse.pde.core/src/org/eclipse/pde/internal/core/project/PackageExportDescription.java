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

import org.eclipse.pde.core.project.IPackageExportDescription;
import org.osgi.framework.Version;

/**
 * Package export description.
 */
public class PackageExportDescription implements IPackageExportDescription {

	private String fName;
	private Version fVersion;
	private String[] fFriends;
	private boolean fApi;

	public PackageExportDescription(String name, Version version, String[] friends, boolean api) {
		fName = name;
		fVersion = version;
		fApi = api;
		if (friends != null && friends.length > 0) {
			fFriends = friends;
			fApi = false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IPackageExportDescription#getVersion()
	 */
	public Version getVersion() {
		return fVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IPackageExportDescription#getFriends()
	 */
	public String[] getFriends() {
		return fFriends;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IPackageExportDescription#isApi()
	 */
	public boolean isApi() {
		return fApi;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IPackageExportDescription#getName()
	 */
	public String getName() {
		return fName;
	}

	public boolean equals(Object obj) {
		if (obj instanceof PackageExportDescription) {
			PackageExportDescription spec = (PackageExportDescription) obj;
			return getName().equals(spec.getName()) && isApi() == spec.isApi() && equalOrNull(getVersion(), spec.getVersion()) && equalOrNull(getFriends(), spec.getFriends());
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int code = getClass().hashCode() + fName.hashCode();
		if (fVersion != null) {
			code += fVersion.hashCode();
		}
		if (fApi) {
			code++;
		}
		if (fFriends != null) {
			for (int i = 0; i < fFriends.length; i++) {
				code += fFriends[i].hashCode();
			}
		}
		return code;
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

	/**
	 * Returns whether the arrays are equal.
	 * 
	 * @param array1 an object array or <code>null</code> 
	 * @param array2 an object array or <code>null</code>
	 * @return whether the arrays are equal
	 */
	private boolean equalOrNull(Object[] array1, Object[] array2) {
		if (array1 == null || array1.length == 0) {
			return array2 == null || array2.length == 0;
		}
		if (array2 == null || array2.length == 0) {
			return false;
		}
		if (array1.length != array2.length) {
			return false;
		}
		for (int i = 0; i < array1.length; i++) {
			if (!array1[i].equals(array2[i])) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(fName);
		if (fVersion != null) {
			buf.append(";version="); //$NON-NLS-1$
			buf.append(fVersion.toString());
		}
		if (fFriends != null) {
			buf.append(";x-friends="); //$NON-NLS-1$
			buf.append('"');
			for (int i = 0; i < fFriends.length; i++) {
				if (i > 0) {
					buf.append(',');
				}
				buf.append(fFriends[i]);
			}
			buf.append('"');
		} else {
			if (!fApi) {
				buf.append(";x-internal=true"); //$NON-NLS-1$
			}
		}
		return buf.toString();
	}
}
