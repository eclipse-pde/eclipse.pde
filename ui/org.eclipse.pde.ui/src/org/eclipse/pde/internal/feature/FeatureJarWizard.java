package org.eclipse.pde.internal.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;

public class FeatureJarWizard extends Wizard {
	private IFile componentFile;
	private FeatureJarWizardPage mainPage;

public FeatureJarWizard(IFile componentFile) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPCOMP_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
	this.componentFile = componentFile;
}
public void addPages() {
	mainPage = new FeatureJarWizardPage(componentFile);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
