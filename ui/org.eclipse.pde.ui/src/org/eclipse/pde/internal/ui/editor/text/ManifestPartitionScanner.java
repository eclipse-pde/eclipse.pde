/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.ArrayList;
import org.eclipse.jface.text.rules.*;

public class ManifestPartitionScanner extends RuleBasedPartitionScanner {

	public static final String MANIFEST_HEADER_VALUE = "__mf_bundle_header_value"; //$NON-NLS-1$

	public static final String MANIFEST_FILE_PARTITIONING = "___mf_partitioning"; //$NON-NLS-1$

	public static final String[] PARTITIONS = new String[] {MANIFEST_HEADER_VALUE};

	public ManifestPartitionScanner() {

		Token value = new Token(MANIFEST_HEADER_VALUE);
		ArrayList rules = new ArrayList();
		rules.add(new SingleLineRule("=", null, value, '\\', true, true)); //$NON-NLS-1$
		rules.add(new SingleLineRule(":", null, value, '\\', true, true)); //$NON-NLS-1$
		rules.add(new SingleLineRule(" ", null, value, '\\', true, true)); //$NON-NLS-1$
		rules.add(new SingleLineRule("\t", null, value, '\\', true, true)); //$NON-NLS-1$
		setPredicateRules((IPredicateRule[]) rules.toArray(new IPredicateRule[rules.size()]));
	}

}
