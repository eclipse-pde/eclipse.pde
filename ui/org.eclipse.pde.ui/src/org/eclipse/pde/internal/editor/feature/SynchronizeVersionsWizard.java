package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;

public class SynchronizeVersionsWizard extends Wizard {
	private FeatureEditor featureEditor;
	private SynchronizeVersionsWizardPage mainPage;

public SynchronizeVersionsWizard(FeatureEditor featureEditor) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
	this.featureEditor = featureEditor;
}
public void addPages() {
	mainPage = new SynchronizeVersionsWizardPage(featureEditor);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
