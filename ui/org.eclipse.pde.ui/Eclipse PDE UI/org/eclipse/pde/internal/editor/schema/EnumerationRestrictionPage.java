package org.eclipse.pde.internal.editor.schema;

import java.util.Vector;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.events.*;
import java.util.Iterator;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.*;

public class EnumerationRestrictionPage implements IRestrictionPage {
	public static final String KEY_CHOICES = "SchemaEditor.RestrictionDialog.enumeration.choices";
	public static final String KEY_NEW_CHOICE = "SchemaEditor.RestrictionDialog.enumeration.newChoice";
	public static final String KEY_ADD = "SchemaEditor.RestrictionDialog.enumeration.add";
	public static final String KEY_REMOVE = "SchemaEditor.RestrictionDialog.enumeration.remove";
	private List choiceList;
	private Button addButton;
	private Button deleteButton;
	private Text text;
	private Control control;

public Control createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.makeColumnsEqualWidth = true;
	container.setLayout(layout);

	Composite listColumn = new Composite(container, SWT.NULL);
	GridData gd = new GridData(GridData.FILL_BOTH);
	listColumn.setLayoutData(gd);
	GridLayout llayout = new GridLayout();
	llayout.marginHeight = 0;
	llayout.marginWidth = 0;
	listColumn.setLayout(llayout);

	Label label = new Label(listColumn, SWT.NULL);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	label.setLayoutData(gd);
	label.setText(PDEPlugin.getResourceString(KEY_CHOICES));
	choiceList = new List(listColumn, SWT.MULTI | SWT.BORDER);
	gd = new GridData(GridData.FILL_BOTH);
	choiceList.setLayoutData(gd);

	Composite editColumn = new Composite(container, SWT.NULL);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	editColumn.setLayoutData(gd);
	GridLayout clayout = new GridLayout();
	clayout.marginHeight = 0;
	clayout.marginWidth = 0;
	clayout.numColumns = 2;
	clayout.makeColumnsEqualWidth = true;
	editColumn.setLayout(clayout);
	label = new Label(editColumn, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_NEW_CHOICE));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	label.setLayoutData(gd);
	text = new Text(editColumn, SWT.SINGLE | SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	text.setLayoutData(gd);

	addButton = new Button(editColumn, SWT.PUSH);
	addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	addButton.setText(PDEPlugin.getResourceString(KEY_ADD));
	addButton.setEnabled(false);
	addButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleAdd();
		}
	});

	deleteButton = new Button(editColumn, SWT.PUSH);
	deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	deleteButton.setText(PDEPlugin.getResourceString(KEY_REMOVE));
	deleteButton.setEnabled(false);
	deleteButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleDelete();
		}
	});

	text.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			String item = text.getText();
			boolean canAdd = true;
			if (item.length() == 0 || choiceList.indexOf(item) != -1)
				canAdd = false;
			addButton.setEnabled(canAdd);
		}
	});
	text.addListener(SWT.Traverse, new Listener() {
		public void handleEvent(Event e) {
			handleAdd();
			e.doit = false;
		}
	});

	choiceList.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			deleteButton.setEnabled(choiceList.getSelectionCount() > 0);
			if (choiceList.getSelectionCount() == 1) {
				text.setText(choiceList.getSelection()[0]);
			}
		}
	});
	this.control = container;
	return container;
}
public Class getCompatibleRestrictionClass() {
	return ChoiceRestriction.class;
}
public org.eclipse.swt.widgets.Control getControl() {
	return control;
}
public ISchemaRestriction getRestriction() {
	ChoiceRestriction restriction = new ChoiceRestriction((ISchema)null);
	String[] items = choiceList.getItems();
	if (items.length > 0) {
		Vector enums = new Vector();
		for (int i = 0; i < items.length; i++) {
			SchemaEnumeration enum = new SchemaEnumeration(restriction, items[i]);
			enums.addElement(enum);
		}
		restriction.setChildren(enums);
	}
	return restriction;
}
private void handleAdd() {
	String item = text.getText();
	choiceList.add(item);
	choiceList.setSelection(new String [] { item });
	addButton.setEnabled(false);
}
private void handleDelete() {
	String[] selection = choiceList.getSelection();
	choiceList.setRedraw(false);
	for (int i = 0; i < selection.length; i++) {
		choiceList.remove(selection[i]);
	}
	choiceList.setRedraw(true);
	deleteButton.setEnabled(false);
}
public void initialize(ISchemaRestriction restriction) {
	if (restriction != null) {
		Object[] children = restriction.getChildren();
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof ISchemaEnumeration) {
				choiceList.add(child.toString());
			}
		}
	}
}
}
