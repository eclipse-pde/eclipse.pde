package org.eclipse.pde.internal.wizards.extension;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;


public class NewExtensionPointWizard extends NewWizard {
	private NewExtensionPointMainPage mainPage;
	private IPluginModelBase model;
	private IProject project;
public NewExtensionPointWizard(IProject project, IPluginModelBase model) {
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXP_WIZ);
	this.model = model;
	this.project = project;
	setNeedsProgressMonitor(true);
}
public void addPages() {
	mainPage = new NewExtensionPointMainPage(project, model);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
