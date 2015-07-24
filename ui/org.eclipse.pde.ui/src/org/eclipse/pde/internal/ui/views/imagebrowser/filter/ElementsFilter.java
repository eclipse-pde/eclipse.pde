/*******************************************************************************
 *  Copyright (c) 2012, 2015 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     IBM Corporation - bugs fixing
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import java.util.*;
import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class ElementsFilter implements IFilter {

	private final List<ImageElement> mElements = new LinkedList<ImageElement>();

	public ElementsFilter() {
	}

	public void addElement(final ImageElement element) {
		mElements.add(element);
	}

	@Override
	public boolean accept(final ImageElement element) {
		return mElements.contains(element);
	}

	@Override
	public String toString() {
		return "Displayed images"; //$NON-NLS-1$
	}

	public void clear() {
		mElements.clear();
	}

	public void addAll(final Collection<ImageElement> elements) {
		mElements.addAll(elements);
	}
}
