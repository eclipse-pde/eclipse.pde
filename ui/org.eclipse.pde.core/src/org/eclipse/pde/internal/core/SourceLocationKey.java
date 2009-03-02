/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.osgi.framework.Version;

/**
 * Used as the key for the bundle manifest location map.  Contains
 * both the bundle name and the version.  The version attribute can
 * be null.
 * @since 3.4
 */
public class SourceLocationKey {
	private String fBundleName;
	private Version fVersion;

	public SourceLocationKey(String bundleName, Version version) {
		fBundleName = bundleName;
		fVersion = version;
	}

	public SourceLocationKey(String bundleName) {
		this(bundleName, null);
	}

	public boolean equals(Object obj) {
		if (obj instanceof SourceLocationKey) {
			SourceLocationKey key = (SourceLocationKey) obj;
			if (fVersion != null && key.fVersion != null) {
				return fBundleName.equals(((SourceLocationKey) obj).fBundleName) && fVersion.equals(((SourceLocationKey) obj).fVersion);
			} else if (fVersion == null && key.fVersion == null) {
				return fBundleName.equals(((SourceLocationKey) obj).fBundleName);
			}
		}
		return false;
	}

	public int hashCode() {
		if (fVersion == null) {
			return fBundleName.hashCode();
		}
		int result = 1;
		result = 31 * result + fBundleName.hashCode();
		result = 31 * result + fVersion.hashCode();
		return result;
	}
}