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
package org.eclipse.pde.internal.core.target;

/**
 * Describes something with a name and version.
 */
public class NameVersionDescriptor {

	private String fId;
	private String fVersion;

	/**
	 * Constructs a descriptor.
	 * 
	 * @param id name identifier
	 * @param version version identifier, can be <code>null</code>
	 */
	public NameVersionDescriptor(String id, String version) {
		fId = id;
		fVersion = version;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof NameVersionDescriptor) {
			NameVersionDescriptor iud = (NameVersionDescriptor) obj;
			if (fId.equals(iud.fId)) {
				return (fVersion != null && fVersion.equals(iud.fVersion)) || (fVersion == null && iud.fVersion == null);
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fId.hashCode() + (fVersion != null ? fVersion.hashCode() : 0);
	}

	public String getId() {
		return fId;
	}

	public String getVersion() {
		return fVersion;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(fId);
		buf.append('_');
		buf.append(fVersion);
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
		StringBuffer buf = new StringBuffer();
		buf.append(fId);
		buf.append('@');
		buf.append(fVersion);
		return buf.toString();
	}
}
