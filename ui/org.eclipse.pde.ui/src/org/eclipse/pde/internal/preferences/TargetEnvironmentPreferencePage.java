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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import java.lang.reflect.InvocationTargetException;

/**
 */
public class TargetEnvironmentPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	public static final String PROP_OS = "org.eclipse.pde.os";
	public static final String PROP_WS = "org.eclipse.pde.ws";
	public static final String PROP_NL = "org.eclipse.pde.nl";
	public static final String PROP_ARCH = "org.eclipse.pde.arch";
	private static final String KEY_DESCRIPTION =
		"Preferences.TargetEnvironmentPage.Description";
	private static final String KEY_OS =
		"Preferences.TargetEnvironmentPage.os";
	private static final String KEY_WS =
		"Preferences.TargetEnvironmentPage.ws";
	private static final String KEY_NL =
		"Preferences.TargetEnvironmentPage.nl";
	private static final String KEY_ARCH =
		"Preferences.TargetEnvironmentPage.arch";

	public TargetEnvironmentPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
	}
	/**
	 */
	protected void createFieldEditors() {
		addField(createComboFieldEditor(PROP_OS, PDEPlugin.getResourceString(KEY_OS), getOSChoices()));
		addField(createComboFieldEditor(PROP_WS, PDEPlugin.getResourceString(KEY_WS), getWSChoices()));
		addField(createComboFieldEditor(PROP_NL, PDEPlugin.getResourceString(KEY_NL), getNLChoices()));
		addField(createComboFieldEditor(PROP_ARCH, PDEPlugin.getResourceString(KEY_ARCH), getArchChoices()));
	}
	
	private FieldEditor createComboFieldEditor(String name, String label, String [] choices) {
		return new ComboFieldEditor(name, label, choices, getFieldEditorParent());
	}
	
	private String [] getOSChoices() {
	}
	
	private String [] getWSChoices() {
	}
	
	private String [] getNLChoices() {
	}
	
	private String [] getArchChoices() {
	}
	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}
}