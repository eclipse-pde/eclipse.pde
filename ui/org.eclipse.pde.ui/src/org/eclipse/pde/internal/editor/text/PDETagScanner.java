package org.eclipse.pde.internal.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.*;
import java.util.*;
import org.eclipse.jface.text.rules.*;



public class PDETagScanner extends RuleBasedScanner {

public PDETagScanner(IColorManager manager) {
	IToken string =
		new Token(new TextAttribute(manager.getColor(IPDEColorConstants.STRING)));

	Vector rules = new Vector();

	// Add rule for single and double quotes
	rules.add(new SingleLineRule("\"", "\"", string, '\\'));
	rules.add(new SingleLineRule("'", "'", string, '\\'));

	// Add generic whitespace rule.
	rules.add(new WhitespaceRule(new PDEWhitespaceDetector()));

	IRule[] result = new IRule[rules.size()];
	rules.copyInto(result);
	setRules(result);
}
	public IToken nextToken() {
		return super.nextToken();
	}
}
