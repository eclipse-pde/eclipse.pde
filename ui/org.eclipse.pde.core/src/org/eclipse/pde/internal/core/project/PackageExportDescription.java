/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.project;

import org.eclipse.pde.core.project.IPackageExportDescription;
import org.osgi.framework.Version;

/**
 * Package export description.
 */
public class PackageExportDescription implements IPackageExportDescription {

	private final String fName;
	private final Version fVersion;
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

	@Override
	public Version getVersion() {
		return fVersion;
	}

	@Override
	public String[] getFriends() {
		return fFriends;
	}

	@Override
	public boolean isApi() {
		return fApi;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PackageExportDescription) {
			PackageExportDescription spec = (PackageExportDescription) obj;
			return getName().equals(spec.getName()) && isApi() == spec.isApi() && equalOrNull(getVersion(), spec.getVersion()) && equalOrNull(getFriends(), spec.getFriends());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int code = getClass().hashCode() + fName.hashCode();
		if (fVersion != null) {
			code += fVersion.hashCode();
		}
		if (fApi) {
			code++;
		}
		if (fFriends != null) {
			for (String friend : fFriends) {
				code += friend.hashCode();
			}
		}
		return code;
	}

	private boolean equalOrNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
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

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
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
