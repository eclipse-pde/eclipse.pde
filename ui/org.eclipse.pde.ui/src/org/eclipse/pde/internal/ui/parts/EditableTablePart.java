/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.RenameDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class EditableTablePart extends TablePart {
	private boolean editable;
	private Action renameAction;

	class RenameAction extends Action {
		public RenameAction() {
			super(PDEUIMessages.EditableTablePart_renameAction);
		}

		@Override
		public void run() {
			doRename();
		}
	}

	class NameModifier implements ICellModifier {
		@Override
		public boolean canModify(Object object, String property) {
			return true;
		}

		@Override
		public void modify(Object object, String property, Object value) {
			entryModified(object, value.toString());
		}

		@Override
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
		if (renameAction == null)
			renameAction = new RenameAction();
		return renameAction;
	}

	@Override
	protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
		TableViewer tableViewer = (TableViewer) super.createStructuredViewer(parent, style, toolkit);
		return tableViewer;
	}

	private void doRename() {
		TableViewer viewer = getTableViewer();
		IStructuredSelection selection = viewer.getStructuredSelection();
		if (selection.size() == 1 && isEditable()) {
			Object obj = selection.getFirstElement();
			String oldName = obj.toString();
			RenameDialog dialog = new RenameDialog(getControl().getShell(), oldName);
			dialog.create();
			dialog.getShell().setText(PDEUIMessages.EditableTablePart_renameTitle);
			dialog.getShell().setSize(300, 150);
			if (dialog.open() == Window.OK) {
				entryModified(obj, dialog.getNewName());
			}
		}
	}

	protected void entryModified(Object entry, String value) {
	}
}
