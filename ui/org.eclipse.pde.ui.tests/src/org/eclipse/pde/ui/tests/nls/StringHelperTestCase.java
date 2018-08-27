/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 225047
 *******************************************************************************/
package org.eclipse.pde.ui.tests.nls;

import junit.framework.TestCase;
import org.eclipse.pde.internal.ui.nls.StringHelper;

/**
 * Tests StringHelper.java convenience methods
 * @since 3.4
 */
public class StringHelperTestCase extends TestCase {
	private static final String newLine = "\r\n";

	public void testSimpleLines() {
		String s1, s2;

		// one line
		s1 = "abc";
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals(s1, s2);

		// two lines
		s1 = "abc" + newLine + "def";
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals("abc\\r\\n\\" + newLine + "def", s2);
	}

	public void testSpaces() {
		String s1, s2;

		// one line, trailing spaces
		s1 = "ab  c    ";
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals(s1, s2);

		// two lines, second line with spaces
		s1 = "ab  c   " + newLine + "    ";
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals("ab  c   \\r\\n    ", s2);

		// two lines, second line with leading spaces
		s1 = "abc   " + newLine + "  d  ef";
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals("abc   \\r\\n  \\" + newLine + "d  ef", s2);
	}

	//	public void testSpecialChars() {
	//		String s1, s2;
	//
	//		// one unicode character
	//		s1 = "abč";
	//		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
	//		assertEquals("ab\\u010D", s2);
	//
	//		// two lines, more than one unicode character
	//		s1 = "abč " + newLine + "  d  éεﻚ f ";
	//		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
	//		assertEquals("ab\\u010D \\r\\n  \\" + newLine + "d  \\u00E9\\u03B5\\uFEDA f ", s2);
	//	}

	public void testSideEffects() {
		String s1, s2;

		// empty string
		s1 = "";
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals(s1, s2);

		// new line only
		s1 = newLine;
		s2 = StringHelper.preparePropertiesString(s1, newLine.toCharArray());
		assertEquals("\\r\\n", s2);
	}
}
