package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.resources.*;

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
	ISchemaAttribute attInfo = value.getAttributeInfo();

	JavaAttributeWizard wizard = new JavaAttributeWizard(project, attInfo, value.getClassName());
	WizardDialog dialog =
		new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
	dialog.create();
	dialog.getShell().setSize(400, 500);
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
