/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.swt.widgets.Composite;

public abstract class CSAbstractSubDetails extends CSAbstractDetails {

	public CSAbstractSubDetails(ICSMaster masterSection, String contextID) {
		super(masterSection, contextID);
	}

	@Override
	public void createContents(Composite parent) {
		// NO-OP
		// This will never be called directly because classes extending this
		// class are subsets of a main details page
		// To create the contents for a the subset of the main details pages,
		// calls to the ICSDetails methods are made directly
	}

}
