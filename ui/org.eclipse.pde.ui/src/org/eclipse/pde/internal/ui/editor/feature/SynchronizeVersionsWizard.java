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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;

public class SynchronizeVersionsWizard extends Wizard {
	private FeatureEditor featureEditor;
	private SynchronizeVersionsWizardPage mainPage;
	private static final String KEY_WTITLE = "VersionSyncWizard.wtitle"; //$NON-NLS-1$

public SynchronizeVersionsWizard(FeatureEditor featureEditor) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
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
