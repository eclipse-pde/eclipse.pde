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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.isite.ISiteBuildModel;
import org.eclipse.pde.internal.ui.*;

public class BuiltFeaturesWizard extends Wizard {
	private ISiteBuildModel model;
	private BuiltFeaturesWizardPage mainPage;

public BuiltFeaturesWizard(ISiteBuildModel model) {
	this.model = model;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new BuiltFeaturesWizardPage(model);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
