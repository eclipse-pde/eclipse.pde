/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import java.util.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.*;

/**
 * @author cgwong
 */
public class PatchSpecPage extends BaseFeatureSpecPage {

	public PatchSpecPage(WizardNewProjectCreationPage mainPage) {
		super(mainPage, true);
		setTitle(PDEPlugin.getResourceString("PatchSpec.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("PatchSpec.desc")); //$NON-NLS-1$
	}

	protected void initialize() {
		if (isInitialized)
			return;
		String projectName = mainPage.getProjectName();
		if (initialId == null)
			patchIdText.setText(computeInitialId(projectName));
		if (initialName == null)
			patchNameText.setText(projectName);
		setErrorMessage(null);
		super.initialize();
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
		IFeatureModel[] featureModels = getAllFeatureModels();
		
		for (int i = 0; i < featureModels.length; i++) {
		    IFeature feature = featureModels[i].getFeature();
		    if (feature.getId().equals(featureIdText.getText())
		            && feature.getVersion().equals(featureVersionText.getText())) {
		        fFeatureToPatch = feature.getModel();
		        setMessage(null);
		        setPageComplete(true);
		        setErrorMessage(null);
		        return;
		    }
		}
		
		fFeatureToPatch = null;
		setMessage(PDEPlugin.getFormattedMessage("NewFeaturePatch.SpecPage.notFound", featureIdText.getText()), DialogPage.WARNING); //$NON-NLS-1$
		setErrorMessage(null);
		getContainer().updateButtons();
		return;
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
     */
    public IWizardPage getNextPage() {
        if (fFeatureToPatch == null)
            return null;
        return super.getNextPage();
    }

	private String getPatchId() {
		if (patchIdText == null)
			return ""; //$NON-NLS-1$
		return patchIdText.getText();
	}

	private String getPatchName() {
		if (patchNameText == null)
			return ""; //$NON-NLS-1$
		return patchNameText.getText();
	}

	private String getPatchProvider() {
		if (patchProviderText == null)
			return ""; //$NON-NLS-1$
		return patchProviderText.getText();
	}
	
	public FeatureData getFeatureData() {
		FeatureData data = new FeatureData();
		data.id = getPatchId();
		data.version = "1.0.0"; //$NON-NLS-1$
		data.provider = getPatchProvider();
		data.name = getPatchName();
		data.library = getInstallHandlerLibrary();
		data.hasCustomHandler = customChoice.getSelection();
		data.isPatch = true;
		data.featureToPatchId = featureIdText.getText();
		data.featureToPatchVersion = featureVersionText.getText();
		return data;
	}

	protected String verifyIdRules() {
		String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
		String name = patchIdText.getText();
		if (name == null || name.length() == 0)
			return PDEPlugin.getResourceString(KEY_PMISSING);
		StringTokenizer stok = new StringTokenizer(name, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)) == false)
					return problemText;
			}
		}
		return super.verifyIdRules();
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
			isInitialized = true;
			patchIdText.setFocus();
		}
	}
}