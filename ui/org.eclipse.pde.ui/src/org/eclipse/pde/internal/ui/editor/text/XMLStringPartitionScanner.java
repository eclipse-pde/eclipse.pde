/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.rules.*;

/**
 * Scanner that exclusively sets predicate rules for checking spelling only for quoted strings.
 *
 */
public class XMLStringPartitionScanner extends RuleBasedPartitionScanner {
	public final static String XML_STRING = "__xml_string"; //$NON-NLS-1$
	public final static String CUSTOM_TAG = "__custom_tag"; //$NON-NLS-1$

	public static final String[] STRING_PARTITIONS = new String[] {XML_STRING, CUSTOM_TAG};

	public XMLStringPartitionScanner() {
		IPredicateRule[] rules = new IPredicateRule[6];
		rules[0] = new MultiLineRule("\"", "\"", new Token(XML_STRING), '\\', true); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new MultiLineRule("\'", "\'", new Token(XML_STRING), '\\', true); //$NON-NLS-1$ //$NON-NLS-2$
		rules[2] = new MultiLineRule("<!--", "-->", new Token(XMLPartitionScanner.XML_COMMENT)); //$NON-NLS-1$//$NON-NLS-2$
		rules[3] = new MultiLineRule("<?", "?>", new Token(XMLPartitionScanner.XML_COMMENT)); //$NON-NLS-1$ //$NON-NLS-2$
		rules[4] = new MultiLineRule("<description>", "</description>", new Token(CUSTOM_TAG)); //$NON-NLS-1$//$NON-NLS-2$
		rules[5] = new MultiLineRule("href=\"", "\"", new Token(XMLPartitionScanner.XML_COMMENT)); //$NON-NLS-1$ //$NON-NLS-2$

		setPredicateRules(rules);
	}
}
