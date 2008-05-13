/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;

public class DSFileWizardPage extends PDEWizardNewFileCreationPage {

	public static final String F_PAGE_NAME = "ds"; //$NON-NLS-1$
	
	private static final String F_FILE_EXTENSION = "xml"; //$NON-NLS-1$
	
	public DSFileWizardPage(IStructuredSelection selection) {
		super(F_PAGE_NAME, selection);
		
		initialize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.CheatSheetFileWizardPage#initialize()
	 */
	protected void initialize() {
		setTitle(Messages.DSFileWizardPage_title);
		setDescription(Messages.DSFileWizardPage_description);
		// Force the file extension to be 'xml'
		setFileExtension(F_FILE_EXTENSION);
	}
	
}
