package org.eclipse.pde.internal.component;

import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;

public class ComponentJarWizard extends Wizard {
	private IFile componentFile;
	private ComponentJarWizardPage mainPage;

public ComponentJarWizard(IFile componentFile) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
	this.componentFile = componentFile;
}
public void addPages() {
	mainPage = new ComponentJarWizardPage(componentFile);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
