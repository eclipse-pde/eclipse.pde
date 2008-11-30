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

import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.swt.widgets.Composite;

/**
 * SimpleCheatSheetFileWizardPage
 */
public class SimpleCSFileWizardPage extends CSFileWizardPage {

	public final static String F_PAGE_NAME = "simple-cheatsheet"; //$NON-NLS-1$

	private String fAbsoluteFileName;

	private String fProjectName;

	/**
	 * @param pageName
	 * @param selection
	 */
	public SimpleCSFileWizardPage(IStructuredSelection selection) {
		super(F_PAGE_NAME, selection);

		// Initialize called by parent
		fAbsoluteFileName = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.CheatSheetFileWizardPage#initialize()
	 */
	protected void initialize() {
		setTitle(CSWizardMessages.SimpleCSFileWizardPage_title);
		setDescription(CSWizardMessages.SimpleCSFileWizardPage_description);
		// Force the file extension to be 'xml'
		setFileExtension(F_FILE_EXTENSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.CheatSheetFileWizardPage#getCheatSheetType()
	 */
	public int getCheatSheetType() {
		return F_SIMPLE_CHEAT_SHEET;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.CheatSheetFileWizardPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAdvancedControls(Composite parent) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.CheatSheetFileWizardPage#validatePage()
	 */
	protected boolean validatePage() {
		// Set the absolute file name
		fAbsoluteFileName = getContainerFullPath().toPortableString() + IPath.SEPARATOR + getFileName();
		// Verify that the project name chosen by the user to store the simple
		// cheat sheet is the same project name the composite cheat sheet is
		// stored in
		if (PDETextHelper.isDefined(fProjectName)) {
			// Form: /<project-name>/<dir>/<dir>/<file>
			String path = getContainerFullPath().toPortableString();
			StringTokenizer tokenizer = new StringTokenizer(path, new Character(IPath.SEPARATOR).toString());
			String compareProject = tokenizer.nextToken();
			if (compareProject.equals(fProjectName) == false) {
				setErrorMessage(NLS.bind(CSWizardMessages.SimpleCSFileWizardPage_errorMessage, fProjectName));
				return false;
			}
		}

		return super.validatePage();
	}

	/**
	 * @return
	 */
	public String getAbsoluteFileName() {
		// This is needed because the resource and group widget is disposed
		// before the file name can be retrieved
		return fAbsoluteFileName;
	}

	/**
	 * @param projectName
	 */
	public void setProjectName(String projectName) {
		fProjectName = projectName;
	}

}
