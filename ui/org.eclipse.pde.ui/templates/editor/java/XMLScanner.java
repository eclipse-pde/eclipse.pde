package $packageName$;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class XMLScanner extends RuleBasedScanner {

public XMLScanner(ColorManager manager) {
	List rules = new ArrayList();
	IToken procInstr =
		new Token(new TextAttribute(manager.getColor(IXMLColorConstants.PROC_INSTR)));

	//Add rule for processing instructions
	rules.add(new SingleLineRule("<?", "?>", procInstr));

	// Add generic whitespace rule.
	rules.add(new WhitespaceRule(new XMLWhitespaceDetector()));

	IRule[] result = new IRule[rules.size()];
	rules.toArray(result);
	setRules(result);
}
}
