/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * PDEWizardNewFileCreationPage
 *
 */
public class PDEWizardNewFileCreationPage extends WizardNewFileCreationPage {

	/**
	 * @param pageName
	 * @param selection
	 */
	public PDEWizardNewFileCreationPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		String filename = getFileName().trim();
		// Verify the filename is non-empty
		if (filename.length() == 0) {
			// Reset previous error message set if any
			setErrorMessage(null);
			return false;
		}
		// Verify the file name does not begin with a dot
		if (filename.charAt(0) == '.') {
			setErrorMessage(PDEUIMessages.PDEWizardNewFileCreationPage_errorMsgStartsWithDot);
			return false;
		}
		// Perform default validation
		return super.validatePage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
	 */
	protected void createLinkTarget() {
		// NOOP
	}

}
