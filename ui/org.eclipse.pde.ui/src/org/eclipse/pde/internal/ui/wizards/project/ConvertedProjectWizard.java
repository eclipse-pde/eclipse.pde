package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.resources.IProject;
import java.util.Vector;

public class ConvertedProjectWizard extends NewWizard {
	private ConvertedProjectsPage mainPage;
	private Vector selected;
	private static final String KEY_WTITLE = "ConvertedProjectWizard.title";

public ConvertedProjectWizard() {
	this(null);
}

public ConvertedProjectWizard(Vector selected) {
	this.selected = selected;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
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
