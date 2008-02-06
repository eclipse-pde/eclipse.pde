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
package org.eclipse.pde.api.tools.internal.comparator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Represents a method inside a clas file
 */
public class MethodDescriptor extends ElementDescriptor {
	private static final String INIT = "<init>"; //$NON-NLS-1$
	private static final String CLINIT = "<clinit>"; //$NON-NLS-1$

	Object defaultValue;
	String descriptor;
	Set exceptions;
	String signature;

	public MethodDescriptor(int access, String name, String desc, String signature, String[] exceptions) {
		super(access, name);
		this.descriptor = desc;
		this.signature = signature;
		if (exceptions != null) {
			this.exceptions = new HashSet();
			for (int i = 0, max = exceptions.length; i < max; i++) {
				this.exceptions.add(exceptions[i].replace('/', '.'));
			}
		}
	}
	
	public boolean isClinit() {
		return this.name.equals(CLINIT);
	}

	public boolean isConstructor() {
		return this.name.equals(INIT);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Method : access(") //$NON-NLS-1$
			.append(this.access)
			.append(") ") //$NON-NLS-1$
			.append(this.descriptor)
			.append(' ')
			.append(this.name);
		if (this.exceptions != null) {
			buffer.append(" throws "); //$NON-NLS-1$
			int i = 0;
			for (Iterator iterator = this.exceptions.iterator(); iterator.hasNext(); ) {
				if (i > 0) buffer.append(',');
				i++;
				buffer.append(iterator.next());
			}
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
		return this.isConstructor() ? IDelta.CONSTRUCTOR_ELEMENT_TYPE : IDelta.METHOD_ELEMENT_TYPE;
	}
}