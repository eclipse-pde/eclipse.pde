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

import org.eclipse.jface.dialogs.Dialog;
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
	implements IWorkbenchPreferencePage, IPreferenceConstants {

	public EditorPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
		setDescription(PDEPlugin.getResourceString("EditorPreferencePage.desc")); //$NON-NLS-1$
	}
	
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(P_USE_SOURCE_PAGE, 
				PDEPlugin.getResourceString("EditorPreferencePage.useSourcePage"),
				getFieldEditorParent()));
		addLabel("", 2);
		addLabel(PDEPlugin.getResourceString("EditorPreferencePage.colorSettings"), 2);
		addSourceColorFields();
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.EDITOR_PREFERENCE_PAGE);
	}
	
	public static boolean getUseSourcePage() {
		return PDEPlugin.getDefault().getPreferenceStore().getBoolean(P_USE_SOURCE_PAGE);
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
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}

	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}
}
