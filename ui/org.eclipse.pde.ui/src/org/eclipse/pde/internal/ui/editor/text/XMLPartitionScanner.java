/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class XMLPartitionScanner extends RuleBasedPartitionScanner {
	public final static String XML_COMMENT = "__xml_comment"; //$NON-NLS-1$
	public final static String XML_TAG = "__xml_tag"; //$NON-NLS-1$

	public static final String[] PARTITIONS = new String[] {XML_COMMENT, XML_TAG};

	public XMLPartitionScanner() {
		IPredicateRule[] rules = new IPredicateRule[2];
		rules[0] = new MultiLineRule("<!--", "-->", new Token(XML_COMMENT), '\\', true); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new XMLTagRule(new Token(XML_TAG));
		setPredicateRules(rules);
	}
}
