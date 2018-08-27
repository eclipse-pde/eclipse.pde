/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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

import org.eclipse.jface.text.rules.*;

/**
 * ExternalizedStringRule
 *
 */
public class ExternalizedStringRule extends WordPatternRule {

	/**
	 * ExternalizedStringDetector
	 *
	 */
	private static class ExternalizedStringDetector implements IWordDetector {

		/**
		 *
		 */
		public ExternalizedStringDetector() {
			super();
		}

		@Override
		public boolean isWordStart(char character) {
			if (character == '%') {
				return true;
			}
			return false;
		}

		@Override
		public boolean isWordPart(char character) {
			if (Character.isLetterOrDigit(character)) {
				return true;
			} else if (character == '.') {
				return true;
			} else if (character == '-') {
				return true;
			} else if (character == '_') {
				return true;
			}
			return false;
		}
	}

	/**
	 * @param token
	 */
	public ExternalizedStringRule(IToken token) {
		super(new ExternalizedStringDetector(), "%", null, token); //$NON-NLS-1$
	}

	@Override
	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		// Read the next character
		char character = (char) scanner.read();
		// Detect end character
		if (Character.isWhitespace(character)) {
			return true;
		} else if (character == '<') {
			return true;
		}
		// Unread the character just read
		scanner.unread();
		// Process as normal
		return super.endSequenceDetected(scanner);
	}

}
