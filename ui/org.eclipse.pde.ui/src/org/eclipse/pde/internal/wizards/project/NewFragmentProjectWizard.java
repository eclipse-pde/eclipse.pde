package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.*;

public class NewFragmentProjectWizard extends NewProjectWizard {

public NewFragmentProjectWizard() {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
}
public boolean isFragmentWizard() {
	return true;
}
}
