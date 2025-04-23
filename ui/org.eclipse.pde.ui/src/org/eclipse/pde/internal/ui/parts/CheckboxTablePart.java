/*******************************************************************************
 *  Copyright (c) 2000, 2025 IBM Corporation and others.
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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTableViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class CheckboxTablePart extends StructuredViewerPart {

	public CheckboxTablePart(String[] buttonLabels) {
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
		FilteredCheckboxTable filteredTable = new FilteredCheckboxTable(parent, toolkit, style);
		CachedCheckboxTableViewer tableViewer = filteredTable.getViewer();
		tableViewer
				.addSelectionChangedListener(e -> CheckboxTablePart.this.selectionChanged(e.getStructuredSelection()));
		tableViewer.addCheckStateListener(event -> elementChecked(event.getElement(), event.getChecked()));
		return tableViewer;
	}

	public CachedCheckboxTableViewer getTableViewer() {
		return (CachedCheckboxTableViewer) getViewer();
	}

	@Override
	protected void buttonSelected(Button button, int index) {
	}

	protected void elementChecked(Object element, boolean checked) {
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}
}
