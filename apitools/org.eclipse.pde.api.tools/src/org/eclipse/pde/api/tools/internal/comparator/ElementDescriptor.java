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

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Represents a element inside a class file.
 */
public abstract class ElementDescriptor {
	public int access;
	public String name;
	
	/**
	 * Equivalent descriptor used by API description
	 */
	IElementDescriptor handle;
	
	public ElementDescriptor() {
		// create uninitialized element
	}
	
	ElementDescriptor(int access, String name) {
		this.name = name;
		this.access = access;
	}
	
	abstract int getElementType();
}