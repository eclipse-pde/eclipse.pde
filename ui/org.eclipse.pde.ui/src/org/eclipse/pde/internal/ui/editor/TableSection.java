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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.widgets.Button;

/**
 * @version 	1.0
 * @author
 */
public abstract class TableSection extends StructuredViewerSection {
	protected boolean handleDefaultButton=true;
	class PartAdapter extends EditableTablePart {
		public PartAdapter(String [] buttonLabels) {
			super(buttonLabels);
		}
		public void entryModified(Object entry, String value) {
			TableSection.this.entryModified(entry, value);
		}
		public void selectionChanged(IStructuredSelection selection) {
			TableSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			TableSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {
			TableSection.this.buttonSelected(index);
			if (handleDefaultButton) button.getShell().setDefaultButton(null);
		}
	}
	/**
	 * Constructor for TableSection.
	 * @param formPage
	 */
	public TableSection(PDEFormPage formPage, String [] buttonLabels) {
		super(formPage, buttonLabels);
	}

	protected StructuredViewerPart createViewerPart(String [] buttonLabels) {
		IModel model = (IModel)getFormPage().getModel();
		EditableTablePart tablePart;
		tablePart = new PartAdapter(buttonLabels);
		tablePart.setEditable(model.isEditable());
		return tablePart;
	}
	
	protected IAction getRenameAction() {
		return getTablePart().getRenameAction();
	}
	
	protected EditableTablePart getTablePart() {
		return (EditableTablePart)viewerPart;
	}
	
	protected void entryModified(Object entry, String value) {
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
}
