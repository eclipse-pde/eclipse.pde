/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class FeatureSpecPage extends BaseFeatureSpecPage {

	protected FeatureSpecPage(WizardNewProjectCreationPage mainPage) {
		super(mainPage, false);
		setTitle(PDEUIMessages.NewFeatureWizard_SpecPage_title);
		setDescription(PDEUIMessages.NewFeatureWizard_SpecPage_desc);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.feature.BaseFeatureSpecPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_FEATURE_DATA);
	}

	protected void initialize() {
		if (isInitialized)
			return;

		String projectName = mainPage.getProjectName();
		if (initialId == null) {
			featureIdText.setText(computeInitialId(projectName));
		}
		if (initialName == null)
			featureNameText.setText(projectName);
		featureVersionText.setText("1.0.0"); //$NON-NLS-1$

		super.initialize();
	}

	public FeatureData getFeatureData() {
		FeatureData data = new FeatureData();
		data.id = featureIdText.getText();
		data.version = featureVersionText.getText();
		data.provider = featureProviderText.getText();
		data.name = featureNameText.getText();
		data.library = getInstallHandlerLibrary();
		data.hasCustomHandler = customChoice.getSelection();
		return data;
	}

	protected void verifyComplete() {
		String message = verifyIdRules();
		if (message != null) {
			setPageComplete(false);
			setErrorMessage(message);
			return;
		}
		message = verifyVersion();
		if (message != null) {
			setPageComplete(false);
			setErrorMessage(message);
			return;
		}
		if (customChoice.getSelection() && libraryText.getText().length() == 0) {
			setPageComplete(false);
			setErrorMessage(PDEUIMessages.NewFeatureWizard_SpecPage_error_library);
			return;
		}
		setPageComplete(true);
		setErrorMessage(null);
		return;

	}
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
			isInitialized = true;
			featureIdText.setFocus();
		}
	}
	
}
