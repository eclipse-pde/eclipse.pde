package org.eclipse.pde.internal.wizards.project;

import org.eclipse.pde.internal.*;

public class NewFragmentProjectWizard extends NewProjectWizard {

public NewFragmentProjectWizard() {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
}
public boolean isFragmentWizard() {
	return true;
}
}
