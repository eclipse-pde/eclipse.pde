package org.eclipse.pde.internal.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;

public class ExternalPluginsWizard extends Wizard {
	private ExternalPluginsWizardPage mainPage;

public ExternalPluginsWizard() {
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
}
public void addPages() {
	mainPage = new ExternalPluginsWizardPage();
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
