package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.isite.ISiteBuildModel;
import org.eclipse.pde.internal.ui.*;

public class BuiltFeaturesWizard extends Wizard {
	private ISiteBuildModel model;
	private BuiltFeaturesWizardPage mainPage;

public BuiltFeaturesWizard(ISiteBuildModel model) {
	this.model = model;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new BuiltFeaturesWizardPage(model);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
