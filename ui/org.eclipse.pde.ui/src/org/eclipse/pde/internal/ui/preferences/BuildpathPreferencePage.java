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

import org.eclipse.swt.widgets.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;

public class BuildpathPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IPreferenceConstants {
	private static final String KEY_PLUGIN_PROJECT_UPDATE =
		"Preferences.BuildpathPage.pluginProjectUpdate";
	private static final String KEY_FRAGMENT_PROJECT_UPDATE =
		"Preferences.BuildpathPage.fragmentProjectUpdate";
	private static final String KEY_MANIFEST_UPDATE =
		"Preferences.BuildpathPage.manifestUpdate";
	private static final String KEY_BUILD_PROPERTIES_UPDATE =
		"Preferences.BuildpathPage.buildPropertiesUpdate";		
	private static final String KEY_DESCRIPTION =
		"Preferences.BuildpathPage.description";
	private static final String KEY_CLASSPATH_CONTAINERS =
		"Preferences.BuildpathPage.classpathContainers";


	public BuildpathPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString(KEY_DESCRIPTION));
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
				PROP_BUILD_PROPERTIES_UPDATE,
				PDEPlugin.getResourceString(KEY_BUILD_PROPERTIES_UPDATE),
				getFieldEditorParent());
		addField(editor);
		new Label(getFieldEditorParent(), SWT.NULL);
		editor =
			new BooleanFieldEditor(
				PROP_CLASSPATH_CONTAINERS,
				PDEPlugin.getResourceString(KEY_CLASSPATH_CONTAINERS),
				getFieldEditorParent());
		addField(editor);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.BUILDPATH_PREFERENCE_PAGE);
	}

	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}

	public boolean performOk() {
		boolean value = super.performOk();
		PDEPlugin.getDefault().savePluginPreferences();
		return value;
	}
}
