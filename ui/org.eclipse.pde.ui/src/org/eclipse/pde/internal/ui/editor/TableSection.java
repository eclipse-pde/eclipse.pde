/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
/**
 * @version 1.0
 * @author
 */
public abstract class TableSection extends StructuredViewerSection {
	protected boolean handleDefaultButton = true;
	class PartAdapter extends EditableTablePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}
		public void entryModified(Object entry, String value) {
			TableSection.this.entryModified(entry, value);
		}
		public void selectionChanged(IStructuredSelection selection) {
			getManagedForm().fireSelectionChanged(TableSection.this, selection);
			TableSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			TableSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {
			TableSection.this.buttonSelected(index);
			if (handleDefaultButton)
				button.getShell().setDefaultButton(null);
		}
		protected void createButtons(Composite parent, FormToolkit toolkit) {
			super.createButtons(parent, toolkit);
			enableButtons();
		}
	}
	/**
	 * Constructor for TableSection.
	 * 
	 * @param formPage
	 */
	public TableSection(PDEFormPage formPage, Composite parent, int style,
			String[] buttonLabels) {
		this (formPage, parent, style, true, buttonLabels);
	}
	/**
	 * Constructor for TableSection.
	 * 
	 * @param formPage
	 */
	public TableSection(PDEFormPage formPage, Composite parent, int style,
			boolean titleBar, String[] buttonLabels) {
		super(formPage, parent, style, titleBar, buttonLabels);
	}
	protected StructuredViewerPart createViewerPart(String[] buttonLabels) {
		return new PartAdapter(buttonLabels);
	}
	protected IAction getRenameAction() {
		return getTablePart().getRenameAction();
	}
	protected EditableTablePart getTablePart() {
		return (EditableTablePart) viewerPart;
	}
	protected void entryModified(Object entry, String value) {
	}
	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
	protected void enableButtons() {
	}
}
