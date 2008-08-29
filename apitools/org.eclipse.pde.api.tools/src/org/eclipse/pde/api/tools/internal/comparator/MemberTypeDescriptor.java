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
package org.eclipse.pde.api.tools.internal.comparator;

import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.objectweb.asm.Opcodes;

public class MemberTypeDescriptor extends ElementDescriptor {
	public MemberTypeDescriptor(String name, int access) {
		this.access = access;
		IReferenceTypeDescriptor typeDescriptor = Factory.typeDescriptor(name);
		this.name = typeDescriptor.getName();
		this.handle = typeDescriptor;
	}

	public boolean equals(Object obj) {
		if (obj instanceof MemberTypeDescriptor) {
			MemberTypeDescriptor memberTypeDescriptor = (MemberTypeDescriptor) obj;
			return this.name.equals(memberTypeDescriptor.name);
		}
		return false;
	}

	int getElementType() {
		if ((this.access & Opcodes.ACC_ENUM) != 0) {
			return IDelta.ENUM_ELEMENT_TYPE;
		}
		if ((this.access & Opcodes.ACC_ANNOTATION) != 0) {
			return IDelta.ANNOTATION_ELEMENT_TYPE;
		}
		if ((this.access & Opcodes.ACC_INTERFACE) != 0) {
			return IDelta.INTERFACE_ELEMENT_TYPE;
		}
		return IDelta.CLASS_ELEMENT_TYPE;
	}

	public int hashCode() {
		return this.name.hashCode();
	}
	public boolean isMemberType() {
		return true;
	}
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Member type : access(") //$NON-NLS-1$
			.append(this.access)
			.append(") ") //$NON-NLS-1$
			.append(this.name);
		return String.valueOf(buffer);
	}
}