/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class TableSection extends StructuredViewerSection {
	protected boolean fHandleDefaultButton = true;

	class PartAdapter extends EditableTablePart {
		private Label fCount;

		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}

		@Override
		public void entryModified(Object entry, String value) {
			TableSection.this.entryModified(entry, value);
		}

		@Override
		public void selectionChanged(IStructuredSelection selection) {
			getManagedForm().fireSelectionChanged(TableSection.this, selection);
			TableSection.this.selectionChanged(selection);
		}

		@Override
		public void handleDoubleClick(IStructuredSelection selection) {
			TableSection.this.handleDoubleClick(selection);
		}

		@Override
		public void buttonSelected(Button button, int index) {
			TableSection.this.buttonSelected(index);
			if (fHandleDefaultButton)
				button.getShell().setDefaultButton(null);
		}

		@Override
		protected void createButtons(Composite parent, FormToolkit toolkit) {
			super.createButtons(parent, toolkit);
			enableButtons();
			if (createCount()) {
				Composite comp = toolkit.createComposite(fButtonContainer);
				comp.setLayout(createButtonsLayout());
				comp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END | GridData.FILL_BOTH));
				fCount = toolkit.createLabel(comp, ""); //$NON-NLS-1$
				fCount.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
				fCount.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				getTablePart().getTableViewer().getTable().addPaintListener(e -> updateLabel());
			}
		}

		protected void updateLabel() {
			if (fCount != null && !fCount.isDisposed())
				fCount.setText(NLS.bind(PDEUIMessages.TableSection_itemCount, Integer.toString(getTableViewer().getTable().getItemCount())));
		}
	}

	/**
	 * Constructor for TableSection.
	 *
	 * @param formPage
	 */
	public TableSection(PDEFormPage formPage, Composite parent, int style, String[] buttonLabels) {
		this(formPage, parent, style, true, buttonLabels);
	}

	/**
	 * Constructor for TableSection.
	 *
	 * @param formPage
	 */
	public TableSection(PDEFormPage formPage, Composite parent, int style, boolean titleBar, String[] buttonLabels) {
		super(formPage, parent, style, titleBar, buttonLabels);
	}

	@Override
	protected StructuredViewerPart createViewerPart(String[] buttonLabels) {
		return new PartAdapter(buttonLabels);
	}

	protected IAction getRenameAction() {
		return getTablePart().getRenameAction();
	}

	protected EditableTablePart getTablePart() {
		return (EditableTablePart) fViewerPart;
	}

	protected void entryModified(Object entry, String value) {
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
	}

	protected void enableButtons() {
	}

	protected boolean createCount() {
		return false;
	}

	@Override
	protected void handleSelectAll() {
		TableViewer viewer = getTablePart().getTableViewer();
		if (viewer == null) {
			return;
		}
		Table table = viewer.getTable();
		if (table == null) {
			return;
		}
		table.selectAll();
		selectionChanged(viewer.getStructuredSelection());
	}
}
