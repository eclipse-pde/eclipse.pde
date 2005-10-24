/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class MainPreferencePage extends PreferencePage
	implements IWorkbenchPreferencePage, IPreferenceConstants {
	private Button fUseID;
	private Button fUseName;
	private Button fRemoveImport;
	private Button fOptionalImport;

	
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
		
		Group group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		group.setText(PDEUIMessages.Preferences_MainPage_showObjects);
		group.setLayout(new GridLayout());
		
		fUseID = new Button(group, SWT.RADIO);
		fUseID.setText(PDEUIMessages.Preferences_MainPage_useIds);
		
		fUseName = new Button(group, SWT.RADIO);
		fUseName.setText(PDEUIMessages.Preferences_MainPage_useFullNames);
		
		if (store.getString(PROP_SHOW_OBJECTS).equals(VALUE_USE_IDS)) {
			fUseID.setSelection(true);
		} else {
			fUseName.setSelection(true);
		}
		
		Group group2 = new Group(composite, SWT.None);
		group2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group2.setText(PDEUIMessages.MainPreferencePage_ManifestOranizationTitle);
		group2.setLayout(new GridLayout());
		
		Label label = new Label(group2, SWT.LEFT);
		label.setText(PDEUIMessages.MainPreferencePage_ResolveImports);
		
		fRemoveImport = new Button(group2, SWT.RADIO);
		fRemoveImport.setText(PDEUIMessages.MainPreferencePage_RemoveImport);
		GridData gd = new GridData();
		gd.horizontalIndent = 20;
		fRemoveImport.setLayoutData(gd);
		
		fOptionalImport = new Button(group2, SWT.RADIO);
		fOptionalImport.setText(PDEUIMessages.MainPreferencePage_ImportOptional);
		gd = new GridData();
		gd.horizontalIndent = 20;
		fOptionalImport.setLayoutData(gd);
		
		if (store.getString(PROP_RESOLVE_IMPORTS).equals(VALUE_REMOVE_IMPORT)) {
			fRemoveImport.setSelection(true);
		} else {
			fOptionalImport.setSelection(true);
		}
		
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
			store.setValue(PROP_SHOW_OBJECTS, VALUE_USE_IDS);
		} else {
			store.setValue(PROP_SHOW_OBJECTS, VALUE_USE_NAMES);
		}
		
		if (fRemoveImport.getSelection()) {
			store.setValue(PROP_RESOLVE_IMPORTS, VALUE_REMOVE_IMPORT);
		} else {
			store.setValue(PROP_RESOLVE_IMPORTS, VALUE_IMPORT_OPTIONAL);
		}
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	protected void performDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (store.getDefaultString(PROP_SHOW_OBJECTS).equals(VALUE_USE_IDS)) {
			fUseID.setSelection(true);
			fUseName.setSelection(false);
		} else {
			fUseID.setSelection(false);
			fUseName.setSelection(true);
		}
		if (store.getDefaultString(PROP_RESOLVE_IMPORTS).equals(VALUE_REMOVE_IMPORT)) {
			fRemoveImport.setSelection(true);
			fOptionalImport.setSelection(false);
		} else {
			fRemoveImport.setSelection(false);
			fOptionalImport.setSelection(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
}
