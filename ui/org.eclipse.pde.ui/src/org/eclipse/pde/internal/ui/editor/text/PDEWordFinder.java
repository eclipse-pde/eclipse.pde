/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
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

package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;

/**
 * PDEWordFinder
 *
 */
public class PDEWordFinder {

	/**
	 *
	 */
	public PDEWordFinder() {
		// NO-OP
	}

	/**
	 * Copied from <code>org.eclipse.jdt.internal.ui.text.JavaWordFinder</code>
	 * @param document
	 * @param offset
	 */
	public static IRegion findWord(IDocument document, int offset) {

		int start = -2;
		int end = -1;

		try {
			int pos = offset;
			char c;

			while (pos >= 0) {
				c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--pos;
			}
			start = pos;

			pos = offset;
			int length = document.getLength();

			while (pos < length) {
				c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++pos;
			}
			end = pos;

		} catch (BadLocationException x) {
		}

		if (start >= -1 && end > -1) {
			if (start == offset && end == offset)
				return new Region(offset, 0);
			else if (start == offset)
				return new Region(start, end - start);
			else
				return new Region(start + 1, end - start - 1);
		}

		return null;
	}

}
