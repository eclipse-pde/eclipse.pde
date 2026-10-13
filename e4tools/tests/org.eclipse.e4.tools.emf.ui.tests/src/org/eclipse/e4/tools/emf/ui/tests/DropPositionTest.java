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

package org.eclipse.e4.tools.emf.ui.tests;

import static org.eclipse.jface.viewers.ViewerDropAdapter.LOCATION_AFTER;
import static org.eclipse.jface.viewers.ViewerDropAdapter.LOCATION_BEFORE;
import static org.eclipse.jface.viewers.ViewerDropAdapter.LOCATION_ON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.tools.emf.ui.internal.common.DropPosition;
import org.junit.jupiter.api.Test;

public class DropPositionTest {

	private static final int ROW = 24;

	@Test
	public void siblingOnlyRowIsSplitInHalves() {
		assertEquals(LOCATION_BEFORE, DropPosition.location(0, ROW, false, false));
		assertEquals(LOCATION_BEFORE, DropPosition.location(11, ROW, false, false));
		assertEquals(LOCATION_AFTER, DropPosition.location(12, ROW, false, false));
		assertEquals(LOCATION_AFTER, DropPosition.location(23, ROW, false, false));
	}

	@Test
	public void containerRowIsSplitInThirds() {
		assertEquals(LOCATION_BEFORE, DropPosition.location(7, ROW, true, false));
		assertEquals(LOCATION_ON, DropPosition.location(8, ROW, true, false));
		assertEquals(LOCATION_ON, DropPosition.location(15, ROW, true, false));
		assertEquals(LOCATION_AFTER, DropPosition.location(16, ROW, true, false));
	}

	@Test
	public void expandedRowHasNoAfterZone() {
		assertEquals(LOCATION_BEFORE, DropPosition.location(7, ROW, true, true));
		assertEquals(LOCATION_ON, DropPosition.location(23, ROW, true, true));
		assertEquals(LOCATION_BEFORE, DropPosition.location(23, ROW, false, true));
	}

	@Test
	public void indexMatchesTheIndicatedSlotWhenMovingWithinTheList() {
		for (int from = 0; from < 4; from++) {
			for (int target = 0; target < 4; target++) {
				assertMove(from, target, false);
				assertMove(from, target, true);
			}
		}
	}

	private static void assertMove(int from, int target, boolean after) {
		final List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
		final String moved = list.get(from);
		final String targetElement = list.get(target);
		final List<String> expected = new ArrayList<>(list);
		if (!moved.equals(targetElement)) {
			expected.remove(moved);
			expected.add(expected.indexOf(targetElement) + (after ? 1 : 0), moved);
		}
		list.remove(from);
		list.add(DropPosition.index(target, after, from), moved);
		assertEquals(expected, list, () -> moved + (after ? " after " : " before ") + targetElement);
	}

	@Test
	public void indexFromAnotherListIsTheInsertionPoint() {
		assertEquals(2, DropPosition.index(2, false, -1));
		assertEquals(3, DropPosition.index(2, true, -1));
	}

	@Test
	public void unknownTargetStaysNegative() {
		assertEquals(-1, DropPosition.index(-1, true, 2));
	}
}
