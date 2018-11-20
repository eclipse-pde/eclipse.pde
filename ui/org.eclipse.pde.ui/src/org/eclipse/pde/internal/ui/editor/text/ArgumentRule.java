/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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

public class ArgumentRule extends WordPatternRule {

	private static class ArgumentDetector implements IWordDetector {

		@Override
		public boolean isWordStart(char c) {
			return '{' == c;
		}

		@Override
		public boolean isWordPart(char c) {
			return c == '}' || Character.isDigit(c);
		}
	}

	private int fCount = 0;

	/**
	 * Creates an argument rule for the given <code>token</code>.
	 *
	 * @param token the token to be returned on success
	 */
	public ArgumentRule(IToken token) {
		super(new ArgumentDetector(), "{", "}", token); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		fCount++;

		if (scanner.read() == '}')
			return fCount > 2;

		scanner.unread();
		return super.endSequenceDetected(scanner);
	}

	@Override
	protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
		fCount = 0;
		return super.sequenceDetected(scanner, sequence, eofAllowed);
	}
}
