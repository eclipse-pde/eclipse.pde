package org.eclipse.pde.internal.preferences;
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
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.jdt.core.*;


public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private static final String KEY_DESCRIPTION = "Preferences.MainPage.Description";
	private static final String KEY_NO_PDE_NATURE = "Preferences.MainPage.noPDENature";
	private static final String KEY_SHOW_OBJECTS = "Preferences.MainPage.showObjects";
	private static final String KEY_USE_IDS = "Preferences.MainPage.useIds";
	private static final String KEY_USE_FULL_NAMES = "Preferences.MainPage.useFullNames";

	public static final String PROP_NO_PDE_NATURE ="Preferences.MainPage.noPDENature";
	public static final String PROP_SHOW_OBJECTS = "Preferences.MainPage.showObjects";
	public static final String VALUE_USE_IDS ="useIds";
	public static final String VALUE_USE_NAMES = "useNames";

public MainPreferencePage() {
	super(GRID);
	setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
	setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	initializeDefaults();
}

private static void initializeDefaults() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_NO_PDE_NATURE, true);
	store.setDefault(PROP_SHOW_OBJECTS, VALUE_USE_NAMES);
}

protected void createFieldEditors() {
	BooleanFieldEditor editor =
		new BooleanFieldEditor(
			PROP_NO_PDE_NATURE,
			PDEPlugin.getResourceString(KEY_NO_PDE_NATURE),
			getFieldEditorParent());
	addField(editor);
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

/**
 * Initializes this preference page using the passed desktop.
 *
 * @param desktop the current desktop
 */
public void init(IWorkbench workbench) {
}
}
