package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;

public class RequiredFeaturesWizard extends Wizard {
	private IFeatureModel model;
	private RequiredFeaturesWizardPage mainPage;

public RequiredFeaturesWizard(IFeatureModel model) {
	this.model = model;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new RequiredFeaturesWizardPage(model);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
