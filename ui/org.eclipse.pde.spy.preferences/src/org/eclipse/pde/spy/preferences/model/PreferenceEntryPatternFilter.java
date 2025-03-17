/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.model;

import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.jface.viewers.Viewer;

public class PreferenceEntryPatternFilter extends PatternFilter {

	public PreferenceEntryPatternFilter() {
		super();
	}

	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element) {

		if (element instanceof PreferenceEntry preferenceEntry) {
			if (wordMatches(preferenceEntry.getNodePath()) || wordMatches(preferenceEntry.getKey())
					|| wordMatches(preferenceEntry.getOldValue()) || wordMatches(preferenceEntry.getNewValue())) {
				return true;
			}
		}

		return super.isLeafMatch(viewer, element);
	}

}
