/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.rules.*;

public class XMLTagRule extends MultiLineRule {

	public XMLTagRule(IToken token) {
		super("<", ">", token); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence,
			boolean eofAllowed) {
		int c = scanner.read();
		if (sequence[0] == '<') {
			if (c == '?') {
				// processing instruction - abort
				scanner.unread();
				return false;
			}
			if (c == '!') {
				scanner.unread();
				// comment - abort
				return false;
			}
		} else if (sequence[0] == '>') {
			scanner.unread();
		}

		return super.sequenceDetected(scanner, sequence, eofAllowed);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.rules.PatternRule#endSequenceDetected(org.eclipse.jface.text.rules.ICharacterScanner)
	 */
	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		int c;
		char[][] delimiters = scanner.getLegalLineDelimiters();
		boolean previousWasEscapeCharacter = false;
		while ((c = scanner.read()) != ICharacterScanner.EOF) {
			if (c == fEscapeCharacter) {
				// Skip the escaped character.
				scanner.read();
			} else if (fEndSequence.length > 0 && c == fEndSequence[0]) {
				// Check if the specified end sequence has been found.
				if (sequenceDetected(scanner, fEndSequence, true)) {
					if (fEndSequence[0] == '>') {
						return endOfTagDetected(scanner);
					}
					return true;
				}
			} else if (fBreaksOnEOL) {
				// Check for end of line since it can be used to terminate the
				// pattern.
				for (int i = 0; i < delimiters.length; i++) {
					if (c == delimiters[i][0]
							&& sequenceDetected(scanner, delimiters[i], true)) {
						if (!fEscapeContinuesLine || !previousWasEscapeCharacter)
							return true;
					}
				}
			}
			previousWasEscapeCharacter = (c == fEscapeCharacter);
		}
		if (fBreaksOnEOF)
			return true;
		scanner.unread();
		return false;
	}

	private boolean endOfTagDetected(ICharacterScanner scanner) {
		int c;
		int scanAhead = 0;
		int endOfTagOffset = 0;
		while ((c = scanner.read()) != ICharacterScanner.EOF && c != '<') {
			scanAhead++;
			if (c == '>')
				endOfTagOffset = scanAhead;
		}

		if (c == '<') {
			int rewind = (scanAhead - endOfTagOffset) + 1;
			for (int i = 0; i < rewind; i++) {
				scanner.unread();
			}
		}
		return true;
	}
}
