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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.text.StringMatcher;
import org.junit.jupiter.api.Test;

/**
 * Pins the matching behavior the icon and feature dialogs rely on.
 */
public class StringMatcherTest {

	private static boolean matches(String pattern, String text) {
		return new StringMatcher(pattern, true, false).match(text);
	}

	@Test
	public void testIconPatterns() {
		assertTrue(matches("*save*.png", "icons/full/save_edit.png")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matches("*SAVE*.png", "icons/save.PNG"), "matching ignores case"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(matches("**.gif", "a.gif"), "an empty filter matches every image of that type"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertFalse(matches("*save*.png", "icons/save.gif")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matches("*save*.png", "icons/open.png")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBinFolderPattern() {
		assertTrue(matches("bin/*", "bin/icons/a.png")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matches("bin/*", "icons/bin/a.png")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testWildcards() {
		assertTrue(matches("Pa?t", "part")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matches("Pa?t", "pat")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matches("*Stack", "PartStack")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matches("Part", "PartStack"), "a pattern without wildcards must match the whole text"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue(matches("", "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matches("", "a")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testIgnoreWildcards() {
		assertTrue(new StringMatcher("a*b", false, true).match("a*b")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(new StringMatcher("a*b", false, true).match("axb")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
