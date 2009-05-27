/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.util.PropertyChangeEvent;

public class XMLScanner extends BasePDEScanner {
	private Token fProcInstr;

	private Token fExternalizedString;

	public XMLScanner(IColorManager manager) {
		super(manager);
	}

	protected void initialize() {
		fProcInstr = new Token(createTextAttribute(IPDEColorConstants.P_PROC_INSTR));
		fExternalizedString = new Token(createTextAttribute(IPDEColorConstants.P_EXTERNALIZED_STRING));

		IRule[] rules = new IRule[3];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", fProcInstr); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new ExternalizedStringRule(fExternalizedString);
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_DEFAULT)));
	}

	protected Token getTokenAffected(PropertyChangeEvent event) {
		if (event.getProperty().startsWith(IPDEColorConstants.P_PROC_INSTR)) {
			return fProcInstr;
		} else if (event.getProperty().startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING)) {
			return fExternalizedString;
		}
		return (Token) fDefaultReturnToken;
	}

	public boolean affectsTextPresentation(String property) {
		return property.startsWith(IPDEColorConstants.P_DEFAULT) || property.startsWith(IPDEColorConstants.P_PROC_INSTR) || property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING);
	}

}
