/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.*;
import org.eclipse.jface.text.rules.*;

public class XMLCommentScanner extends RuleBasedScanner {

public XMLCommentScanner(IColorManager manager) {
	IToken comment =
		new Token(new Token(manager.getColor(IPDEColorConstants.P_XML_COMMENT)));

	List rules = new ArrayList();

	// Add rule for comments.
	rules.add(new MultiLineRule("<!--", "-->", comment)); //$NON-NLS-1$ //$NON-NLS-2$

	IRule[] result = new IRule[rules.size()];
	rules.toArray(result);
	setRules(result);
}
}
