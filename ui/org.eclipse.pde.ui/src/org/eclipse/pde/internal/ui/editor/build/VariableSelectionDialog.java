package org.eclipse.pde.internal.ui.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.events.*;
import java.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.*;

public class VariableSelectionDialog extends SelectionDialog {
	public static final String DIALOG_LABEL =
		"BuildEditor.VariableSection.dialogLabel";
	public static final String DIALOG_LIST =
		"BuildEditor.VariableSection.dialogList";

	private TableViewer variableTable;
	private String selectedVariable;
	private IBuildModel model;
	private Image variableImage;
	private Text resultText;

	private static final String[] supportedVariables =
		{
			"bin.includes",
			"bin.excludes",
			"src.includes",
			"src.excludes",
			"custom",
			"jars.extra.classpath",
			"jars.compile.order" };

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			return getRemainingVariables();
		}
	}
	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return variableImage;
		}
	}

public VariableSelectionDialog(Shell parentShell, IBuildModel model) {
	super(parentShell);
	variableImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
	this.model = model;
}
public boolean close() {
	variableImage.dispose();
	return super.close();
}
protected void createButtonsForButtonBar(Composite parent) {
	super.createButtonsForButtonBar(parent);
	getOkButton().setEnabled(false);
}
public Control createDialogArea(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	container.setLayout(layout);
	container.setLayoutData(new GridData(GridData.FILL_BOTH));

	Label label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(DIALOG_LABEL));

	resultText = new Text(container, SWT.SINGLE | SWT.BORDER);
	resultText.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			getOkButton().setEnabled(resultText.getText().length() > 0);
		}
	});
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	resultText.setLayoutData(gd);

	Table table = new Table(container, SWT.FULL_SELECTION | SWT.BORDER);
	TableLayout tlayout = new TableLayout();

	TableColumn tableColumn = new TableColumn(table, SWT.NULL);
	tableColumn.setText(PDEPlugin.getResourceString(DIALOG_LIST));
	ColumnLayoutData cLayout = new ColumnWeightData(100, true);
	tlayout.addColumnData(cLayout);
	table.setLayout(tlayout);
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;
	table.setLayoutData(gd);

	variableTable = new TableViewer(table);
	variableTable.setContentProvider(new TableContentProvider());
	variableTable.setLabelProvider(new TableLabelProvider());
	variableTable.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent e) {
			ISelection sel = e.getSelection();
			Object obj = ((IStructuredSelection) sel).getFirstElement();
			resultText.setText(obj != null ? obj.toString() : "");
		}
	});
	variableTable.setInput(model);
	return container;
}
private Object[] getRemainingVariables() {
	IBuild build = model.getBuild();
	Vector remaining = new Vector();

	for (int i=0; i<supportedVariables.length; i++) {
		String variable = supportedVariables[i];
		if (build.getEntry(variable)==null) {
			remaining.add(variable);
		}
	}
	return remaining.toArray();
}
public java.lang.String getSelectedVariable() {
	return selectedVariable;
}
protected void okPressed() {
	selectedVariable = resultText.getText();
	super.okPressed();
}
}
