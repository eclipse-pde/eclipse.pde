package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;

public class PortabilityChoiceCellEditor extends DialogCellEditor {
	private static final String KEY_TITLE =
		"FeatureEditor.PortabilityChoicesDialog.title";
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
			label.setText("");
	}
}