package org.eclipse.pde.internal.editor.component;

import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;

public class SynchronizeVersionsWizard extends Wizard {
	private ComponentEditor componentEditor;
	private SynchronizeVersionsWizardPage mainPage;

public SynchronizeVersionsWizard(ComponentEditor componentEditor) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
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
