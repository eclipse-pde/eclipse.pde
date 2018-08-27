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
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.PlatformUI;

public class UpdateBuildpathWizard extends Wizard {
	private UpdateBuildpathWizardPage page1;
	private IPluginModelBase[] fSelected;
	private IPluginModelBase[] fUnupdated;
	private static final String STORE_SECTION = "UpdateBuildpathWizard"; //$NON-NLS-1$

	public UpdateBuildpathWizard(IPluginModelBase[] models, IPluginModelBase[] selected) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setWindowTitle(PDEUIMessages.UpdateBuildpathWizard_wtitle);
		setNeedsProgressMonitor(true);
		this.fSelected = selected;
		this.fUnupdated = models;
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	@Override
	public boolean performFinish() {
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return false;

		Object[] finalSelected = page1.getSelected();
		page1.storeSettings();
		IPluginModelBase[] modelArray = new IPluginModelBase[finalSelected.length];
		System.arraycopy(finalSelected, 0, modelArray, 0, finalSelected.length);
		Job j = new UpdateClasspathJob(modelArray);
		j.setUser(true);
		j.schedule();
		return true;
	}

	@Override
	public void addPages() {
		page1 = new UpdateBuildpathWizardPage(fUnupdated, fSelected);
		addPage(page1);
	}
}
