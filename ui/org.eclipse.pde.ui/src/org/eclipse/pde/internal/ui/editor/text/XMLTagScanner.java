/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
import org.eclipse.jface.util.PropertyChangeEvent;

public class XMLTagScanner extends BasePDEScanner {

	private Token fStringToken;

	private Token fExternalizedStringToken;

	public XMLTagScanner(IColorManager manager) {
		super(manager);
	}

	@Override
	protected void initialize() {
		fStringToken = new Token(createTextAttribute(IPDEColorConstants.P_STRING));
		fExternalizedStringToken = new Token(createTextAttribute(IPDEColorConstants.P_EXTERNALIZED_STRING));
		IRule[] rules = new IRule[5];
		rules[0] = new SingleLineRule("\"%", "\"", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'%", "'", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add rule for single and double quotes
		rules[2] = new MultiLineRule("\"", "\"", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[3] = new SingleLineRule("'", "'", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[4] = new WhitespaceRule(new XMLWhitespaceDetector());
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
		}
		return (Token) fDefaultReturnToken;
	}

	@Override
	public boolean affectsTextPresentation(String property) {
		return property.startsWith(IPDEColorConstants.P_TAG) || property.startsWith(IPDEColorConstants.P_STRING) || property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING);
	}

}
