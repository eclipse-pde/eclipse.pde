/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;

public class PortabilityChoiceCellEditor extends DialogCellEditor {
	private static final String KEY_TITLE =
		"FeatureEditor.PortabilityChoicesDialog.title"; //$NON-NLS-1$
	private Label label;
	private Choice[] choices;

	public PortabilityChoiceCellEditor(
		Composite parent,
		Choice[] choices) {
		super(parent);
		this.choices = choices;
	}
	protected Control createContents(Composite cell) {
		label = new Label(cell, SWT.LEFT);
		label.setFont(cell.getFont());
		label.setBackground(cell.getBackground());
		return label;
	}
	protected Object openDialogBox(Control cellEditorWindow) {
		String value = (String) getValue();

		PortabilityChoicesDialog dialog =
			new PortabilityChoicesDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				choices,
				value);
		dialog.create();
		dialog.getShell().setText(PDEPlugin.getResourceString(KEY_TITLE));
		//dialog.getShell().setSize(300, 400);
		int result = dialog.open();
		if (result == PortabilityChoicesDialog.OK) {
			return dialog.getValue();
		}
		return value;
	}
	protected void updateContents(Object value) {
		if (value != null)
			label.setText(value.toString());
		else
			label.setText(""); //$NON-NLS-1$
	}
}
