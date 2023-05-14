/*******************************************************************************
 *  Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;

public class XMLTagScanner extends BasePDEScanner {

	private Token fStringToken;
	private Token fExternalizedStringToken;
	private Token fCommentToken;

	public XMLTagScanner(IColorManager manager) {
		super(manager);
	}

	@Override
	protected void initialize() {
		fStringToken = new Token(createTextAttribute(IPDEColorConstants.P_STRING));
		fExternalizedStringToken = new Token(createTextAttribute(IPDEColorConstants.P_EXTERNALIZED_STRING));
		fCommentToken = new Token(createTextAttribute(IPDEColorConstants.P_XML_COMMENT));
		IRule[] rules = new IRule[6];
		rules[0] = new SingleLineRule("\"%", "\"", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'%", "'", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add rule for single and double quotes
		rules[2] = new MultiLineRule("\"", "\"", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[3] = new SingleLineRule("'", "'", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[4] = new MultiLineRule("<!--", "-->", fCommentToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[5] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_TAG)));
	}

	@Override
	protected Token getTokenAffected(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.startsWith(IPDEColorConstants.P_STRING)) {
			return fStringToken;
		} else if (property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING)) {
			return fExternalizedStringToken;
		} else 	if (event.getProperty().startsWith(IPDEColorConstants.P_XML_COMMENT)) {
			return fCommentToken;
		}
		return (Token) fDefaultReturnToken;
	}

	@Override
	public boolean affectsTextPresentation(String property) {
		return property.startsWith(IPDEColorConstants.P_TAG) || property.startsWith(IPDEColorConstants.P_STRING)
				|| property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING)
				|| property.startsWith(IPDEColorConstants.P_XML_COMMENT);
	}

}
