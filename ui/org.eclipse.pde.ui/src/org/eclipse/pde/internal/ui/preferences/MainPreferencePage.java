/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.pde.internal.ui.*;

public class MainPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage, IPreferenceConstants {
	private static final String KEY_DESCRIPTION =
		"Preferences.MainPage.Description";
	private static final String KEY_SHOW_OBJECTS =
		"Preferences.MainPage.showObjects";
	private static final String KEY_USE_IDS = "Preferences.MainPage.useIds";
	private static final String KEY_USE_FULL_NAMES =
		"Preferences.MainPage.useFullNames";
	private static final String KEY_BUILD_SCRIPT_NAME =
		"Preferences.MainPage.buildScriptName";
	
	private static final String KEY_CODE_GENERATION =
		"Preferences.MainPage.codeGeneration";
	private static final String KEY_ADD_TODO = 
		"Preferences.MainPage.addTodo";


	private Button useID;
	private Button useName;
	private Text buildText;
	private Button addTodo;
	
	public MainPreferencePage() {
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	}

	protected Control createContents(Composite parent) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Group group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		group.setText(PDEPlugin.getResourceString(KEY_SHOW_OBJECTS));
		group.setLayout(new GridLayout());
		
		useID = new Button(group, SWT.RADIO);
		useID.setText(PDEPlugin.getResourceString(KEY_USE_IDS));
		
		useName = new Button(group, SWT.RADIO);
		useName.setText(PDEPlugin.getResourceString(KEY_USE_FULL_NAMES));
		
		if (store.getString(PROP_SHOW_OBJECTS).equals(VALUE_USE_IDS)) {
			useID.setSelection(true);
		} else {
			useName.setSelection(true);
		}
		
		Composite buildArea = new Composite(composite, SWT.NONE);
		buildArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		buildArea.setLayout(layout);
		
		Label label = new Label(buildArea, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_BUILD_SCRIPT_NAME));
		
		buildText = new Text(buildArea, SWT.BORDER);
		buildText.setText(store.getString(PROP_BUILD_SCRIPT_NAME));
		buildText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		group = new Group(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEPlugin.getResourceString(KEY_CODE_GENERATION));
		group.setLayout(new GridLayout());
		
		addTodo = new Button(group, SWT.CHECK);
		addTodo.setText(PDEPlugin.getResourceString(KEY_ADD_TODO));
		addTodo.setSelection(store.getBoolean(PROP_ADD_TODO));

		return composite;		
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.MAIN_PREFERENCE_PAGE);
	}	

	public static boolean isFullNameModeEnabled() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		return store.getString(PROP_SHOW_OBJECTS).equals(VALUE_USE_NAMES);
	}

	public static String getBuildScriptName() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		return store.getString(PROP_BUILD_SCRIPT_NAME);
	}
	
	public static boolean getAddTodo() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		return store.getString(PROP_ADD_TODO).equals("true");
	}	

	public boolean performOk() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (useID.getSelection()) {
			store.setValue(PROP_SHOW_OBJECTS, VALUE_USE_IDS);
		} else {
			store.setValue(PROP_SHOW_OBJECTS, VALUE_USE_NAMES);
		}
		store.setValue(PROP_BUILD_SCRIPT_NAME, buildText.getText());
		store.setValue(PROP_ADD_TODO, addTodo.getSelection()?"true":"false");
		
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	protected void performDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (store.getDefaultString(PROP_SHOW_OBJECTS).equals(VALUE_USE_IDS)) {
			useID.setSelection(true);
			useName.setSelection(false);
		} else {
			useID.setSelection(false);
			useName.setSelection(true);
		}
		buildText.setText(store.getDefaultString(PROP_BUILD_SCRIPT_NAME));
		addTodo.setSelection(store.getDefaultBoolean(PROP_ADD_TODO));
	}

	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}
}
