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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;

/**
 * Base implementation of {@link IApiMember}
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class ApiMember implements IApiMember {

	private String fName;
	private int fType;
	private IApiType fEnclosing;
	private int fFlags = -1;
	private String fSignature;
	private String fGenericSignature;
	
	/**
	 * Constructs a member enclosed by the given type with the specified
	 * name, type constant, and flags (modifiers).
	 *  
	 * @param enclosing enclosing type or <code>null</code> if unknown
	 *  or a top-level type.
	 * @param name member name
	 * @param signature signature (type or method)
	 * @param type see element type constants in {@link IApiMember}
	 * @param flags modifier flags
	 */
	protected ApiMember(IApiType enclosing, String name, String signature, String genericSig, int type, int flags) {
		fEnclosing = enclosing;
		fName = name;
		fType = type;
		fFlags = flags;
		fSignature = signature;
		fGenericSignature = genericSig;
	}
	
	/**
	 * Returns this member's signature.
	 * 
	 * @return signature
	 */
	public String getSignature() {
		return fSignature;
	}
	
	/**
	 * Returns generic signature or <code>null</code> if none.
	 * 
	 * @return
	 */
	public String getGenericSignature() {
		return fGenericSignature;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getModifiers()
	 */
	public int getModifiers() {
		return fFlags;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getName()
	 */
	public String getName() {
		return fName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getEnclosingType()
	 */
	public IApiType getEnclosingType() throws CoreException {
		if (fEnclosing == null) {
			// TODO: resolve enclosing type
		}
		return fEnclosing;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getElementType()
	 */
	public int getElementType() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getApiComponent()
	 */ 
	public IApiComponent getApiComponent() {
		try {
			return getEnclosingType().getApiComponent();
		} catch (CoreException e) {
			// should never happen for type members
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ApiMember) {
			ApiMember mem = (ApiMember) obj;
			return fEnclosing.equals(mem.fEnclosing)
			 && getName().equals(mem.getName());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fEnclosing.hashCode() + fName.hashCode();
	}	
	
	/**
	 * Used when building a member type.
	 * @param access modifiers
	 */
	void setModifiers(int access) {
		fFlags = access;
	}
}
