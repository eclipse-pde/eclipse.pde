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
package org.eclipse.pde.internal.ui.parts;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.RenameDialog;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
/**
 * @version 	1.0
 * @author
 */
public class EditableTablePart extends TablePart {
	private boolean editable;
	private Action renameAction;
	
	class RenameAction extends Action {
		public RenameAction() {
			super(PDEPlugin.getResourceString("EditableTablePart.renameAction")); //$NON-NLS-1$
		}
		public void run() {
			doRename();
		}
	}

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
	
	public IAction getRenameAction() {
		if (renameAction==null) renameAction = new RenameAction();
		return renameAction;
	}

	protected StructuredViewer createStructuredViewer(
		Composite parent,
		int style,
		FormToolkit toolkit) {
		TableViewer tableViewer =
			(TableViewer) super.createStructuredViewer(parent, style, toolkit);
		return tableViewer;
	}

	private void doRename() {
		TableViewer viewer = getTableViewer();
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		if (selection.size()==1 && isEditable()) {
			Object obj = selection.getFirstElement();
			String oldName = obj.toString();
			RenameDialog dialog = new RenameDialog(getControl().getShell(), oldName);
			dialog.create();
			dialog.getShell().setText(PDEPlugin.getResourceString("EditableTablePart.renameTitle")); //$NON-NLS-1$
			dialog.getShell().setSize(300, 150);
			if (dialog.open()==Dialog.OK) {
				entryModified(doFindItem(obj), dialog.getNewName());
			}
		}
	}
	
	private Widget doFindItem(Object element) {
		TableItem[] children = getTableViewer().getTable().getItems();
		for (int i = 0; i < children.length; i++) {
			TableItem item = children[i];
			Object data = item.getData();
			if (data != null && data.equals(element))
				return item;
		}
		return null;
	}

	protected void entryModified(Object entry, String value) {
	}
}
