/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.wizards.imports.UpdateClasspathAction;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.dialogs.IDialogSettings;

public class UpdateBuildpathWizard extends Wizard {
	private UpdateBuildpathWizardPage page1;
	private IPluginModelBase [] selected;
	private static final String STORE_SECTION = "UpdateBuildpathWizard";
	private static final String KEY_WTITLE = "UpdateBuildpathWizard.wtitle";

	public UpdateBuildpathWizard(IPluginModelBase[] selected) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		setNeedsProgressMonitor(true);
		this.selected = selected;
	}
	
	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}
	
	public boolean performFinish() {
		Object [] finalSelected = page1.getSelected();
		page1.storeSettings();
		
		IPluginModelBase [] modelArray = new IPluginModelBase[finalSelected.length];
		System.arraycopy(finalSelected, 0, modelArray, 0, finalSelected.length);
		UpdateClasspathAction.run(true, getContainer(), modelArray);
		return true;
	}
	
	public void addPages() {
		page1 = new UpdateBuildpathWizardPage(selected);
		addPage(page1);
	}
}