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

import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.*;

/**
 * @author cgwong
 *
 */
public class PatchSpecPage extends BaseFeatureSpecPage {

	public PatchSpecPage(WizardNewProjectCreationPage mainPage) {
		super(mainPage, true);
		setTitle(PDEPlugin.getResourceString("PatchSpec.title"));
		setDescription(PDEPlugin.getResourceString("PatchSpec.desc"));
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
	}

	protected void verifyComplete() {
		boolean complete = featureIdText.getText().length() > 0
				&& patchIdText.getText().length() > 0;
		setPageComplete(complete);
		if (complete) {
			String message = verifyIdRules();
			if (message != null) {
				setPageComplete(false);
				setErrorMessage(message);
			} else {
				setErrorMessage(null);
				verifyVersion();
			}
		} else
			setErrorMessage(PDEPlugin.getResourceString(KEY_MISSING));
		
		if (canFlipToNextPage()){

			IFeatureModel[] featureModels = getWorkspaceFeatureModels();

			for (int i = 0; i < featureModels.length; i++) {
				IFeature feature = featureModels[i].getFeature();
				if (feature.getId().equals(featureIdText.getText()) 
						&& feature.getVersion().equals(featureVersionText.getText())){
					fFeatureToPatch = feature.getModel();
					return;
				}
			}
			fFeatureToPatch = null;
		}
	}

	public String getPatchId(){
		if (patchIdText == null)
			return "";
		return patchIdText.getText();
	}
	
	public String getPatchName(){
		if (patchNameText == null)
			return "";
		return patchNameText.getText();
	}
	
	public String getPatchProvider(){
		if (patchProviderText == null)
			return "";
		return patchProviderText.getText();
	}
	
}
