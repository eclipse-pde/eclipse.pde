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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class TreeSection extends StructuredViewerSection {

	protected boolean fHandleDefaultButton = true;

	class PartAdapter extends TreePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}

		@Override
		public void selectionChanged(IStructuredSelection selection) {
			getManagedForm().fireSelectionChanged(TreeSection.this, selection);
			TreeSection.this.selectionChanged(selection);
		}

		@Override
		public void handleDoubleClick(IStructuredSelection selection) {
			TreeSection.this.handleDoubleClick(selection);
		}

		@Override
		public void buttonSelected(Button button, int index) {
			TreeSection.this.buttonSelected(index);
			if (fHandleDefaultButton)
				button.getShell().setDefaultButton(null);
		}

		@Override
		protected void createButtons(Composite parent, FormToolkit toolkit) {
			super.createButtons(parent, toolkit);
			enableButtons();
			if (parent.getData("filtered") != null) { //$NON-NLS-1$
				GridLayout layout = (GridLayout) fButtonContainer.getLayout();
				layout.marginHeight = 28;
			}
		}

		@Override
		protected TreeViewer createTreeViewer(Composite parent, int style) {
			return TreeSection.this.createTreeViewer(parent, style);
		}

	}

	/**
	 * Constructor for TableSection.
	 * @param formPage
	 */
	public TreeSection(PDEFormPage formPage, Composite parent, int style, String[] buttonLabels) {
		super(formPage, parent, style, buttonLabels);
	}

	@Override
	protected StructuredViewerPart createViewerPart(String[] buttonLabels) {
		return new PartAdapter(buttonLabels);
	}

	protected TreePart getTreePart() {
		return (TreePart) fViewerPart;
	}

	protected TreeViewer createTreeViewer(Composite parent, int style) {
		return new TreeViewer(parent, style);
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}

	/**
	 * Expands or collapsed selected node according to its current state
	 * @param selection
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		TreeViewer viewer = (TreeViewer) fViewerPart.getViewer();
		boolean expandedState = viewer.getExpandedState(selection.getFirstElement());
		viewer.setExpandedState(selection.getFirstElement(), !expandedState);
	}

	protected void enableButtons() {
	}

	@Override
	protected void handleSelectAll() {
		TreeViewer viewer = getTreePart().getTreeViewer();
		if (viewer == null) {
			return;
		}
		Tree tree = viewer.getTree();
		if (tree == null) {
			return;
		}
		tree.selectAll();
		selectionChanged(viewer.getStructuredSelection());
	}
}
