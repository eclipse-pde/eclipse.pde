package org.eclipse.pde.internal.wizards.extension;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.plugin.*;
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
