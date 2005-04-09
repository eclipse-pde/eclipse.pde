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
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;

public class TypeRestrictionCellEditor extends DialogCellEditor {
	private Label label;
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
	SWTUtil.setDialogSize(dialog, 300, 350);
	dialog.getShell().setText(PDEUIMessages.RestrictionDialog_wtitle);
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
		label.setText(""); //$NON-NLS-1$
}
}
