/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Point;
/**
 * @version 	1.0
 * @author
 */
public abstract class StructuredViewerPart extends SharedPartWithButtons {
	private StructuredViewer viewer;
	private Point minSize = null;

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
		applyMinimumSize();
	}
	
	public void setMinimumSize(int width, int height) {
		minSize = new Point(width, height);
		if (viewer!=null)
			applyMinimumSize();
	}
	private void applyMinimumSize() {
		if (minSize!=null) {
			GridData gd = (GridData)viewer.getControl().getLayoutData();
			gd.widthHint = minSize.x;
			gd.heightHint = minSize.y;
		}
	}

	protected void updateEnabledState() {
		getControl().setEnabled(isEnabled());
		super.updateEnabledState();
	}
	
	protected abstract StructuredViewer createStructuredViewer(Composite parent, int style, FormWidgetFactory factory);
}