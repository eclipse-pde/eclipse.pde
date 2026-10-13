/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common;

import org.eclipse.jface.viewers.ViewerDropAdapter;

/**
 * Where a drag and drop in the model editor tree goes: the drop zone under the
 * pointer and the list index that zone stands for.
 */
public final class DropPosition {

	private DropPosition() {
	}

	/**
	 * Returns the {@link ViewerDropAdapter} location for a pointer {@code offset}
	 * pixels below the top of a row that accepts the element as a sibling.
	 *
	 * @param acceptsChild         whether the row's element also accepts the
	 *                             dragged element as a child
	 * @param expandedWithChildren whether the row shows its children below it, so
	 *                             that a line under it would mean "first child"
	 *                             rather than "after the whole subtree"
	 */
	public static int location(int offset, int height, boolean acceptsChild, boolean expandedWithChildren) {
		if (!acceptsChild) {
			return expandedWithChildren || offset < height / 2 ? ViewerDropAdapter.LOCATION_BEFORE
					: ViewerDropAdapter.LOCATION_AFTER;
		}
		if (offset < height / 3) {
			return ViewerDropAdapter.LOCATION_BEFORE;
		}
		if (expandedWithChildren || offset < height * 2 / 3) {
			return ViewerDropAdapter.LOCATION_ON;
		}
		return ViewerDropAdapter.LOCATION_AFTER;
	}

	/**
	 * Returns the index the dragged element ends up at when dropped before or after
	 * the element at {@code target}, or {@code target} itself when it is negative.
	 *
	 * @param current the dragged element's index in the same list, or -1 when it
	 *                comes from another list
	 */
	public static int index(int target, boolean after, int current) {
		if (target < 0) {
			return target;
		}
		final int index = after ? target + 1 : target;
		return current >= 0 && current < index ? index - 1 : index;
	}
}
