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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common base class for element descriptors.
 * 
 * @since 1.0.0
 */
public abstract class ElementDescriptorImpl implements IElementDescriptor, Comparable {

	/**
	 * Constant used for controlling tracing in the descriptor framework
	 */
	private static boolean Debug = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the descriptor framework
	 */
	public static void setDebug(boolean debugValue) {
		Debug = debugValue || Util.DEBUG;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IElementDescriptor#getParent()
	 */
	public IElementDescriptor getParent() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IElementDescriptor#getPath()
	 */
	public IElementDescriptor[] getPath() {
		List list = new ArrayList();
		IElementDescriptor element = this;
		while (element != null) {
			list.add(0, element);
			element = element.getParent();
		}
		return (IElementDescriptor[]) list.toArray(new IElementDescriptor[list.size()]);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof ElementDescriptorImpl) {
			ElementDescriptorImpl element = (ElementDescriptorImpl) o;
			return getComparable().compareTo(element.getComparable());
		}
		if (Debug) {
			System.err.println(o.getClass());
		}
		return -1;
	}

	/**
	 * Returns this element's comparable delegate.
	 * 
	 * @return comparable
	 */
	protected abstract Comparable getComparable();
}
