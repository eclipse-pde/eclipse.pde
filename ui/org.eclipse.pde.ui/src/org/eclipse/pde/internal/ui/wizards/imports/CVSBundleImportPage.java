/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.importing.BundleImportDescription;
import org.eclipse.pde.internal.core.importing.CvsBundleImportDescription;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.ui.IBundeImportWizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 *
 */
public class CVSBundleImportPage extends WizardPage implements IBundeImportWizardPage {

	private BundleImportDescription[] descriptions;

	private Button useHead;

	/**
	 * Constructs the page.
	 */
	public CVSBundleImportPage() {
		super("cvs", PDEUIMessages.CVSBundleImportPage_0, null); //$NON-NLS-1$
		setDescription(PDEUIMessages.CVSBundleImportPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		useHead = SWTFactory.createCheckButton(comp, PDEUIMessages.CVSBundleImportPage_2, null, false, 1);
		setControl(comp);
		setPageComplete(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		setPageComplete(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IBundeImportWizardPage#finish()
	 */
	public boolean finish() {
		if (getControl() != null) {
			if (useHead.getSelection()) {
				// modify tags on bundle import descriptions
				for (int i = 0; i < descriptions.length; i++) {
					CvsBundleImportDescription description = (CvsBundleImportDescription) descriptions[i];
					description.setTag(null);
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IBundeImportWizardPage#getSelection()
	 */
	public BundleImportDescription[] getSelection() {
		return descriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IBundeImportWizardPage#setSelection(org.eclipse.pde.core.importing.BundleImportDescription[])
	 */
	public void setSelection(BundleImportDescription[] descriptions) {
		this.descriptions = descriptions;
	}

}
