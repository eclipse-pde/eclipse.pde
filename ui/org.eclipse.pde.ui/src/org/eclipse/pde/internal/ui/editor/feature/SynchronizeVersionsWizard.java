/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.*;

public class SynchronizeVersionsWizard extends Wizard {
	private FeatureEditor featureEditor;
	private SynchronizeVersionsWizardPage mainPage;

	public SynchronizeVersionsWizard(FeatureEditor featureEditor) {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.VersionSyncWizard_wtitle);
		this.featureEditor = featureEditor;
	}

	@Override
	public void addPages() {
		mainPage = new SynchronizeVersionsWizardPage(featureEditor);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		return mainPage.finish();
	}
}
