/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.swt.SWT;

/**
 * @version 	1.0
 * @author
 */
public class TablePart extends StructuredViewerPart {

	/**
	 * Constructor for TablePart.
	 * @param buttonLabels
	 */
	public TablePart(String[] buttonLabels) {
		super(buttonLabels);
	}

	/*
	 * @see StructuredViewerPart#createStructuredViewer(Composite, FormWidgetFactory)
	 */
	protected StructuredViewer createStructuredViewer(
		Composite parent,
		int style,
		FormWidgetFactory factory) {
		style |= SWT.H_SCROLL | SWT.V_SCROLL;
		if (factory==null) {
			style |= SWT.BORDER;
		}
		else {
			style |= FormWidgetFactory.BORDER_STYLE;
		}
		TableViewer	tableViewer = new TableViewer(parent, style);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener () {
			public void selectionChanged(SelectionChangedEvent e) {
				TablePart.this.selectionChanged((IStructuredSelection)e.getSelection());
			}
		});
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				TablePart.this.handleDoubleClick((IStructuredSelection)e.getSelection());
			}
		});
		return tableViewer;
	}
	
	public TableViewer getTableViewer() {
		return (TableViewer)getViewer();
	}
	/*
	 * @see SharedPartWithButtons#buttonSelected(int)
	 */
	protected void buttonSelected(Button button, int index) {
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
	}
}
