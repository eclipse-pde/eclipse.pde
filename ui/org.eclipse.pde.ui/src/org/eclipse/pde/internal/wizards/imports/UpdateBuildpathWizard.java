/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.base.model.plugin.IPluginModelBase;
import org.eclipse.pde.internal.wizards.imports.UpdateClasspathAction;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.dialogs.IDialogSettings;

public class UpdateBuildpathWizard extends Wizard {
	private UpdateBuildpathWizardPage page1;
	private IPluginModelBase [] selected;
	private static final String STORE_SECTION = "UpdateBuildpathWizard";

	public UpdateBuildpathWizard(IPluginModelBase[] selected) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
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
		UpdateClasspathAction.run(false, getContainer(), modelArray);
		return true;
	}
	
	public void addPages() {
		page1 = new UpdateBuildpathWizardPage(selected);
		addPage(page1);
	}
}