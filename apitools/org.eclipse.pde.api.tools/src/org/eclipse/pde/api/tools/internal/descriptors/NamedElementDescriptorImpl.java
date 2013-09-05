/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Common base class for element descriptors with names.
 * 
 * @since 1.0.0
 */
public abstract class NamedElementDescriptorImpl extends ElementDescriptorImpl {

	/**
	 * element name
	 */
	private String fName;

	/**
	 * Constructs an element descriptor with the given name and parent.
	 * 
	 * @param name element name
	 */
	NamedElementDescriptorImpl(String name) {
		fName = name;
	}

	/**
	 * Returns this element's simple name.
	 * 
	 * @return element name
	 */
	public String getName() {
		return fName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(IElementDescriptor o) {
		if (o instanceof ReferenceTypeDescriptorImpl) {
			return getName().compareTo(((ReferenceTypeDescriptorImpl) o).getQualifiedName());
		}
		if (o instanceof NamedElementDescriptorImpl) {
			return getName().compareTo(((NamedElementDescriptorImpl) o).getName());
		}
		if (ApiPlugin.DEBUG_ELEMENT_DESCRIPTOR_FRAMEWORK) {
			System.err.println(o.getClass());
		}
		return 0;
	}
}
