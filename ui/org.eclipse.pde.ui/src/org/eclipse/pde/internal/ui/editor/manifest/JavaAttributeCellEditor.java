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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;

public class JavaAttributeCellEditor extends DialogCellEditor {
	private Label label;


protected JavaAttributeCellEditor(Composite parent) {
	super(parent);
}
protected Control createContents(Composite cell) {
	label = new Label(cell, SWT.LEFT);
	label.setFont(cell.getFont());
	label.setBackground(cell.getBackground());
	return label;
}
protected Object openDialogBox(Control cellEditorWindow) {
	JavaAttributeValue value = (JavaAttributeValue)getValue();
	IProject project = value.getProject();
	IPluginModelBase model = value.getModel();
	ISchemaAttribute attInfo = value.getAttributeInfo();

	JavaAttributeWizard wizard = new JavaAttributeWizard(project, model, attInfo, value.getClassName());
	WizardDialog dialog =
		new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
	dialog.create();
	SWTUtil.setDialogSize(dialog, 400, 500);
	int result = dialog.open();
	if (result == WizardDialog.OK) {
		return wizard.getValue();
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
