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

import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.Opcodes;

/**
 * Represents a field inside a class file.
 */
public class FieldDescriptor extends ElementDescriptor {
	String descriptor;
	String signature;
	Object value;

	public FieldDescriptor(int access, String name, String desc, String signature, Object value) {
		super(access, name);
		this.descriptor = desc;
		this.signature = signature;
		this.value = value;
	}

	public boolean isEnum() {
		return (this.access & Opcodes.ACC_ENUM) != 0;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Field : access(") //$NON-NLS-1$
			.append(this.access)
			.append(") ") //$NON-NLS-1$
			.append(this.descriptor)
			.append(' ')
			.append(this.name)
			.append(" isEnum constant ") //$NON-NLS-1$
			.append(this.isEnum());
		if (this.value != null) {
			buffer.append(" = ").append(this.value); //$NON-NLS-1$
		}
		buffer.append(';').append(Util.LINE_DELIMITER);
		if (this.signature != null) {
			buffer
				.append(" Signature : ") //$NON-NLS-1$
				.append(this.signature).append(Util.LINE_DELIMITER);
		}
		return String.valueOf(buffer);
	}
	
	int getElementType() {
		return IDelta.FIELD_ELEMENT_TYPE;
	}
}