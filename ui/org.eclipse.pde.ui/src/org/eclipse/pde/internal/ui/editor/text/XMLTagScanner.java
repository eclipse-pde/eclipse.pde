/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;
import java.util.*;
import org.eclipse.jface.text.rules.*;



public class XMLTagScanner extends RuleBasedScanner {

public XMLTagScanner(IColorManager manager) {
	IToken string =
		new Token(new TextAttribute(manager.getColor(IPDEColorConstants.P_STRING)));

	Vector rules = new Vector();

	// Add rule for single and double quotes
	rules.add(new SingleLineRule("\"", "\"", string, '\\'));
	rules.add(new SingleLineRule("'", "'", string, '\\'));

	// Add generic whitespace rule.
	rules.add(new WhitespaceRule(new XMLWhitespaceDetector()));

	IRule[] result = new IRule[rules.size()];
	rules.copyInto(result);
	setRules(result);
}
	public IToken nextToken() {
		return super.nextToken();
	}
}
