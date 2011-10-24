/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.pde.core.target.ITargetHandle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.BaseBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This is the top level preference page for PDE.  It contains a random assortment of preferences that don't belong to other pages.
 *
 */
public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final class DefaultRuntimeWorkspaceBlock extends BaseBlock {

		DefaultRuntimeWorkspaceBlock() {
			super(null);
		}

		public void createControl(Composite parent) {
			Group group = SWTFactory.createGroup(parent, PDEUIMessages.MainPreferencePage_runtimeWorkspaceGroup, 2, 1, GridData.FILL_HORIZONTAL);
			Composite radios = SWTFactory.createComposite(group, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);

			fRuntimeWorkspaceLocationRadio = new Button(radios, SWT.RADIO);
			fRuntimeWorkspaceLocationRadio.setText(PDEUIMessages.MainPreferencePage_runtimeWorkspace_asLocation);
			fRuntimeWorkspaceLocationRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			fRuntimeWorkspaceLocationRadio.setSelection(true);

			fRuntimeWorkspacesContainerRadio = new Button(radios, SWT.RADIO);
			fRuntimeWorkspacesContainerRadio.setText(PDEUIMessages.MainPreferencePage_runtimeWorkspace_asContainer);
			fRuntimeWorkspacesContainerRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			createText(group, PDEUIMessages.WorkspaceDataBlock_location, 0);
			((GridData) fLocationText.getLayoutData()).widthHint = 200;
			fRuntimeWorkspaceLocation = fLocationText;

			Composite buttons = SWTFactory.createComposite(group, 3, 2, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL, 0, 0);
			createButtons(buttons, new String[] {PDEUIMessages.MainPreferencePage_runtimeWorkspace_workspace, PDEUIMessages.MainPreferencePage_runtimeWorkspace_fileSystem, PDEUIMessages.MainPreferencePage_runtimeWorkspace_variables});
		}

		protected String getName() {
			return PDEUIMessages.WorkspaceDataBlock_name;
		}

		protected boolean isFile() {
			return false;
		}
	}

	private final class DefaultJUnitWorkspaceBlock extends BaseBlock {

		DefaultJUnitWorkspaceBlock() {
			super(null);
		}

		public void createControl(Composite parent) {
			Group group = SWTFactory.createGroup(parent, PDEUIMessages.MainPreferencePage_junitWorkspaceGroup, 2, 1, GridData.FILL_HORIZONTAL);
			Composite radios = SWTFactory.createComposite(group, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);

			fJUnitWorkspaceLocationRadio = new Button(radios, SWT.RADIO);
			fJUnitWorkspaceLocationRadio.setText(PDEUIMessages.MainPreferencePage_junitWorkspace_asLocation);
			fJUnitWorkspaceLocationRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			fJUnitWorkspaceLocationRadio.setSelection(true);

			fJUnitWorkspacesContainerRadio = new Button(radios, SWT.RADIO);
			fJUnitWorkspacesContainerRadio.setText(PDEUIMessages.MainPreferencePage_junitWorkspace_asContainer);
			fJUnitWorkspacesContainerRadio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			createText(group, PDEUIMessages.WorkspaceDataBlock_location, 0);
			((GridData) fLocationText.getLayoutData()).widthHint = 200;
			fJUnitWorkspaceLocation = fLocationText;

			Composite buttons = SWTFactory.createComposite(group, 3, 2, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL, 0, 0);
			createButtons(buttons, new String[] {PDEUIMessages.MainPreferencePage_junitWorkspace_workspace, PDEUIMessages.MainPreferencePage_junitWorkspace_fileSystem, PDEUIMessages.MainPreferencePage_junitWorkspace_variables});
		}

		protected String getName() {
			return PDEUIMessages.DefaultJUnitWorkspaceBlock_name;
		}

		protected boolean isFile() {
			return false;
		}
	}

	public static final String ID = "org.eclipse.pde.ui.MainPreferencePage"; //$NON-NLS-1$

	private Button fUseID;
	private Button fUseName;
	private Button fAutoManage;
	private Button fOverwriteBuildFiles;
	private Button fShowSourceBundles;
	private Button fPromptOnRemove;
	private Button fAddToJavaSearch;

	private Text fRuntimeWorkspaceLocation;
	private Button fRuntimeWorkspaceLocationRadio;
	private Button fRuntimeWorkspacesContainerRadio;

	private Text fJUnitWorkspaceLocation;
	private Button fJUnitWorkspaceLocationRadio;
	private Button fJUnitWorkspacesContainerRadio;

	public MainPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEUIMessages.Preferences_MainPage_Description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();

		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		((GridLayout) composite.getLayout()).verticalSpacing = 15;
		((GridLayout) composite.getLayout()).marginTop = 15;

		Composite optionComp = SWTFactory.createComposite(composite, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);

		fOverwriteBuildFiles = new Button(optionComp, SWT.CHECK);
		fOverwriteBuildFiles.setText(PDEUIMessages.MainPreferencePage_promptBeforeOverwrite);
		fOverwriteBuildFiles.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.OVERWRITE_BUILD_FILES_ON_EXPORT)));

		fAutoManage = new Button(optionComp, SWT.CHECK);
		fAutoManage.setText(PDEUIMessages.MainPreferencePage_updateStale);
		fAutoManage.setSelection(launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE));

		fPromptOnRemove = new Button(optionComp, SWT.CHECK);
		fPromptOnRemove.setText(PDEUIMessages.MainPreferencePage_promtBeforeRemove);
		fPromptOnRemove.setSelection(!MessageDialogWithToggle.ALWAYS.equals(store.getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET)));
		fPromptOnRemove.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				PDEPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET, fPromptOnRemove.getSelection() ? MessageDialogWithToggle.PROMPT : MessageDialogWithToggle.ALWAYS);

			}

		});

		fAddToJavaSearch = new Button(optionComp, SWT.CHECK);
		fAddToJavaSearch.setText(PDEUIMessages.MainPreferencePage_addToJavaSearch);
		fAddToJavaSearch.setSelection(store.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH));

		Group group = SWTFactory.createGroup(composite, PDEUIMessages.Preferences_MainPage_showObjects, 2, 1, GridData.FILL_HORIZONTAL);
		fUseID = new Button(group, SWT.RADIO);
		fUseID.setText(PDEUIMessages.Preferences_MainPage_useIds);

		fUseName = new Button(group, SWT.RADIO);
		fUseName.setText(PDEUIMessages.Preferences_MainPage_useFullNames);

		fShowSourceBundles = SWTFactory.createCheckButton(group, PDEUIMessages.MainPreferencePage_showSourceBundles, null, store.getBoolean(IPreferenceConstants.PROP_SHOW_SOURCE_BUNDLES), 2);

		if (store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_IDS)) {
			fUseID.setSelection(true);
		} else {
			fUseName.setSelection(true);
		}

		new DefaultRuntimeWorkspaceBlock().createControl(composite);
		fRuntimeWorkspaceLocation.setText(launchingStore.getString(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION));
		boolean runtimeLocationIsContainer = launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER);
		fRuntimeWorkspaceLocationRadio.setSelection(!runtimeLocationIsContainer);
		fRuntimeWorkspacesContainerRadio.setSelection(runtimeLocationIsContainer);

		new DefaultJUnitWorkspaceBlock().createControl(composite);
		fJUnitWorkspaceLocation.setText(launchingStore.getString(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION));
		boolean jUnitLocationIsContainer = launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
		fJUnitWorkspaceLocationRadio.setSelection(!jUnitLocationIsContainer);
		fJUnitWorkspacesContainerRadio.setSelection(jUnitLocationIsContainer);

		return composite;
	}

	public void createControl(Composite composite) {
		super.createControl(composite);
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
			try {
				if (synchJavaSearch) {
					ITargetHandle target = TargetPlatformService.getDefault().getWorkspaceTargetHandle();
					if (target != null) {
						AddToJavaSearchJob.synchWithTarget(target.getTargetDefinition());
					}
				} else {
					AddToJavaSearchJob.clearAll();
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}

		PDEPlugin.getDefault().getPreferenceManager().savePluginPreferences();

		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, fAutoManage.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION, fRuntimeWorkspaceLocation.getText());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER, fRuntimeWorkspacesContainerRadio.getSelection());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION, fJUnitWorkspaceLocation.getText());
		launchingStore.setValueOrRemove(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER, fJUnitWorkspacesContainerRadio.getSelection());
		try {
			launchingStore.flush();
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

		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		boolean runtimeLocationIsContainer = launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER);
		fRuntimeWorkspaceLocationRadio.setSelection(!runtimeLocationIsContainer);
		fRuntimeWorkspacesContainerRadio.setSelection(runtimeLocationIsContainer);
		fRuntimeWorkspaceLocation.setText(launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_RUNTIME_WORKSPACE_LOCATION));

		boolean jUnitLocationIsContainer = launchingStore.getDefaultBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER);
		fJUnitWorkspaceLocationRadio.setSelection(!jUnitLocationIsContainer);
		fJUnitWorkspacesContainerRadio.setSelection(jUnitLocationIsContainer);
		fJUnitWorkspaceLocation.setText(launchingStore.getDefaultString(ILaunchingPreferenceConstants.PROP_JUNIT_WORKSPACE_LOCATION));
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
