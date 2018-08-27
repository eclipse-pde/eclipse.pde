/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class TablePart extends StructuredViewerPart {

	/**
	 * Constructor for TablePart.
	 * @param buttonLabels
	 */
	public TablePart(String[] buttonLabels) {
		super(buttonLabels);
	}

	@Override
	protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
		style |= SWT.H_SCROLL | SWT.V_SCROLL;
		if (toolkit == null)
			style |= SWT.BORDER;
		else
			style |= toolkit.getBorderStyle();
		TableViewer tableViewer = new TableViewer(parent, style);
		tableViewer.addSelectionChangedListener(e -> TablePart.this.selectionChanged(e.getStructuredSelection()));
		tableViewer.addDoubleClickListener(e -> TablePart.this.handleDoubleClick((IStructuredSelection) e.getSelection()));
		return tableViewer;
	}

	public TableViewer getTableViewer() {
		return (TableViewer) getViewer();
	}

	@Override
	protected void buttonSelected(Button button, int index) {
	}

	protected void selectionChanged(IStructuredSelection selection) {
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
	}
}
