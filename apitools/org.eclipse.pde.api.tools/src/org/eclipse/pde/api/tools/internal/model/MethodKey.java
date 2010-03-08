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
	private boolean fConsiderTypename = false;
	
	/**
	 * Constructs a new method key
	 * @param typename the name (fully qualified or otherwise) of the type the method is from
	 * @param name method name the name of the method
	 * @param sig method signature the signature of the method or <code>null</code>
	 * @param considertypename if the given type name should be used when computing equality and hash codes
	 */
	public MethodKey(String typename, String name, String sig, boolean considertypename) {
		fTypename = typename;
		fSelector = name;
		fSig = sig;
		fConsiderTypename = considertypename;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof MethodKey) {
			MethodKey key = (MethodKey) obj;
			return fSelector.equals(key.fSelector) &&
			 signaturesEqual(fSig, key.fSig) &&
			 (fConsiderTypename ? fTypename.equals(key.fTypename) : true);
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (fConsiderTypename ? fTypename.hashCode() : 0) + fSelector.hashCode() + (fSig == null ? 0 : fSig.hashCode());
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
