/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.regex.Pattern;

public class PatternConstructor {
	private static final Pattern PATTERN_BACK_SLASH = Pattern.compile("\\\\"); //$NON-NLS-1$

	private static final Pattern PATTERN_QUESTION = Pattern.compile("\\?"); //$NON-NLS-1$

	private static final Pattern PATTERN_STAR = Pattern.compile("\\*"); //$NON-NLS-1$

	private static final Pattern PATTERN_LBRACKET = Pattern.compile("\\("); //$NON-NLS-1$

	private static final Pattern PATTERN_RBRACKET = Pattern.compile("\\)"); //$NON-NLS-1$

	/*
	 * Converts user string to regular expres '*' and '?' to regEx variables.
	 * 
	 */
	private static String asRegEx(String pattern, boolean group) {
		// Replace \ with \\, * with .* and ? with .
		// Quote remaining characters
		String result1 = PATTERN_BACK_SLASH.matcher(pattern).replaceAll("\\\\E\\\\\\\\\\\\Q"); //$NON-NLS-1$
		String result2 = PATTERN_STAR.matcher(result1).replaceAll("\\\\E.*\\\\Q"); //$NON-NLS-1$
		String result3 = PATTERN_QUESTION.matcher(result2).replaceAll("\\\\E.\\\\Q"); //$NON-NLS-1$
		if (group) {
			result3 = PATTERN_LBRACKET.matcher(result3).replaceAll("\\\\E(\\\\Q"); //$NON-NLS-1$
			result3 = PATTERN_RBRACKET.matcher(result3).replaceAll("\\\\E)\\\\Q"); //$NON-NLS-1$
		}
		return "\\Q" + result3 + "\\E"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates a regular expression pattern from the pattern string (which is
	 * our old 'StringMatcher' format).
	 * 
	 * @param pattern
	 *            The search pattern
	 * @param isCaseSensitive
	 *            Set to <code>true</code> to create a case insensitve pattern
	 * @return The created pattern
	 */
	public static Pattern createPattern(String pattern, boolean isCaseSensitive) {
		if (isCaseSensitive)
			return Pattern.compile(asRegEx(pattern, false));
		return Pattern.compile(asRegEx(pattern, false), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	public static Pattern createGroupedPattern(String pattern, boolean isCaseSensitive) {
		if (isCaseSensitive)
			return Pattern.compile(asRegEx(pattern, true));
		return Pattern.compile(asRegEx(pattern, true), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	private PatternConstructor() {
	}
}
