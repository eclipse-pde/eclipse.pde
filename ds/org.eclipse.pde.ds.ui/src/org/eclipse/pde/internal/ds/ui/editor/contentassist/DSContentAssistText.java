/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Content assist text is any contiguous segment of text that can be construed
 * as the beginning of a element document node prior to invoking content assist
 * at the end of it.
 */
public class DSContentAssistText {

	private String fText;

	private int fStartOffset;

	private DSContentAssistText(String text, int startOffset) {
		fText = text;
		fStartOffset = startOffset;
	}

	/**
	 * Parses document for content assist text.
	 * 
	 * @param offset
	 *            The document offset to start scanning backward from
	 * @param document
	 *            The document
	 * @return new content assist text if found; otherwise, returns null.
	 */
	public static DSContentAssistText parse(int offset, IDocument document) {
		boolean writeCAText = true;
		int lastCATextOffset = -1;
		StringBuffer buffer = new StringBuffer();
		int endOffset = offset - 1;
		char currentChar;

		if (offset <= 0) {
			return null;
		}
		// Performance enhancement
		// Ensure the first character is valid content assist text
		try {
			currentChar = document.getChar(endOffset);
		} catch (BadLocationException e) {
			return null;
		}
		if (isContentAssistText(currentChar)) {
			buffer.append(currentChar);
			lastCATextOffset = endOffset;
		} else {
			return null;
		}
		// Scan backwards from specified offset until we find a right angle
		// bracket
		for (int i = endOffset - 1; i > 0; i--) {
			try {
				currentChar = document.getChar(i);
			} catch (BadLocationException e) {
				return null;
			}
			if (isContentAssistText(currentChar)) {
				if (writeCAText) {
					// Accumulate the contiguous segment of content assist
					// text
					buffer.append(currentChar);
					// Track the start offset of the contiguous segment of
					// content assist text
					lastCATextOffset = i;
				}
			} else if (Character.isWhitespace(currentChar)) {
				// We found whitespace. This represents the contiguous text
				// boundary. Do not write anything else to the buffer.
				// Continue scanning backwards to make sure we find a right
				// angle bracket to validate what we have in the buffer is
				// indeed valid content assist text
				writeCAText = false;
			} else if (currentChar == '>') {
				// We found the right angle bracket, if there is anything in
				// the buffer it is valid content assist text
				if (buffer.length() > 0) {
					return new DSContentAssistText(buffer.reverse().toString(),
							lastCATextOffset);
				}
				return null;
			} else {
				// We found an invalid content assist character
				// Anything we have in the buffer is garbage
				return null;
			}
		}
		// We should never reach here
		return null;
	}

	/**
	 * Determines whether a character is a valid XML element name character
	 * 
	 * @param c
	 *            A character
	 * @return True if the character is valid content assist text; Otherwise,
	 *         returns false.
	 */
	private static boolean isContentAssistText(char c) {
		if ((Character.isLetterOrDigit(c)) || (c == '.') || (c == '-')
				|| (c == '_') || (c == ':')) {
			return true;
		}
		return false;
	}

	/**
	 * @return the fText
	 */
	public String getText() {
		return fText;
	}

	/**
	 * @return the fStartOffset
	 */
	public int getStartOffset() {
		return fStartOffset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Start Offset: " + fStartOffset + " Text: |" + fText + "|\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
