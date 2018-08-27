/*******************************************************************************
 *  Copyright (c) 2012, 2015 Christian Pontesegger and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     IBM Corporation - bugs fixing
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import java.util.regex.Pattern;
import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class PatternFilter implements IFilter {

	private final Pattern mPattern;

	public PatternFilter(final String pattern) {
		mPattern = Pattern.compile(pattern);
	}

	@Override
	public boolean accept(final ImageElement element) {
		return mPattern.matcher(element.getPlugin() + "/" + element.getPath()).matches(); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "match " + mPattern.pattern(); //$NON-NLS-1$
	}
}
