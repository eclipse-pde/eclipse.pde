/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(getEnclosingType().getQualifiedName());
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(Signature.toString(getSignature(), getName(), null, true, true));
		return buffer.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IMethodDescriptor) {
			IMethodDescriptor method = (IMethodDescriptor) obj;
			return getName().equals(method.getName()) && getEnclosingType().equals(method.getEnclosingType()) && getSignature().equals(method.getSignature());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getName().hashCode() + getEnclosingType().hashCode();
	}

	@Override
	public int getElementType() {
		return IElementDescriptor.METHOD;
	}

	@Override
	public String getSignature() {
		return fSignature;
	}

	@Override
	public boolean isConstructor() {
		return "<init>".equals(getName()); //$NON-NLS-1$
	}
}
