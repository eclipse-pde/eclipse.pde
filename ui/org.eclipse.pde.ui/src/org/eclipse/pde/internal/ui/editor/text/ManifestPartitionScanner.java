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

import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;

public class ManifestPartitionScanner extends RuleBasedPartitionScanner {
	
	public static final String MANIFEST_FILE_PARTITIONING = "__mf_partitioning"; //$NON-NLS-1$

	public static final String MANIFEST_HEADER = "__mf_bundle_header"; //$NON-NLS-1$
	
	public static final String[] PARTITIONS = new String[] {MANIFEST_HEADER};
	
	public ManifestPartitionScanner() {
	}

}
