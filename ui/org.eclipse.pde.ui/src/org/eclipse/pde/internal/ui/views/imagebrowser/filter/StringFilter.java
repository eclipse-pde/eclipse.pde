package org.eclipse.pde.internal.ui.views.imagebrowser.filter;

import org.eclipse.pde.internal.ui.util.StringMatcher;
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
 *
 */
public class StringFilter implements IFilter {
	private final StringMatcher mPattern;

	public StringFilter(final String pattern) {
		mPattern = new StringMatcher(pattern, true, false) {
			@Override
			public String toString() {
				return fPattern;
			}
		};
	}

	@Override
	public boolean accept(final ImageElement element) {
		return mPattern.match(element.getPlugin() + "/" + element.getPath()); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "match " + mPattern.toString(); //$NON-NLS-1$
	}
}
