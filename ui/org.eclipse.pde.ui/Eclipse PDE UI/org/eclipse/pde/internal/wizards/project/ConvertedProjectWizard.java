package org.eclipse.pde.internal.wizards.project;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;

public class ConvertedProjectWizard extends NewWizard {
	private ConvertedProjectsPage mainPage;

public ConvertedProjectWizard() {
	super();
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}
public void addPages() {
	mainPage = new ConvertedProjectsPage();
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
