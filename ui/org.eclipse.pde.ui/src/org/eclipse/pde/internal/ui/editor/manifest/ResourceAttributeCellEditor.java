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

import org.eclipse.ui.dialogs.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.resources.*;

public class ResourceAttributeCellEditor extends DialogCellEditor {
	public static final String TITLE = "ManifestEditor.ResourceAttributeCellEditor.title";
	private Label label;


protected ResourceAttributeCellEditor(Composite parent) {
	super(parent);
}
protected Control createContents(Composite cell) {
	label = new Label(cell, SWT.LEFT);
	label.setFont(cell.getFont());
	label.setBackground(cell.getBackground());
	return label;
}
protected Object openDialogBox(Control cellEditorWindow) {
	ResourceAttributeValue value = (ResourceAttributeValue) getValue();
	IProject project = value.getProject();

	ResourceSelectionDialog dialog =
		new ResourceSelectionDialog(
			PDEPlugin.getActiveWorkbenchShell(),
			project,
			PDEPlugin.getResourceString(TITLE));
	int result = dialog.open();
	if (result == ResourceSelectionDialog.OK) {
		Object[] resources = dialog.getResult();
		IResource resource = (IResource) resources[0];
		if (resource instanceof IFile) {
			String stringValue = resource.getProjectRelativePath().toString();
			return new ResourceAttributeValue(project, stringValue);
		}
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
