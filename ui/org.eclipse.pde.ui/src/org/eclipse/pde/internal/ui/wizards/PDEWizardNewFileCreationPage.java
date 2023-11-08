/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
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

	@Override
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

	@Override
	protected IStatus validateLinkedResource() {
		return Status.OK_STATUS;
	}

	@Override
	protected void createLinkTarget() {
		// NOOP
	}

}
