/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class TocWizardPage extends PDEWizardNewFileCreationPage {

	private static String EXTENSION = "xml"; //$NON-NLS-1$

	public TocWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(TocWizardMessages.TocWizardPage_title);
		setDescription(TocWizardMessages.TocWizardPage_description);
		// Force the file extension to be 'xml'
		setFileExtension(EXTENSION);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TOC_PAGE);
	}

	@Override
	protected void createAdvancedControls(Composite parent) {
	}

	@Override
	protected boolean validatePage() {
		String tocName = getTocName();
		if (tocName == null) {
			return false;
		}

		tocName = tocName.trim();
		// Verify the TOC name is non-empty
		if (tocName.length() == 0) {
			// Set the appropriate error message
			setErrorMessage(TocWizardMessages.TocWizardPage_errorMessage);
			return false;
		}
		// Perform default validation
		return super.validatePage();
	}

	public String getTocName() {
		return TocWizardMessages.TocWizardPage_name;
	}
}
