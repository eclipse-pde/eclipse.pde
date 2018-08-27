/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.nls;

import java.util.Map;
import java.util.regex.Pattern;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.util.PatternConstructor;

public class AvailableFilter extends ViewerFilter {
	public static final String WILDCARD = "*"; //$NON-NLS-1$
	private Pattern fPattern;
	private final Map<?, ?> selected;
	private final ILabelProvider labelProvider;

	public AvailableFilter(Map<?, ?> selected, ILabelProvider labelProvider) {
		setPattern(WILDCARD);
		this.selected = selected;
		this.labelProvider = labelProvider;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// filter out any items that are currently selected
		// on a full refresh, these will have been added back to the list
		if (selected.containsKey(element))
			return false;

		String displayName = labelProvider.getText(element);
		return matches(element.toString()) || matches(displayName);
	}

	private boolean matches(String s) {
		return fPattern.matcher(s.toLowerCase()).matches();
	}

	public boolean setPattern(String pattern) {
		String newPattern = pattern.toLowerCase();

		if (!newPattern.endsWith(WILDCARD))
			newPattern += WILDCARD;
		if (!newPattern.startsWith(WILDCARD))
			newPattern = WILDCARD + newPattern;
		if (fPattern != null) {
			String oldPattern = fPattern.pattern();
			if (newPattern.equals(oldPattern))
				return false;
		}
		fPattern = PatternConstructor.createPattern(newPattern, true);
		return true;
	}
}
