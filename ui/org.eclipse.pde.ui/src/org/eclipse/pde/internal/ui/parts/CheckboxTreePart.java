/*******************************************************************************
 * Copyright (c) 2016, 2018 Martin Karpisek and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Martin Karpisek <martin.karpisek@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class CheckboxTreePart extends StructuredViewerPart {
	private FilteredCheckboxTree filteredTree;

	public CheckboxTreePart(String[] buttonLabels) {
		super(buttonLabels);
	}

	@Override
	protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
		style |= SWT.H_SCROLL | SWT.V_SCROLL;
		if (toolkit == null) {
			style |= SWT.BORDER;
		} else {
			style |= toolkit.getBorderStyle();
		}
		filteredTree = new FilteredCheckboxTree(parent, toolkit, style);
		CheckboxTreeViewer treeViewer = filteredTree.getCheckboxTreeViewer();
		treeViewer.addSelectionChangedListener(event -> {
			CheckboxTreePart.this.selectionChanged(event.getStructuredSelection());
		});
		treeViewer.addCheckStateListener(event -> {
			elementChecked(event.getElement(), event.getChecked());
		});
		return treeViewer;
	}

	public CachedCheckboxTreeViewer getTreeViewer() {
		return (CachedCheckboxTreeViewer) getViewer();
	}

	@Override
	protected void buttonSelected(Button button, int index) {
	}

	protected void elementChecked(Object element, boolean checked) {
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}
}
