/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.contentassist.display;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jface.internal.text.html.SingleCharReader;

/**
 * Reads a java doc comment from a java doc comment. Skips star-character
 * on begin of line.
 * <p>
 * XXX: copied from <code>org.eclipse.jdt.ui</code>.
 * </p>
 */
public class JavaDocCommentReader extends SingleCharReader {

	private IBuffer fBuffer;

	private int fCurrPos;

	private final int fStartPos;

	private final int fEndPos;

	private boolean fWasNewLine;

	public JavaDocCommentReader(IBuffer buf, int start, int end) {
		fBuffer = buf;
		fStartPos = start + 3;
		fEndPos = end - 2;

		reset();
	}

	/**
	 * @see java.io.Reader#read()
	 */
	@Override
	public int read() {
		if (fCurrPos < fEndPos) {
			char ch;
			if (fWasNewLine) {
				do {
					ch = fBuffer.getChar(fCurrPos++);
				} while (fCurrPos < fEndPos && Character.isWhitespace(ch));
				if (ch == '*') {
					if (fCurrPos < fEndPos) {
						do {
							ch = fBuffer.getChar(fCurrPos++);
						} while (ch == '*');
					} else {
						return -1;
					}
				}
			} else {
				ch = fBuffer.getChar(fCurrPos++);
			}
			fWasNewLine = IndentManipulation.isLineDelimiterChar(ch);

			return ch;
		}
		return -1;
	}

	/**
	 * @see java.io.Reader#close()
	 */
	@Override
	public void close() {
		fBuffer = null;
	}

	/**
	 * @see java.io.Reader#reset()
	 */
	@Override
	public void reset() {
		fCurrPos = fStartPos;
		fWasNewLine = true;
	}

	/**
	 * Returns the offset of the last read character in the passed buffer.
	 *
	 * @return the offset
	 */
	public int getOffset() {
		return fCurrPos;
	}

}
