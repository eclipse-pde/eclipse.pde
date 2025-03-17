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
package org.eclipse.pde.spy.preferences.viewer;

import java.util.Comparator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;

public class PreferenceEntryViewerComparator extends ViewerComparator {

	public PreferenceEntryViewerComparator() {
	}

	public PreferenceEntryViewerComparator(Comparator<? super String> comparator) {
		super(comparator);
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof PreferenceEntry entry1 && e2 instanceof PreferenceEntry) {
			PreferenceEntry entry2 = (PreferenceEntry) e2;

			long time = entry1.getTime();
			long time2 = entry2.getTime();

			if (time != 0 && time2 != 0) {
				return (int) (time2 - time);
			}
		}

		return super.compare(viewer, e1, e2);
	}

	@Override
	public int category(Object element) {
		if (element instanceof PreferenceEntry) {
			return ((PreferenceEntry) element).isRecentlyChanged() ? 0 : 1;
		}
		return 2;
	}

}
