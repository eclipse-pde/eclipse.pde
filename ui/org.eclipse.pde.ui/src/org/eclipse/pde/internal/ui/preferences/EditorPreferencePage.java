package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.preference.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class EditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	public static final String P_USE_SOURCE_PAGE = "useSourcePage";
	public static final String P_ASK_DEFAULT_PAGE = "askDefaultPage";

	public EditorPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString("EditorPreferencePage.desc")); //$NON-NLS-1$
	}
	
	private static IPreferenceStore initializeDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(P_USE_SOURCE_PAGE, false);
		store.setDefault(P_ASK_DEFAULT_PAGE, true);
		return store;
	}

	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(P_USE_SOURCE_PAGE, 
				PDEPlugin.getResourceString("EditorPreferencePage.useSourcePage"),
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(P_ASK_DEFAULT_PAGE, 
				PDEPlugin.getResourceString("EditorPreferencePage.askDefaultPage"),
				getFieldEditorParent()));
		addLabel("", 2);
		addLabel(PDEPlugin.getResourceString("EditorPreferencePage.colorSettings"), 2);
		addSourceColorFields();
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.EDITOR_PREFERENCE_PAGE);
	}
	
	public static boolean getUseSourcePage() {
		IPreferenceStore store = initializeDefaults();
		return store.getBoolean(P_USE_SOURCE_PAGE);
	}
	
	public static boolean getAskDefaultPage() {
		IPreferenceStore store = initializeDefaults();
		return store.getBoolean(P_ASK_DEFAULT_PAGE);
	}
	
	private void addLabel(String text, int span) {
		Label label = new Label(getFieldEditorParent(), SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
		label.setText(text);
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