package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;

public class SynchronizeVersionsWizard extends Wizard {
	private FeatureEditor componentEditor;
	private SynchronizeVersionsWizardPage mainPage;

public SynchronizeVersionsWizard(FeatureEditor componentEditor) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPCOMP_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
	this.componentEditor = componentEditor;
}
public void addPages() {
	mainPage = new SynchronizeVersionsWizardPage(componentEditor);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
