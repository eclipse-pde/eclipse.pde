/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
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

import java.util.HashMap;

/**
 * PDEHTMLHelper
 *
 */
public class PDEHTMLHelper {

	public final static HashMap<String, String> fgEntityLookup = new HashMap<>(6);
	static {
		fgEntityLookup.put("lt", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("gt", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("nbsp", " "); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("amp", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("apos", "'"); //$NON-NLS-1$ //$NON-NLS-2$
		fgEntityLookup.put("quot", "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String stripTags(String html) {
		if (html == null) {
			return null;
		}
		int length = html.length();
		boolean write = true;
		char oldChar = ' ';
		StringBuilder sb = new StringBuilder(length);

		boolean processingEntity = false;
		StringBuilder entityBuffer = new StringBuilder();

		for (int i = 0; i < length; i++) {
			char curr = html.charAt(i);

			// Detect predefined character entities
			if (curr == '&') {
				// Process predefined character entity found
				processingEntity = true;
				entityBuffer = new StringBuilder();
				continue;
			} else if (processingEntity && (curr == ';')) {
				// End of predefined character entity found
				processingEntity = false;
				// Resolve the entity
				String entity = (fgEntityLookup.get(entityBuffer.toString()));
				if (entity == null) {
					// If the entity is not found or supported, ignore it
					continue;
				}
				// Present the resolved character for writing
				curr = entity.charAt(0);
			} else if (processingEntity) {
				// Collect predefined character entity name character by
				// character
				entityBuffer.append(curr);
				continue;
			}

			if (curr == '<') {
				write = false;
			} else if (curr == '>') {
				write = true;
			} else if (write && curr != '\r' && curr != '\n' && curr != '\t') {
				if (!(curr == ' ') || !(oldChar == curr)) { // skip multiple spaces
					sb.append(curr);
					oldChar = curr;
				}
			}
		}
		if (isAllWhitespace(sb.toString())) {
			return null;
		}
		return sb.toString();
	}

	public static boolean isAllWhitespace(String string) {
		if (string == null) {
			return false;
		}
		char[] characters = string.toCharArray();
		for (int i = 0; i < characters.length; i++) {
			if (!Character.isWhitespace(characters[i])) {
				return false;
			}
		}
		return true;
	}

}
