/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.launching.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.osgi.service.prefs.BackingStoreException;

public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Button fUseID;
	private Button fUseName;
	private Button fAutoManage;
	private Button fOverwriteBuildFiles;
	private Button fShowSourceBundles;
	private Button fPromptOnRemove;
	private Button fAddToJavaSearch;

	public MainPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEUIMessages.Preferences_MainPage_Description);
	}

	protected Control createContents(Composite parent) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();

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
		fAutoManage.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE));

		group = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_exportingGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fOverwriteBuildFiles = new Button(group, SWT.CHECK);
		fOverwriteBuildFiles.setText(PDEUIMessages.MainPreferencePage_promptBeforeOverwrite);
		fOverwriteBuildFiles.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT)));

		group = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_sourceGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fShowSourceBundles = new Button(group, SWT.CHECK);
		fShowSourceBundles.setText(PDEUIMessages.MainPreferencePage_showSourceBundles);
		fShowSourceBundles.setSelection(store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES));

		group = SWTFactory.createGroup(composite, PDEUIMessages.MainPreferencePage_targetDefinitionsGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fPromptOnRemove = new Button(group, SWT.CHECK);
		fPromptOnRemove.setText(PDEUIMessages.MainPreferencePage_promtBeforeRemove);
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		fPromptOnRemove.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				PDEPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET, fPromptOnRemove.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS);

			}

		});

		fAddToJavaSearch = new Button(group, SWT.CHECK);
		fAddToJavaSearch.setText(PDEUIMessages.MainPreferencePage_addToJavaSearch);
		fAddToJavaSearch.setSelection(store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH));
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
		store.setValue(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT, fOverwriteBuildFiles.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS);
		store.setValue(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES, fShowSourceBundles.getSelection());

		boolean synchJavaSearch = fAddToJavaSearch.getSelection();
		if (store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH) != synchJavaSearch) {
			store.setValue(IPreferenceConstants.ADD_TO_JAVA_SEARCH, synchJavaSearch);
			if (synchJavaSearch) {
				try {
					AddToJavaSearchJob.synchWithTarget(TargetPlatformService.getDefault().getWorkspaceTargetHandle().getTargetDefinition());
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}

		PDEPlugin.getDefault().getPreferenceManager().savePluginPreferences();

		// write AUTO_MANAGE setting to pde.launching instance scope 
		IEclipsePreferences instancePrefs = new InstanceScope().getNode(IPDEConstants.PLUGIN_ID);
		IEclipsePreferences defaultPrefs = new DefaultScope().getNode(IPDEConstants.PLUGIN_ID);
		if (defaultPrefs.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, false) == fAutoManage.getSelection()) {
			instancePrefs.remove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE);
		} else {
			instancePrefs.putBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, fAutoManage.getSelection());
		}
		try {
			instancePrefs.flush();
		} catch (BackingStoreException e) {
			PDEPlugin.log(e);
		}

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
		fShowSourceBundles.setSelection(false);
		fPromptOnRemove.setSelection(true);

		fAddToJavaSearch.setSelection(store.getDefaultBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS.equals(PDEPlugin.getDefault().getPreferenceManager().getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		super.setVisible(visible);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}
