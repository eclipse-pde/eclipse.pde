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
package org.eclipse.pde.internal.core.target.provisional;

/**
 * Describes something with a name and version.  When used as a restriction
 * in a target definition the id is always required.  If a version is set
 * it will be used, if it is <code>null</code> it will be ignored.
 * 
 * @see ITargetDefinition
 */
public class NameVersionDescriptor {

	private String fId;
	private String fVersion;

	/**
	 * Constructs a new descriptor.  Equivalent to calling
	 * {@link #NameVersionDescriptor(String, String)} with
	 * a <code>null</code> version argument.
	 * 
	 * @param id name identifier
	 */
	public NameVersionDescriptor(String id) {
		fId = id;
		fVersion = null;
	}

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
		if (fVersion != null) {
			buf.append('_');
			buf.append(fVersion);
		}
		return buf.toString();
	}
}
