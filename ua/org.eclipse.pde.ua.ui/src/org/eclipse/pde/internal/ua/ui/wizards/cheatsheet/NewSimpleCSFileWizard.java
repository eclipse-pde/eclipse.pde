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

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

/**
 * NewSimpleCheatSheetFileWizard
 */
public class NewSimpleCSFileWizard extends NewCSFileWizard {

	/**
	 * 
	 */
	public NewSimpleCSFileWizard() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.NewCheatSheetFileWizard#addPages()
	 */
	public void addPages() {
		fMainPage = new SimpleCSFileWizardPage(getSelection());
		addPage(fMainPage);
	}

}
