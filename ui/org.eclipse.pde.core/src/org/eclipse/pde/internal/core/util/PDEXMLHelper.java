/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

public class PDEXMLHelper {
	private PDEXMLHelper() {
	}

	public static String getWritableString(String source) {
		if (source == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			char character = source.charAt(i);
			switch (character) {
			case '&' -> buf.append("&amp;"); //$NON-NLS-1$
			case '<' -> buf.append("&lt;"); //$NON-NLS-1$
			case '>' -> buf.append("&gt;"); //$NON-NLS-1$
			case '\'' -> buf.append("&apos;"); //$NON-NLS-1$
			case '\"' -> buf.append("&quot;"); //$NON-NLS-1$
			default -> buf.append(character);
			}
		}
		return buf.toString();
	}

	public static String getWritableAttributeString(String source) {
		// Ensure source is defined
		if (source == null) {
			return ""; //$NON-NLS-1$
		}
		// Trim the leading and trailing whitespace if any
		source = source.trim();
		StringBuilder buffer = new StringBuilder();
		// Translate source character by character
		for (int i = 0; i < source.length(); i++) {
			char character = source.charAt(i);
			switch (character) {
			case '&' -> buffer.append("&amp;"); //$NON-NLS-1$
			case '<' -> buffer.append("&lt;"); //$NON-NLS-1$
			case '>' -> buffer.append("&gt;"); //$NON-NLS-1$
			case '\'' -> buffer.append("&apos;"); //$NON-NLS-1$
			case '\"' -> buffer.append("&quot;"); //$NON-NLS-1$
			case '\r' -> buffer.append("&#x0D;"); //$NON-NLS-1$
			case '\n' -> buffer.append("&#x0A;"); //$NON-NLS-1$
			default -> buffer.append(character);
			}
		}
		return buffer.toString();
	}

}
