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

import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class OrFilter implements IFilter {

	private final IFilter[] mFilter;

	public OrFilter(final IFilter[] filter) {
		mFilter = filter;
	}

	@Override
	public boolean accept(final ImageElement element) {
		for (IFilter filter : mFilter) {
			if (filter.accept(element))
				return true;
		}

		return false;
	}
}
