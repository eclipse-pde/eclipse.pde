/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.swt.widgets.Composite;

/**
 * CSAbstractSubDetails
 */
public abstract class CSAbstractSubDetails extends CSAbstractDetails {

	/**
	 * @param masterSection
	 * @param contextID
	 */
	public CSAbstractSubDetails(ICSMaster masterSection, String contextID) {
		super(masterSection, contextID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		// NO-OP
		// This will never be called directly because classes extending this
		// class are subsets of a main details page
		// To create the contents for a the subset of the main details pages,
		// calls to the ICSDetails methods are made directly
	}

}
