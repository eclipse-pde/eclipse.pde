package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.PDEPlugin;

public class TypeRestrictionCellEditor extends DialogCellEditor {
	private Label label;
	private static final String KEY_TYPE_RESTRICTION_TITLE = "SchemaEditor.RestrictionDialog.wtitle";

protected TypeRestrictionCellEditor(Composite parent) {
	super(parent);
}
protected Control createContents(Composite cell) {
	label = new Label(cell, SWT.LEFT);
	label.setFont(cell.getFont());
	label.setBackground(cell.getBackground());
	return label;
}
protected Object openDialogBox(Control cellEditorWindow) {
	Object value = getValue();
	TypeRestrictionDialog dialog =
		new TypeRestrictionDialog(
			cellEditorWindow.getShell(),
			(ISchemaRestriction) value);
	dialog.create();
	dialog.getShell().setSize(300, 350);
	dialog.getShell().setText(PDEPlugin.getResourceString(KEY_TYPE_RESTRICTION_TITLE));
	int result = dialog.open();
	if (result == TypeRestrictionDialog.OK) {
		value = dialog.getValue();
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
