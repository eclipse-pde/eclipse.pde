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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.*;

public class XMLPartitionScanner extends RuleBasedPartitionScanner {
	public final static String XML_DEFAULT = "__xml_default";
	public final static String XML_COMMENT = "__xml_comment";
	public final static String XML_TAG = "__xml_tag";

	public XMLPartitionScanner() {

		List rules = new ArrayList();

		IToken xmlComment = new Token(XML_COMMENT);
		IToken tag = new Token(XML_TAG);

		rules.add(new MultiLineRule("<!--", "-->", xmlComment));
		rules.add(new XMLTagRule(tag));

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
