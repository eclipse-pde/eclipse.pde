/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.pde.internal.ui.PDEUIMessages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Button fUseID;
	private Button fUseName;
	private Button fAutoManage;
	private Button fOverwriteBuildFiles;

	public MainPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEUIMessages.Preferences_MainPage_Description);
	}

	protected Control createContents(Composite parent) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		composite.setLayout(layout);

		Group group = SWTFactory.createGroup(composite, PDEUIMessages.Preferences_MainPage_showObjects, 1, 1, GridData.FILL_HORIZONTAL);

		fUseID = new Button(group, SWT.RADIO);
		fUseID.setText(PDEUIMessages.Preferences_MainPage_useIds);

		fUseName = new Button(group, SWT.RADIO);
		fUseName.setText(PDEUIMessages.Preferences_MainPage_useFullNames);

		if (store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_IDS)) {
			fUseID.setSelection(true);
		} else {
			fUseName.setSelection(true);
		}

		group = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_group2, 1, 1, GridData.FILL_HORIZONTAL);

		fAutoManage = new Button(group, SWT.CHECK);
		fAutoManage.setText(PDEUIMessages.MainPreferencePage_updateStale);
		fAutoManage.setSelection(store.getBoolean(IPreferenceConstants.PROP_AUTO_MANAGE));

		group = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_exportingGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fOverwriteBuildFiles = new Button(group, SWT.CHECK);
		fOverwriteBuildFiles.setText(PDEUIMessages.MainPreferencePage_promptBeforeOverwrite);
		fOverwriteBuildFiles.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT)));

		return composite;
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.MAIN_PREFERENCE_PAGE);
	}

	public boolean performOk() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (fUseID.getSelection()) {
			store.setValue(IPreferenceConstants.PROP_SHOW_OBJECTS, IPreferenceConstants.VALUE_USE_IDS);
		} else {
			store.setValue(IPreferenceConstants.PROP_SHOW_OBJECTS, IPreferenceConstants.VALUE_USE_NAMES);
		}
		store.setValue(IPreferenceConstants.PROP_AUTO_MANAGE, fAutoManage.getSelection());
		store.setValue(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT, fOverwriteBuildFiles.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS);
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}

	protected void performDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (store.getDefaultString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_IDS)) {
			fUseID.setSelection(true);
			fUseName.setSelection(false);
		} else {
			fUseID.setSelection(false);
			fUseName.setSelection(true);
		}
		fAutoManage.setSelection(false);
		fOverwriteBuildFiles.setSelection(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}
