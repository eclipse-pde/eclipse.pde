package $packageName$;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.*;
import java.util.*;
import org.eclipse.jface.text.rules.*;



public class XMLTagScanner extends RuleBasedScanner {

public XMLTagScanner(ColorManager manager) {
	IToken string =
		new Token(new TextAttribute(manager.getColor(IXMLColorConstants.STRING)));

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
