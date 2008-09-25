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
		return IDelta.MEMBER_ELEMENT_TYPE;
	}

	public int hashCode() {
		return this.name.hashCode();
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