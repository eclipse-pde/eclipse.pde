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

import java.util.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class XMLScanner extends RuleBasedScanner {

public XMLScanner(IColorManager manager) {
	List rules = new ArrayList();
	IToken procInstr =
		new Token(new TextAttribute(manager.getColor(IPDEColorConstants.P_PROC_INSTR)));

	//Add rule for processing instructions
	rules.add(new SingleLineRule("<?", "?>", procInstr));

	// Add generic whitespace rule.
	rules.add(new WhitespaceRule(new XMLWhitespaceDetector()));

	IRule[] result = new IRule[rules.size()];
	rules.toArray(result);
	setRules(result);
}
}
