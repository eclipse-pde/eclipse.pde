package org.eclipse.pde.internal.ui.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.jface.text.rules.*;

public class PDECommentScanner extends RuleBasedScanner {

public PDECommentScanner(IColorManager manager) {
	IToken comment =
		new Token(new Token(manager.getColor(IPDEColorConstants.XML_COMMENT)));

	List rules = new ArrayList();

	// Add rule for comments.
	rules.add(new MultiLineRule("<!--", "-->", comment));

	IRule[] result = new IRule[rules.size()];
	rules.toArray(result);
	setRules(result);
}
}
