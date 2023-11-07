package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import org.eclipse.core.text.StringMatcher;
import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

/*******************************************************************************
s
s This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Alena Laskavaia - initial API and implementation
 *******************************************************************************/

/**
 *
 * Image filter that user string pattern like "my*icon", vs PatternFilter which
 * user regular expessions.
 */
public class StringFilter implements IFilter {
	private final String mPatternString;
	private final StringMatcher mPattern;

	public StringFilter(final String pattern) {
		mPatternString = pattern;
		mPattern = new StringMatcher(pattern, true, false);
	}

	@Override
	public boolean accept(final ImageElement element) {
		return mPattern.match(element.getPlugin() + "/" + element.getPath()); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "match " + mPatternString; //$NON-NLS-1$
	}
}
