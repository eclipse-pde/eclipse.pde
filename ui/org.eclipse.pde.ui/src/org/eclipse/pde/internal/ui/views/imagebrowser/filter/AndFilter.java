/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

public class AndFilter<T> implements IFilter<T> {

	private final IFilter<T>[] mFilter;

	public AndFilter(final IFilter<T>[] filter) {
		mFilter = filter;
	}

	public boolean accept(final T element) {
		for (IFilter<T> filter : mFilter) {
			if (!filter.accept(element))
				return false;
		}

		return true;
	}
}
