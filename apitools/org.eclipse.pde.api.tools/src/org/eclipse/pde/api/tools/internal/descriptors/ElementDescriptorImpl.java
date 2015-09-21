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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Common base class for element descriptors.
 * 
 * @since 1.0.0
 */
public abstract class ElementDescriptorImpl implements IElementDescriptor, Comparable<IElementDescriptor> {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.model.component.IElementDescriptor#getParent()
	 */
	@Override
	public IElementDescriptor getParent() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.model.component.IElementDescriptor#getPath()
	 */
	@Override
	public IElementDescriptor[] getPath() {
		List<IElementDescriptor> list = new ArrayList<>();
		IElementDescriptor element = this;
		while (element != null) {
			list.add(0, element);
			element = element.getParent();
		}
		return list.toArray(new IElementDescriptor[list.size()]);
	}
}
