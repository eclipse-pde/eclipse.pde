/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IArrayTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.ITypeDescriptor;

/**
 * @since 1.0.0
 */
public class ArrayDescriptorImpl extends ElementDescriptorImpl implements IArrayTypeDescriptor {
	
	/**
	 * Type of elements in the array
	 */
	private ITypeDescriptor fComponentType;
	
	/**
	 * Number of dimensions in the array
	 */
	private int fDimensions;
	
	/**
	 * Constructs a descriptor for an array.
	 * 
	 * @param componentType type of elements
	 * @param dimensions number of dimensions
	 */
	ArrayDescriptorImpl(ITypeDescriptor componentType, int dimensions) {
		fComponentType = componentType;
		fDimensions = dimensions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IArrayTypeDescriptor#getComponentType()
	 */
	public ITypeDescriptor getComponentType() {
		return fComponentType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IArrayTypeDescriptor#getDimensions()
	 */
	public int getDimensions() {
		return fDimensions;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getComponentType().toString());
		for (int i = 0; i < getDimensions(); i++) {
			buffer.append('[').append(']');
		}
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IArrayTypeDescriptor) {
			IArrayTypeDescriptor array = (IArrayTypeDescriptor) obj;
			return getComponentType().equals(array.getComponentType()) && getDimensions() == array.getDimensions();
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getComponentType().hashCode() + getDimensions();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl#getComparable()
	 */
	protected Comparable getComparable() {
		return toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IElementDescriptor#getElementType()
	 */
	public int getElementType() {
		return IElementDescriptor.T_ARRAY_TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.ITypeDescriptor#getSignature()
	 */
	public String getSignature() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < getDimensions(); i++) {
			buf.append('[');
		}
		buf.append(getComponentType().getSignature());
		return buf.toString();
	}	

}
