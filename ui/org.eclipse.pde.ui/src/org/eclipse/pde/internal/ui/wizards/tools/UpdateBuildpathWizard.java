/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.dialogs.IDialogSettings;

public class UpdateBuildpathWizard extends Wizard {
	private UpdateBuildpathWizardPage page1;
	private IPluginModelBase[] fSelected;
	private IPluginModelBase[] fUnupdated;
	private static final String STORE_SECTION = "UpdateBuildpathWizard"; //$NON-NLS-1$
	private static final String KEY_WTITLE = "UpdateBuildpathWizard.wtitle"; //$NON-NLS-1$

	public UpdateBuildpathWizard(IPluginModelBase[] models, IPluginModelBase[] selected) {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
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
	
	public boolean performFinish() {
		Object [] finalSelected = page1.getSelected();
		page1.storeSettings();
		IPluginModelBase [] modelArray = new IPluginModelBase[finalSelected.length];
		System.arraycopy(finalSelected, 0, modelArray, 0, finalSelected.length);
		Job j = new UpdateClasspathJob(modelArray);
		j.setUser(true);
		j.schedule();
		return true;
	}
	
	public void addPages() {
		page1 = new UpdateBuildpathWizardPage(fUnupdated, fSelected);
		addPage(page1);
	}
}
