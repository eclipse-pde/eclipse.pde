/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.parts;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
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
			table.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.keyCode==SWT.F2) {
						activateEditMode();
					}
				}
			});
		}
		return tableViewer;
	}
	
	private void activateEditMode() {
		TableViewer viewer = getTableViewer();
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		if (selection.size()==1 && !viewer.isCellEditorActive()) {
			viewer.editElement(selection.getFirstElement(), 0);
		}
	}

	protected void entryModified(Object entry, String value) {
	}
}