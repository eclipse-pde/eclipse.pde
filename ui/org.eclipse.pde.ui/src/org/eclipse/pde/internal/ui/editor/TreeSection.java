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
public abstract class TreeSection extends StructuredViewerSection {
	protected boolean handleDefaultButton=true;
	class PartAdapter extends TreePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}
		public void selectionChanged(IStructuredSelection selection) {
			TreeSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			TreeSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {
			TreeSection.this.buttonSelected(index);
			if (handleDefaultButton)
				button.getShell().setDefaultButton(null);
		}
	}
	/**
	 * Constructor for TableSection.
	 * @param formPage
	 */
	public TreeSection(PDEFormPage formPage, String[] buttonLabels) {
		super(formPage, buttonLabels);
	}

	protected StructuredViewerPart createViewerPart(String[] buttonLabels) {
		return new PartAdapter(buttonLabels);
	}

	protected TreePart getTreePart() {
		return (TreePart) viewerPart;
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
}