/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.xml;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;

public class XMLTagScanner extends RuleBasedScanner {

	public XMLTagScanner() {
		IToken string = new Token(new TextAttribute(XMLColors.get(SyntaxThemeConstants.STRING_COLOR)));
		IToken attributeName = new Token(new TextAttribute(XMLColors.get(SyntaxThemeConstants.ATTRIBUTE_NAME_COLOR)));

		IRule[] rules = new IRule[4];

		// Add rule for double quotes
		rules[0] = new SingleLineRule("\"", "\"", string, '\\'); //$NON-NLS-1$//$NON-NLS-2$
		// Add a rule for single quotes
		rules[1] = new SingleLineRule("'", "'", string, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		rules[2] = new AttributeNameRule(attributeName);
		// Add generic whitespace rule.
		rules[3] = new WhitespaceRule(new XMLWhitespaceDetector());

		setRules(rules);
	}

	/**
	 * Matches a name that is followed by {@code =}, so the tag name itself keeps
	 * the default tag color.
	 */
	private static class AttributeNameRule implements IRule {

		private final IToken token;

		AttributeNameRule(IToken token) {
			this.token = token;
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int read = 0;
			int c = scanner.read();
			read++;
			while (isNameChar(c)) {
				c = scanner.read();
				read++;
			}
			int nameLength = read - 1;
			while (c == ' ' || c == '\t') {
				c = scanner.read();
				read++;
			}
			if (nameLength > 0 && c == '=') {
				// keep only the name in the token
				for (int i = nameLength; i < read; i++) {
					scanner.unread();
				}
				return token;
			}
			for (int i = 0; i < read; i++) {
				scanner.unread();
			}
			return Token.UNDEFINED;
		}

		private static boolean isNameChar(int c) {
			return c != ICharacterScanner.EOF && (Character.isLetterOrDigit(c) || c == ':' || c == '_' || c == '-' || c == '.');
		}
	}
}
