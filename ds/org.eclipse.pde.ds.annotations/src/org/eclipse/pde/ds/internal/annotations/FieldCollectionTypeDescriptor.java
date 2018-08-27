/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class FieldCollectionTypeDescriptor {

	private final ITypeBinding elementType;

	private final boolean exact;

	public FieldCollectionTypeDescriptor(ITypeBinding elementType, boolean exact) {
		this.elementType = elementType;
		this.exact = exact;
	}

	public ITypeBinding getElementType() {
		return elementType;
	}

	public boolean isExact() {
		return exact;
	}
}