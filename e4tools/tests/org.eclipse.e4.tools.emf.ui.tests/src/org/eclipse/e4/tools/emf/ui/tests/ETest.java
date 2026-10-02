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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.empty.E;
import org.junit.jupiter.api.Test;

public class ETest {

	@Test
	public void testCompareToOrdersNullFirst() {
		assertEquals(0, E.compareTo((Boolean) null, null));
		assertTrue(E.compareTo(null, Boolean.FALSE) < 0);
		assertTrue(E.compareTo(Boolean.FALSE, null) > 0);
		assertTrue(E.compareTo(Boolean.FALSE, Boolean.TRUE) < 0);
		assertEquals(0, E.compareTo(Boolean.TRUE, Boolean.TRUE));
	}

	@Test
	public void testEmptiness() {
		assertTrue(E.isEmpty((List<?>) null));
		assertTrue(E.isEmpty(List.of()));
		assertFalse(E.isEmpty(List.of("a"))); //$NON-NLS-1$
		assertTrue(E.notEmpty("a")); //$NON-NLS-1$
		assertFalse(E.notEmpty("")); //$NON-NLS-1$
		assertFalse(E.notEmpty((String) null));
	}
}
