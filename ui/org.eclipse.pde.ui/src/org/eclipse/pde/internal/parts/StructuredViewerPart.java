/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.parts;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.jface.viewers.StructuredViewer;
/**
 * @version 	1.0
 * @author
 */
public abstract class StructuredViewerPart extends SharedPartWithButtons {
	private StructuredViewer viewer;

	public StructuredViewerPart(String [] buttonLabels) {
		super(buttonLabels);
	}
	
	public StructuredViewer getViewer() {
		return viewer;
	}
	
	public Control getControl() {
		return viewer.getControl();
	}
	
	/*
	 * @see SharedPartWithButtons#createMainControl(Composite, int, FormWidgetFactory)
	 */
	protected void createMainControl(
		Composite parent,
		int style,
		int span,
		FormWidgetFactory factory) {
		
		viewer = createStructuredViewer(parent, style, factory);
		Control control = viewer.getControl();
		if (factory!=null) {
			factory.hookDeleteListener(control);
		}
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = span;
		control.setLayoutData(gd);
	}

	protected void updateEnabledState() {
		getControl().setEnabled(isEnabled());
		super.updateEnabledState();
	}
	
	protected abstract StructuredViewer createStructuredViewer(Composite parent, int style, FormWidgetFactory factory);
}