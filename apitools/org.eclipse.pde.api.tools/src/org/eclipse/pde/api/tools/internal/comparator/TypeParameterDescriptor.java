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
package org.eclipse.pde.api.tools.internal.comparator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Represents a type parameter inside a generic signature
 */
class TypeParameterDescriptor {
	private static final String JAVA_LANG_OBJECT = "java.lang.Object"; //$NON-NLS-1$
	String classBound;
	List<String> interfaceBounds;
	String name;

	public TypeParameterDescriptor(String name) {
		this.name = name;
	}

	public void addInterfaceBound(String bound) {
		if (this.interfaceBounds == null) {
			this.interfaceBounds = new ArrayList<>();
		}
		this.interfaceBounds.add(bound);
	}

	public void setClassBound(String bound) {
		if (JAVA_LANG_OBJECT.equals(bound)) {
			// we consider Object as an implicit bound
			// <E> is implicitly <E extends Object>
			return;
		}
		this.classBound = bound;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("type parameter ").append(this.name).append(" : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.classBound != null) {
			buffer.append("class bound : ").append(this.classBound).append(Util.LINE_DELIMITER); //$NON-NLS-1$
		}
		if (this.interfaceBounds != null) {
			buffer.append("interface bounds : "); //$NON-NLS-1$
			int i = 0;
			for (String string : this.interfaceBounds) {
				if (i > 0) {
					buffer.append(',');
				}
				i++;
				buffer.append(string);
			}
			buffer.append(Util.LINE_DELIMITER);
		}
		return String.valueOf(buffer);
	}
}