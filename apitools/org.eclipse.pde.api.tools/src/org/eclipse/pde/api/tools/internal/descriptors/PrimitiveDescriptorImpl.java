/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IArrayTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPrimitiveTypeDescriptor;

/**
 * A primitive type.
 * 
 * @since 1.0.0
 */
public class PrimitiveDescriptorImpl extends ElementDescriptorImpl implements IPrimitiveTypeDescriptor {

	private String fSignature;
	
	/**
	 * Constructs a primitive type descriptor based on the given signature.
	 * 
	 * @param signature primitive type signature
	 */
	public PrimitiveDescriptorImpl(String signature) {
		fSignature = signature;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IPrimitiveTypeDescriptor#getArray(int)
	 */
	public IArrayTypeDescriptor getArray(int dimensions) {
		return new ArrayDescriptorImpl(this, dimensions);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IPrimitiveTypeDescriptor#getSignature()
	 */
	public String getSignature() {
		return fSignature;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return Signature.toString(getSignature());
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IPrimitiveTypeDescriptor) {
			IPrimitiveTypeDescriptor prim = (IPrimitiveTypeDescriptor) obj;
			return getSignature().equals(prim.getSignature());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getSignature().hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl#getComparable()
	 */
	protected Comparable getComparable() {
		return getSignature();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IElementDescriptor#getElementType()
	 */
	public int getElementType() {
		return IElementDescriptor.T_PRIMITIVE_TYPE;
	}	
}
