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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Common base class for element descriptors.
 *
 * @since 1.0.0
 */
public abstract class ElementDescriptorImpl implements IElementDescriptor, Comparable<IElementDescriptor> {

	@Override
	public IElementDescriptor getParent() {
		return null;
	}

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
