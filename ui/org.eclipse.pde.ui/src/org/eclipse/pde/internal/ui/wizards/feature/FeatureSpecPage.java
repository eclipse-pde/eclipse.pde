/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.*;

public class FeatureSpecPage extends BaseFeatureSpecPage {

	public static final String PAGE_TITLE = "NewFeatureWizard.SpecPage.title"; //$NON-NLS-1$
	public static final String PAGE_DESC = "NewFeatureWizard.SpecPage.desc"; //$NON-NLS-1$

	protected FeatureSpecPage(WizardNewProjectCreationPage mainPage) {
		super(mainPage, false);
		setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(PAGE_DESC));
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
		try {
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(featureVersionText
					.getText());
			data.version = pvi.toString();
		} catch (NumberFormatException e) {
			data.version = featureVersionText.getText();
		}
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
			setErrorMessage(PDEPlugin.getResourceString(KEY_LIBRARY_MISSING));
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