/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.parts;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
/**
 * @version 	1.0
 * @author
 */
public class EditableTablePart extends TablePart {
	private boolean editable;

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}
		public void modify(Object object, String property, Object value) {
			entryModified(object, value.toString());
		}
		public Object getValue(Object object, String property) {
			return object.toString();
		}
	}
	/**
	 * Constructor for EditableTablePart.
	 * @param buttonLabels
	 */
	public EditableTablePart(String[] buttonLabels) {
		super(buttonLabels);
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	protected StructuredViewer createStructuredViewer(
		Composite parent,
		int style,
		FormWidgetFactory factory) {
		TableViewer tableViewer =
			(TableViewer) super.createStructuredViewer(parent, style, factory);
		if (editable) {
			Table table = tableViewer.getTable();
			CellEditor[] editors = new CellEditor[] { new ModifiedTextCellEditor(table)};
			String[] properties = { "name" };
			tableViewer.setCellEditors(editors);
			tableViewer.setCellModifier(new NameModifier());
			tableViewer.setColumnProperties(properties);
		}
		return tableViewer;
	}

	protected void entryModified(Object entry, String value) {
	}
}