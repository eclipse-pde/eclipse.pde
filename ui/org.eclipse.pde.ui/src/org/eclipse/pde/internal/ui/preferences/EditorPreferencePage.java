/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		setDescription(PDEUIMessages.EditorPreferencePage_desc); //$NON-NLS-1$
	}
	
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(P_USE_SOURCE_PAGE, 
				PDEUIMessages.EditorPreferencePage_useSourcePage, //$NON-NLS-1$
				getFieldEditorParent()));
		addLabel("", 2); //$NON-NLS-1$
		addLabel(PDEUIMessages.EditorPreferencePage_colorSettings, 2); //$NON-NLS-1$
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
				PDEUIMessages.EditorPreferencePage_text, //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_PROC_INSTR,
				PDEUIMessages.EditorPreferencePage_proc, //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_STRING,
				PDEUIMessages.EditorPreferencePage_string, //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_TAG,
				PDEUIMessages.EditorPreferencePage_tag, //$NON-NLS-1$
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_XML_COMMENT,
				PDEUIMessages.EditorPreferencePage_comment, //$NON-NLS-1$
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
