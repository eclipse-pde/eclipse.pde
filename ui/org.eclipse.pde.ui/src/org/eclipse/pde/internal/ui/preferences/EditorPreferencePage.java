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
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class EditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage, IPreferenceConstants {

	public EditorPreferencePage() {
		super(GRID);
		setPreferenceStore(PDEPlugin.getDefault().getPreferenceStore());
	}
	
	protected void createFieldEditors() {
		addLabel(PDEUIMessages.EditorPreferencePage_colorSettings, 2); 
		addSourceColorFields();
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.EDITOR_PREFERENCE_PAGE);
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
				PDEUIMessages.EditorPreferencePage_text, 
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_PROC_INSTR,
				PDEUIMessages.EditorPreferencePage_proc, 
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_STRING,
				PDEUIMessages.EditorPreferencePage_string, 
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_TAG,
				PDEUIMessages.EditorPreferencePage_tag, 
				getFieldEditorParent()));
		addField(
			new ColorFieldEditor(
				IPDEColorConstants.P_XML_COMMENT,
				PDEUIMessages.EditorPreferencePage_comment, 
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
