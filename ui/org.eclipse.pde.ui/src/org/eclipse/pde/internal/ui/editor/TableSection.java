/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.layout.GridLayout;

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