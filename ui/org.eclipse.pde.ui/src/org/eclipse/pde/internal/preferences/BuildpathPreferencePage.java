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
import org.eclipse.core.boot.BootLoader;
import org.eclipse.jdt.core.*;


public class BuildpathPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String KEY_PLUGIN_PROJECT_UPDATE ="Preferences.BuildpathPage.pluginProjectUpdate";
	public static final String KEY_FRAGMENT_PROJECT_UPDATE = "Preferences.BuildpathPage.fragmentProjectUpdate";
	public static final String KEY_MANIFEST_UPDATE = "Preferences.BuildpathPage.manifestUpdate";
	public static final String KEY_CONVERSION_UPDATE = "Preferences.BuildpathPage.conversionUpdate";
	public static final String KEY_DESCRIPTION = "Preferences.BuildpathPage.description";

	public static final String PROP_PLUGIN_PROJECT_UPDATE ="Preferences.BuildpathPage.pluginProjectUpdate";
	public static final String PROP_FRAGMENT_PROJECT_UPDATE = "Preferences.BuildpathPage.fragmentProjectUpdate";
	public static final String PROP_MANIFEST_UPDATE = "Preferences.BuildpathPage.manifestUpdate";
	public static final String PROP_CONVERSION_UPDATE = "Preferences.BuildpathPage.conversionUpdate";

public BuildpathPreferencePage() {
	super(GRID);
	setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
	setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	initializeDefaults();
}

private static void initializeDefaults() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_PLUGIN_PROJECT_UPDATE, true);
	store.setDefault(PROP_FRAGMENT_PROJECT_UPDATE, true);
	store.setDefault(PROP_MANIFEST_UPDATE, true);
	store.setDefault(PROP_CONVERSION_UPDATE, true);
}

protected void createFieldEditors() {
	BooleanFieldEditor editor =
		new BooleanFieldEditor(
			PROP_PLUGIN_PROJECT_UPDATE,
			PDEPlugin.getResourceString(KEY_PLUGIN_PROJECT_UPDATE),
			getFieldEditorParent());
	addField(editor);
	editor =
		new BooleanFieldEditor(
			PROP_FRAGMENT_PROJECT_UPDATE,
			PDEPlugin.getResourceString(KEY_FRAGMENT_PROJECT_UPDATE),
			getFieldEditorParent());
	addField(editor);
	editor =
		new BooleanFieldEditor(
			PROP_MANIFEST_UPDATE,
			PDEPlugin.getResourceString(KEY_MANIFEST_UPDATE),
			getFieldEditorParent());
	addField(editor);
	editor =
		new BooleanFieldEditor(
			PROP_CONVERSION_UPDATE,
			PDEPlugin.getResourceString(KEY_CONVERSION_UPDATE),
			getFieldEditorParent());
	addField(editor);
}

public static boolean isPluginProjectUpdate() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_PLUGIN_PROJECT_UPDATE, true);
	return store.getBoolean(PROP_PLUGIN_PROJECT_UPDATE);
}

public static boolean isFragmentProjectUpdate() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_FRAGMENT_PROJECT_UPDATE, true);
	return store.getBoolean(PROP_FRAGMENT_PROJECT_UPDATE);
}

public static boolean isManifestUpdate() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_MANIFEST_UPDATE, false);
	return store.getBoolean(PROP_MANIFEST_UPDATE);
}

public static boolean isConversionUpdate() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_CONVERSION_UPDATE, false);
	return store.getBoolean(PROP_CONVERSION_UPDATE);
}

/**
 * Initializes this preference page using the passed desktop.
 *
 * @param desktop the current desktop
 */
public void init(IWorkbench workbench) {
}
}
