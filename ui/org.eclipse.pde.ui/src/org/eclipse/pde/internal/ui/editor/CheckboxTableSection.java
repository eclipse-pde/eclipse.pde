/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @version 	1.0
 * @author
 */
public abstract class CheckboxTableSection extends StructuredViewerSection {
	protected boolean handleDefaultButton=true;
	class PartAdapter extends CheckboxTablePart {
		public PartAdapter(String [] buttonLabels) {
			super(buttonLabels);
		}
		public void selectionChanged(IStructuredSelection selection) {
			CheckboxTableSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			CheckboxTableSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {
			CheckboxTableSection.this.buttonSelected(index);
			if (handleDefaultButton) button.getShell().setDefaultButton(null);
		}
		protected void elementChecked(Object element, boolean checked) {
			CheckboxTableSection.this.elementChecked(element, checked);
		}
	}
	/**
	 * Constructor for TableSection.
	 * @param formPage
	 */
	public CheckboxTableSection(PDEFormPage formPage, String [] buttonLabels) {
		super(formPage, buttonLabels);
	}

	protected StructuredViewerPart createViewerPart(String [] buttonLabels) {
		//IModel model = (IModel)getFormPage().getModel();
		//CheckboxTablePart tablePart;
		//tablePart = new PartAdapter(buttonLabels);
		return new PartAdapter(buttonLabels);
	}
	
	protected CheckboxTablePart getTablePart() {
		return (CheckboxTablePart)viewerPart;
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
	protected void elementChecked(Object element, boolean checked) {
	}
}