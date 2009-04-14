/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.target.impl.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Wizard that opens an appropriate page for editing a specific type of bundle container
 *
 */
public class EditBundleContainerWizard extends Wizard {

	private IBundleContainer fContainer;
	private EditDirectoryContainerPage fPage;

	public EditBundleContainerWizard(IBundleContainer container) {
		fContainer = container;
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(AddBundleContainerSelectionPage.SETTINGS_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(AddBundleContainerSelectionPage.SETTINGS_SECTION);
		}
		setDialogSettings(settings);
		setWindowTitle(Messages.EditBundleContainerWizard_0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		if (fContainer instanceof DirectoryBundleContainer) {
			fPage = new EditDirectoryContainerPage(fContainer);
			addPage(fPage);
		} else if (fContainer instanceof ProfileBundleContainer) {
			fPage = new EditProfileContainerPage(fContainer);
			addPage(fPage);
		} else if (fContainer instanceof FeatureBundleContainer) {
			fPage = new EditFeatureContainerPage(fContainer);
			addPage(fPage);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		if (fPage != null) {
			fPage.storeSettings();
			fContainer = fPage.getBundleContainer();
			return true;
		}
		return false;
	}

	/**
	 * @return the edited bundle container (may not be the same container as provided in the contructor)
	 */
	public IBundleContainer getBundleContainer() {
		return fContainer;
	}

}
