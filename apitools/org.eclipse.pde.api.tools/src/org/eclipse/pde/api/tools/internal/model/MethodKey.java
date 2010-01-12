/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;


/**
 * A key for a method - name & signature based.
 * 
 * @since 1.1
 */
public class MethodKey {
	private String fSelector;
	private String fSig;
	private String fTypename;
	/**
	 * Constructs a new method key
	 * @param typename
	 * @param name method name
	 * @param sig method signature
	 */
	public MethodKey(String typename, String name, String sig) {
		fTypename = typename;
		fSelector = name;
		fSig = sig;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof MethodKey) {
			MethodKey key = (MethodKey) obj;
			return fSelector.equals(key.fSelector) &&
			 signaturesEqual(fSig, key.fSig) &&
			 fTypename.equals(key.fTypename);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fTypename.hashCode() + fSelector.hashCode() + (fSig == null ? 0 : fSig.hashCode());
	}
	
	/**
	 * Returns if the given signatures are equal.
	 * Signatures are considered equal iff:
	 * <ul>
	 * <li>both are equal</li>
	 * <li>both are <code>null</code></li>
	 * </ul>
	 * 
	 * @param sig1
	 * @param sig2
	 * @return <code>true</code> if the signatures are equal <code>false</code> otherwise
	 */
	boolean signaturesEqual(String sig1, String sig2) {
		if(sig1 != null) {
			return sig1.equals(sig2);
		}
		return sig2 == null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Method Key: [enclosing type - ").append(fTypename).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("[method name - ").append(fSelector).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("[signature - ").append(fSig).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
		return buf.toString();
	}
}
