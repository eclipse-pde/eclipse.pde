/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;

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
		FormWidgetFactory factory) {
		style |= SWT.H_SCROLL | SWT.V_SCROLL;
		if (factory==null) {
			style |= SWT.BORDER;
		}
		else {
			style |= FormWidgetFactory.BORDER_STYLE;
		}
		CheckboxTableViewer	tableViewer = CheckboxTableViewer.newCheckList(parent, style);
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
}