package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;

public class EditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public EditorPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString("EditorPreferencePage.desc")); //$NON-NLS-1$
	}

	protected void createFieldEditors() {
		addSourceColorFields();
	}

	private void addSourceColorFields() {
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_DEFAULT,
				PDEPlugin.getResourceString("EditorPreferencePage.text"), //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_PROC_INSTR,
				PDEPlugin.getResourceString("EditorPreferencePage.proc"), //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_STRING,
				PDEPlugin.getResourceString("EditorPreferencePage.string"), //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_TAG,
				PDEPlugin.getResourceString("EditorPreferencePage.tag"), //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_XML_COMMENT,
				PDEPlugin.getResourceString("EditorPreferencePage.comment"), //$NON-NLS-1$
				getFieldEditorParent()));
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