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
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public abstract class BaseExportWizard extends Wizard implements IExportWizard, ILaunchingPreferenceConstants {

	protected IStructuredSelection fSelection;

	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle(PDEUIMessages.BaseExportWizard_wtitle);
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public IStructuredSelection getSelection() {
		return fSelection;
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		String name = getSettingsSectionName();
		IDialogSettings settings = master.getSection(name);
		if (settings == null)
			settings = master.addNewSection(name);
		return settings;
	}

	protected abstract String getSettingsSectionName();

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fSelection = selection;
	}

	@Override
	public boolean performFinish() {
		saveSettings();
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return false;

		if (!performPreliminaryChecks())
			return false;

		if (!confirmDelete())
			return false;

		scheduleExportJob();
		return true;
	}

	protected void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			((AbstractExportWizardPage) page).saveSettings(settings);
		}
	}

	protected abstract boolean performPreliminaryChecks();

	protected abstract boolean confirmDelete();

	protected abstract void scheduleExportJob();

}
