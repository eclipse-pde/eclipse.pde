/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.pde.api.tools.internal.util.Signatures;


/**
 * A key for a method - name & signature based.
 * 
 * @since 1.1
 */
public class MethodKey {
	protected String fSelector;
	protected String fSig;
	/**
	 * Constructs a new method key
	 * @param name method name
	 * @param sig method signature
	 */
	public MethodKey(String name, String sig) {
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
			Signatures.matchesSignatures(fSig, key.fSig);
		}
		
		return false;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fSelector.hashCode() + fSig.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(fSelector);
		buf.append(fSig);
		return buf.toString();
	}
}
