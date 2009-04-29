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
	 * @param version version identifier 
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
			return fId.endsWith(iud.fId) && fVersion.equals(iud.fVersion);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fId.hashCode() + fVersion.hashCode();
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
}
