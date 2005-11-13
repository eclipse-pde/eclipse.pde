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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jdt.internal.ui.propertiesfileeditor.LeadingWhitespacePredicateRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class ManifestPartitionScanner extends RuleBasedPartitionScanner {
	
	public static final String MANIFEST_FILE_PARTITIONING = "__mf_partitioning"; //$NON-NLS-1$
	public static final String HEADER_NAME = "__mf_header_name"; //$NON-NLS-1$
	public static final String HEADER_ASSIGNMENT = "__mf_header_assignment"; //$NON-NLS-1$ 
	public static final String HEADER_VALUE = "__mf_header_value"; //$NON-NLS-1$
	public static final String[] PARTITIONS = new String[] {
		HEADER_NAME, HEADER_ASSIGNMENT, HEADER_VALUE
	};
	
	public ManifestPartitionScanner() {
		super();

		IToken headerName = new Token(HEADER_NAME);
		IToken headerValue = new Token(HEADER_VALUE);

		IPredicateRule[] rules = new IPredicateRule[3];

		rules[0] = new LeadingWhitespacePredicateRule(headerName, "\t"); //$NON-NLS-1$
		rules[1] = new LeadingWhitespacePredicateRule(headerName, " "); //$NON-NLS-1$

		// Add rules for property values.
		rules[2] = new SingleLineRule(":", null, headerValue, '\\', true, true); //$NON-NLS-1$

		setPredicateRules(rules);
	}

}
