/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Description of a method.
 * 
 * @since 1.0.0
 */
public class MethodDescriptorImpl extends MemberDescriptorImpl implements IMethodDescriptor {

	/**
	 * Method signature
	 */
	private String fSignature;

	/**
	 * Constructs a method descriptor.
	 * 
	 * @param name method name
	 * @param enclosingType enclosing type
	 * @param signature method signature
	 */
	MethodDescriptorImpl(String name, IReferenceTypeDescriptor enclosingType, String signature) {
		super(name, enclosingType);
		fSignature = signature;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getEnclosingType().getQualifiedName());
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(Signature.toString(getSignature(), getName(), null, true, true));
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IMethodDescriptor) {
			IMethodDescriptor method = (IMethodDescriptor) obj;
			return getName().equals(method.getName()) && getEnclosingType().equals(method.getEnclosingType()) && getSignature().equals(method.getSignature());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getName().hashCode() + getEnclosingType().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.model.component.IElementDescriptor#getElementType
	 * ()
	 */
	@Override
	public int getElementType() {
		return IElementDescriptor.METHOD;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.model.component.IMethodDescriptor#getSignature
	 * ()
	 */
	@Override
	public String getSignature() {
		return fSignature;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor
	 * #isConstructor()
	 */
	@Override
	public boolean isConstructor() {
		return "<init>".equals(getName()); //$NON-NLS-1$
	}
}
