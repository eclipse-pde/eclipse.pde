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
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;

public class XMLScanner extends BufferedRuleBasedScanner {
	private final Token fProcInstr = new Token(null);

	private final Token fExternalizedString = new Token(null);

	public XMLScanner() {
		IRule[] rules = new IRule[3];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", fProcInstr); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new ExternalizedStringRule(fExternalizedString);
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		updateColors();
	}

	public void updateColors() {
		fProcInstr.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.DIRECTIVE_COLOR)));
		fExternalizedString.setData(new TextAttribute(XMLSyntaxColors.get(XMLSyntaxColors.EXTERNALIZED_STRING_COLOR)));
	}

}
