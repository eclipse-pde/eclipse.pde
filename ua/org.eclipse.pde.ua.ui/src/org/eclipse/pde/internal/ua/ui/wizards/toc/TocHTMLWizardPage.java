/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.toc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.toc.HelpEditorUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * PDEWizardNewFileCreationPage
 */
public class TocHTMLWizardPage extends WizardNewFileCreationPage {

	private String fLastFilename;

	/**
	 * @param pageName
	 * @param selection
	 */
	public TocHTMLWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		fLastFilename = getFileName().trim();

		// Verify the filename is non-empty
		if (fLastFilename.length() == 0) {
			// Reset previous error message set if any
			setErrorMessage(null);
			return false;
		}

		// Verify the file name does not begin with a dot
		if (fLastFilename.charAt(0) == '.') {
			setErrorMessage(TocWizardMessages.TocHTMLWizardPage_errorMessage1);
			return false;
		}

		if(!HelpEditorUtil.hasValidPageExtension(new Path(fLastFilename)))
		{	String message = NLS.bind(
				TocWizardMessages.TocHTMLWizardPage_errorMessage2, 
				HelpEditorUtil.getPageExtensionList());
		
			setErrorMessage(message);
			return false;
		}

		// Perform default validation
		return super.validatePage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createLinkTarget()
	 */
	protected void createLinkTarget() {
		// NO-OP
	}

	protected void createAdvancedControls(Composite parent) {
		// NO-OP
	}

	public String getFileName() {
		if (getControl() != null && getControl().isDisposed()) {
			return fLastFilename;
		}

		return super.getFileName();
	}
}
