package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;

/**
 */
public class TargetEnvironmentPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IEnvironmentVariables {
	private static final String KEY_DESCRIPTION =
		"Preferences.TargetEnvironmentPage.Description";
	public static final String KEY_OS = "Preferences.TargetEnvironmentPage.os";
	public static final String KEY_WS = "Preferences.TargetEnvironmentPage.ws";
	public static final String KEY_NL = "Preferences.TargetEnvironmentPage.nl";
	public static final String KEY_ARCH =
		"Preferences.TargetEnvironmentPage.arch";

	public TargetEnvironmentPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
		TargetPlatform.initializeDefaults();
	}

	/**
	 */
	protected void createFieldEditors() {
		addField(
			createComboFieldEditor(
				OS,
				PDEPlugin.getResourceString(KEY_OS),
				TargetPlatform.getOSChoices()));
		addField(
			createComboFieldEditor(
				WS,
				PDEPlugin.getResourceString(KEY_WS),
				TargetPlatform.getWSChoices()));
		addField(
			createComboFieldEditor(
				NL,
				PDEPlugin.getResourceString(KEY_NL),
				TargetPlatform.getNLChoices()));
		addField(
			createComboFieldEditor(
				ARCH,
				PDEPlugin.getResourceString(KEY_ARCH),
				TargetPlatform.getArchChoices()));
	}

	private FieldEditor createComboFieldEditor(
		String name,
		String label,
		Choice[] choices) {
		return new ComboFieldEditor(
			name,
			label,
			choices,
			getFieldEditorParent());
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