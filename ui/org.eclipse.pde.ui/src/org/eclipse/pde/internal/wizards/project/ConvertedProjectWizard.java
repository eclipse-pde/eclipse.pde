package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.resources.IProject;
import java.util.Vector;

public class ConvertedProjectWizard extends NewWizard {
	private ConvertedProjectsPage mainPage;
	private Vector selected;

public ConvertedProjectWizard() {
	this(null);
}

public ConvertedProjectWizard(Vector selected) {
	this.selected = selected;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}
public void addPages() {
	mainPage = new ConvertedProjectsPage(selected);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
