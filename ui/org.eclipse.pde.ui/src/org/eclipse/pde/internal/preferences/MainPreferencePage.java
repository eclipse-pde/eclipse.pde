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


public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public static final String KEY_DESCRIPTION = "Preferences.MainPage.Description";
	public static final String KEY_NO_PDE_NATURE = "Preferences.MainPage.noPDENature";

	public static final String PROP_NO_PDE_NATURE ="Preferences.MainPage.noPDENature";

public MainPreferencePage() {
	super(GRID);
	setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
	setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	initializeDefaults();
}

private static void initializeDefaults() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	store.setDefault(PROP_NO_PDE_NATURE, true);
}

protected void createFieldEditors() {
	BooleanFieldEditor editor =
		new BooleanFieldEditor(
			PROP_NO_PDE_NATURE,
			PDEPlugin.getResourceString(KEY_NO_PDE_NATURE),
			getFieldEditorParent());
	addField(editor);
}

public static boolean isNoPDENature() {
	IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
	initializeDefaults();
	return store.getBoolean(PROP_NO_PDE_NATURE);
}

/**
 * Initializes this preference page using the passed desktop.
 *
 * @param desktop the current desktop
 */
public void init(IWorkbench workbench) {
}
}
