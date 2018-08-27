/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filter for the viewer, uses a text matcher
 */
class StringFilter extends ViewerFilter {

	private String pattern = null;
	StringMatcher matcher = null;

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (pattern == null) {
			return true;
		}
		if (pattern.trim().length() == 0) {
			return true;
		}
		String name = null;
		if (element instanceof IResource) {
			name = ((IResource) element).getName();
		}
		if (name == null) {
			return false;
		}
		matcher = new StringMatcher(pattern, true, false);
		return matcher.match(name, 0, name.length());
	}

}