/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.feature;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;

public class RequiredFeaturesWizard extends Wizard {
	private IFeatureModel model;
	private RequiredFeaturesWizardPage mainPage;

public RequiredFeaturesWizard(IFeatureModel model) {
	this.model = model;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new RequiredFeaturesWizardPage(model);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
