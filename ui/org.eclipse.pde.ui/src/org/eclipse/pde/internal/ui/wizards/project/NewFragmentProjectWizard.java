package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.*;

public class NewFragmentProjectWizard extends NewProjectWizard {
private static final String KEY_TITLE = "NewFragmentProjectWizard.title";

public NewFragmentProjectWizard() {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
	setWindowTitle(PDEPlugin.getResourceString(KEY_TITLE));
}
public boolean isFragmentWizard() {
	return true;
}
}
