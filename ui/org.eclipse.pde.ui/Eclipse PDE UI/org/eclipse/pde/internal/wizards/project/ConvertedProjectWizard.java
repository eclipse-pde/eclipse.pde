package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;

public class ConvertedProjectWizard extends NewWizard {
	private ConvertedProjectsPage mainPage;

public ConvertedProjectWizard() {
	super();
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
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
