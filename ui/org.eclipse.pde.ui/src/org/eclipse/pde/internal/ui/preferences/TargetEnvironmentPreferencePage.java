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
import org.eclipse.core.boot.BootLoader;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.internal.ui.util.Choice;

/**
 */
public class TargetEnvironmentPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IEnvironmentVariables {
	private static final String KEY_DESCRIPTION =
		"Preferences.TargetEnvironmentPage.Description";
	public static final String KEY_OS =
		"Preferences.TargetEnvironmentPage.os";
	public static final String KEY_WS =
		"Preferences.TargetEnvironmentPage.ws";
	public static final String KEY_NL =
		"Preferences.TargetEnvironmentPage.nl";
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
		addField(createComboFieldEditor(OS, PDEPlugin.getResourceString(KEY_OS), TargetPlatform.getOSChoices()));
		addField(createComboFieldEditor(WS, PDEPlugin.getResourceString(KEY_WS), TargetPlatform.getWSChoices()));
		addField(createComboFieldEditor(NL, PDEPlugin.getResourceString(KEY_NL), TargetPlatform.getNLChoices()));
		addField(createComboFieldEditor(ARCH, PDEPlugin.getResourceString(KEY_ARCH), TargetPlatform.getArchChoices()));
	}
	
	private FieldEditor createComboFieldEditor(String name, String label, Choice [] choices) {
		return new ComboFieldEditor(name, label, choices, getFieldEditorParent());
	}
	
	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}
}