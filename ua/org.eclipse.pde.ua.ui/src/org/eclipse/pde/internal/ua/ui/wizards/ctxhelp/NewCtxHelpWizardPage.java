/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.ctxhelp;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page to create a new context help xml file.
 * 
 * @since 3.4
 * @see NewCtxHelpWizard
 */
public class NewCtxHelpWizardPage extends PDEWizardNewFileCreationPage {

	private static String EXTENSION = "xml"; //$NON-NLS-1$

	public NewCtxHelpWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(CtxWizardMessages.NewCtxHelpWizardPage_title);
		setDescription(CtxWizardMessages.NewCtxHelpWizardPage_description);
		// Force the file extension to be 'xml'
		setFileExtension(EXTENSION);
	}

	protected void createAdvancedControls(Composite parent) {
		// We don't want any advanced controls showing up
	}

	public void createControl(Composite parent) {
		super.createControl(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_CTX_HLP_PAGE);
	}

}
