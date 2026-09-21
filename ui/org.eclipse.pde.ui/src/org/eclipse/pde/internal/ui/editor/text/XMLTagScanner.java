/*******************************************************************************
 *  Copyright (c) 2000, 2026 IBM Corporation and others.
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

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;

public class XMLTagScanner extends BufferedRuleBasedScanner {

	private final Token fTagToken = new Token(null);
	private final Token fAttributeNameToken = new Token(null);
	private final Token fStringToken = new Token(null);
	private final Token fExternalizedStringToken = new Token(null);
	private final Token fCommentToken = new Token(null);

	public XMLTagScanner() {
		IRule[] rules = new IRule[7];
		rules[0] = new SingleLineRule("\"%", "\"", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'%", "'", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add rule for single and double quotes
		rules[2] = new MultiLineRule("\"", "\"", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[3] = new SingleLineRule("'", "'", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[4] = new MultiLineRule("<!--", "-->", fCommentToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[5] = new AttributeNameRule(fAttributeNameToken);
		// Add generic whitespace rule.
		rules[6] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		updateColors();
		setDefaultReturnToken(fTagToken);
	}

	public void updateColors() {
		fTagToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.TAG_COLOR)));
		fAttributeNameToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.ATTRIBUTE_NAME_COLOR)));
		fStringToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.STRING_COLOR)));
		fExternalizedStringToken.setData(new TextAttribute(XMLSyntaxColors.get(XMLSyntaxColors.EXTERNALIZED_STRING_COLOR)));
		fCommentToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.COMMENT_COLOR)));
	}

	/**
	 * Matches a name that is followed by {@code =}, so the tag name itself keeps
	 * the tag color.
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
			while (c != ICharacterScanner.EOF && Character.isWhitespace(c)) {
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
