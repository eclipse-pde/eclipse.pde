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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.core.plugin.*;

public class NewDependencyWizard extends Wizard {
	private IPluginModelBase modelBase;
	private NewDependencyWizardPage mainPage;
	private static final String KEY_WTITLE = "ManifestEditor.ImportListSection.new.wtitle";

public NewDependencyWizard(IPluginModelBase modelBase) {
	this.modelBase = modelBase;
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
}

public void addPages() {
	mainPage = new NewDependencyWizardPage(modelBase);
	addPage(mainPage);
}

public boolean performFinish() {
	return mainPage.finish();
}

}
