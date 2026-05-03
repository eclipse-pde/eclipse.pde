/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

package org.eclipse.pde.genericeditor.extension.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;

/*
 * Copied from org.eclipse.jdt.ui.tests/test plugin/org/eclipse/jdt/testplugin/StringAsserts.javas
 */
public class StringAsserts {

	public static void assertEqualStringIgnoreDelim(String actual, String expected) throws IOException {
		if (actual == null || expected == null) {
			if (actual == expected) {
				return;
			}
			if (actual == null) {
				Assertions.fail("Content not as expected: is 'null' expected: " + expected);
			} else {
				Assertions.fail("Content not as expected: expected 'null' is: " + actual);
			}
		}

		BufferedReader read1 = new BufferedReader(new StringReader(actual));
		BufferedReader read2 = new BufferedReader(new StringReader(expected));

		int line = 1;
		do {
			String s1 = read1.readLine();
			String s2 = read2.readLine();

			if (s1 == null || !s1.equals(s2)) {
				if (s1 == null && s2 == null) {
					return;
				}
				String diffStr = (s1 == null) ? s2 : s1;

				String message = "Content not as expected: Content is: \n" + actual + "\nDiffers at line " + line + ": "
						+ diffStr + "\nExpected contents: \n" + expected;
				Assertions.assertEquals(expected, actual, message);
			}
			line++;
		} while (true);
	}
}
