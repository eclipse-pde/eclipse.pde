package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.net.URL;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.jdt.core.*;


public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private static final String KEY_DESCRIPTION = "Preferences.MainPage.Description";
	private static final String KEY_NO_PDE_NATURE = "Preferences.MainPage.noPDENature";
	private static final String KEY_SHOW_OBJECTS = "Preferences.MainPage.showObjects";
	private static final String KEY_USE_IDS = "Preferences.MainPage.useIds";
	private static final String KEY_USE_FULL_NAMES = "Preferences.MainPage.useFullNames";
	private static final String KEY_BUILD_SCRIPT_NAME = "Preferences.MainPage.buildScriptName";

	public static final String PROP_NO_PDE_NATURE ="Preferences.MainPage.noPDENature";
	public static final String PROP_SHOW_OBJECTS = "Preferences.MainPage.showObjects";
	public static final String VALUE_USE_IDS ="useIds";
	public static final String VALUE_USE_NAMES = "useNames";
	public static final String PROP_BUILD_SCRIPT_NAME = "Preferences.MainPage.buildScriptName";

public MainPreferencePage() {
	super(GRID);
	setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
	setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	initializeDefaults();
}

private static void initializeDefaults() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_NO_PDE_NATURE, true);
	store.setDefault(PROP_SHOW_OBJECTS, VALUE_USE_IDS);
	store.setDefault(PROP_BUILD_SCRIPT_NAME, "build.xml");
}

protected void createFieldEditors() {
	/*
	BooleanFieldEditor editor =
		new BooleanFieldEditor(
			PROP_NO_PDE_NATURE,
			PDEPlugin.getResourceString(KEY_NO_PDE_NATURE),
			getFieldEditorParent());
	addField(editor);
	
	new Label(getFieldEditorParent(), SWT.NULL);
	*/
	
	RadioGroupFieldEditor reditor =
		new RadioGroupFieldEditor(
			PROP_SHOW_OBJECTS,
			PDEPlugin.getResourceString(KEY_SHOW_OBJECTS),
			1,
			new String [][] {
				{ PDEPlugin.getResourceString(KEY_USE_IDS),
				VALUE_USE_IDS },
				{PDEPlugin.getResourceString(KEY_USE_FULL_NAMES),
				VALUE_USE_NAMES }},
				getFieldEditorParent());
	addField(reditor);
	StringFieldEditor textEditor = 
		new StringFieldEditor(
			PROP_BUILD_SCRIPT_NAME,
			PDEPlugin.getResourceString(KEY_BUILD_SCRIPT_NAME),
			getFieldEditorParent());
	addField(textEditor);
}

public static boolean isNoPDENature() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	initializeDefaults();
	return store.getBoolean(PROP_NO_PDE_NATURE);
}

public static boolean isFullNameModeEnabled() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	initializeDefaults();
	return store.getString(PROP_SHOW_OBJECTS).equals(VALUE_USE_NAMES);
}

public static String getBuildScriptName() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	initializeDefaults();
	return store.getString(PROP_BUILD_SCRIPT_NAME);
}

public boolean performOk() {
	boolean value = super.performOk();
	PDEPlugin.getDefault().savePluginPreferences();
	return value;
}


/**
 * Initializes this preference page using the passed desktop.
 *
 * @param desktop the current desktop
 */
public void init(IWorkbench workbench) {
}
}
