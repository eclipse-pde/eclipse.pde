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
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @version 	1.0
 * @author
 */
public class CheckboxTablePart extends StructuredViewerPart {
	public CheckboxTablePart(String [] buttonLabels) {
		super(buttonLabels);
	}

	/*
	 * @see StructuredViewerPart#createStructuredViewer(Composite, FormWidgetFactory)
	 */
	protected StructuredViewer createStructuredViewer(
		Composite parent,
		int style,
		FormToolkit toolkit) {
		style |= SWT.H_SCROLL | SWT.V_SCROLL;
		if (toolkit==null) {
			style |= SWT.BORDER;
		}
		else {
			style |= toolkit.getBorderStyle();
		}
		CheckboxTableViewer	tableViewer = CheckboxTableViewer.newCheckList(parent, style);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				CheckboxTablePart.this.selectionChanged((IStructuredSelection)e.getSelection());
			}
		});
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				elementChecked(event.getElement(), event.getChecked());
			}
		});
		return tableViewer;
	}
	
	public CheckboxTableViewer getTableViewer() {
		return (CheckboxTableViewer)getViewer();
	}
	
	/*
	 * @see SharedPartWithButtons#buttonSelected(int)
	 */
	protected void buttonSelected(Button button, int index) {
	}
	
	protected void elementChecked(Object element, boolean checked) {
	}
	protected void selectionChanged(IStructuredSelection selection) {
	}
}
