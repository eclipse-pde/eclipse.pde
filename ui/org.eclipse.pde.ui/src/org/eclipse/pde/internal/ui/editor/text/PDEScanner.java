package org.eclipse.pde.internal.ui.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class PDEScanner extends RuleBasedScanner {

public PDEScanner(IColorManager manager) {
	List rules = new ArrayList();
	IToken procInstr =
		new Token(new TextAttribute(manager.getColor(IPDEColorConstants.PROC_INSTR)));

	//Add rule for processing instructions
	rules.add(new SingleLineRule("<?", "?>", procInstr));

	// Add generic whitespace rule.
	rules.add(new WhitespaceRule(new PDEWhitespaceDetector()));

	IRule[] result = new IRule[rules.size()];
	rules.toArray(result);
	setRules(result);
}
}
