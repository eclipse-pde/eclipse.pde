/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
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
	
	public XMLTagScanner(IColorManager manager) {
		super(manager);
	}
	
	protected void initialize() {
		fStringToken = new Token(createTextAttribute(IPDEColorConstants.P_STRING));
		IRule[] rules = new IRule[3];
		// Add rule for single and double quotes
		rules[0] = new MultiLineRule("\"", "\"", fStringToken, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'", "'", fStringToken, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_TAG)));
	}
	
    protected Token getTokenAffected(PropertyChangeEvent event) {
    	String property = event.getProperty();
    	if (property.startsWith(IPDEColorConstants.P_STRING)) 
    		return fStringToken;
    	return (Token)fDefaultReturnToken;
    }

	public boolean affectsTextPresentation(String property) {
		return property.startsWith(IPDEColorConstants.P_TAG) || property.startsWith(IPDEColorConstants.P_STRING);
	}

}
