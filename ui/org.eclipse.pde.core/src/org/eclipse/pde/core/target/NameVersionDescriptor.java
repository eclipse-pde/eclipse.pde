/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.pde.core.target;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.frameworkadmin.BundleInfo;

/**
 * Describes something with a name and version.
 *
 * @since 3.8
 * @noextend This class is not intended to be subclassed by clients.
 */
public class NameVersionDescriptor {

	public static final String TYPE_PLUGIN = "plugin"; //$NON-NLS-1$
	public static final String TYPE_FEATURE = "feature"; //$NON-NLS-1$
	public static final String TYPE_PACKAGE = "package"; //$NON-NLS-1$

	private final String fId;
	private final String fVersion;
	private final String fType;

	/**
	 * Constructs a descriptor with a type of 'plugin'
	 * <p>
	 * If the passed string version is equal to {@link BundleInfo#EMPTY_VERSION},
	 * the version will be replaced with <code>null</code>.
	 * </p>
	 *
	 * @param id name identifier
	 * @param version version identifier, can be <code>null</code>
	 */
	public NameVersionDescriptor(String id, String version) {
		Assert.isNotNull(id); // Better to throw the exception now then throw NPEs later
		fId = id;
		// If an empty version was passed to the constructor, treat it as if null was passed
		if (version == null || version.equals(BundleInfo.EMPTY_VERSION)) {
			fVersion = null;
		} else {
			fVersion = version;
		}
		fType = TYPE_PLUGIN;
	}

	/**
	 * Constructs a descriptor of the given type
	 * <p>
	 * If the passed string version is equal to {@link BundleInfo#EMPTY_VERSION},
	 * the version will be replaced with <code>null</code>.
	 * </p>
	 *
	 * @param id name identifier
	 * @param version version identifier, can be <code>null</code>
	 * @param type type of object this descriptor represents, should be one of the TYPE constants defined in this file
	 */
	public NameVersionDescriptor(String id, String version, String type) {
		fId = id;
		// If an empty version was passed to the constructor, treat it as if null was passed
		if (version == null || version.equals(BundleInfo.EMPTY_VERSION)) {
			fVersion = null;
		} else {
			fVersion = version;
		}
		fType = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NameVersionDescriptor) {
			NameVersionDescriptor iud = (NameVersionDescriptor) obj;
			if (fId.equals(iud.fId)) {
				return (fVersion != null && fVersion.equals(iud.fVersion)) || (fVersion == null && iud.fVersion == null);
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fId.hashCode() + (fVersion != null ? fVersion.hashCode() : 0);
	}

	public String getId() {
		return fId;
	}

	public String getVersion() {
		return fVersion;
	}

	public String getType() {
		return fType;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(fId);
		if (fVersion != null) {
			buf.append(' ');
			buf.append('(');
			buf.append(fVersion);
			buf.append(')');
		}
		buf.append(' ');
		buf.append('[');
		buf.append(fType);
		buf.append(']');
		return buf.toString();
	}

	/**
	 * Creates a descriptor from a portable string.
	 *
	 * @param portable generated from {@link #toPortableString()}
	 * @return descriptor
	 */
	public static NameVersionDescriptor fromPortableString(String portable) {
		int index = portable.indexOf('@');
		if (index > 0) {
			String name = portable.substring(0, index);
			String ver = null;
			index++;
			if (index < portable.length()) {
				ver = portable.substring(index);
			}
			return new NameVersionDescriptor(name, ver);
		}
		return new NameVersionDescriptor(portable, null);
	}

	/**
	 * Returns a portable form for this descriptor.
	 *
	 * @return portable form
	 */
	public String toPortableString() {
		StringBuilder buf = new StringBuilder();
		buf.append(fId);
		buf.append('@');
		buf.append(fVersion);
		return buf.toString();
	}
}
