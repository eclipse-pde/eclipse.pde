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
package org.eclipse.pde.internal.ui.newparts;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @version 	1.0
 * @author
 */
public class TreePart extends StructuredViewerPart {

	/**
	 * Constructor for TreePart.
	 * @param buttonLabels
	 */
	public TreePart(String[] buttonLabels) {
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
		TreeViewer	treeViewer = new TreeViewer(parent, style);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener () {
			public void selectionChanged(SelectionChangedEvent e) {
				TreePart.this.selectionChanged((IStructuredSelection)e.getSelection());
			}
		});
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				TreePart.this.handleDoubleClick((IStructuredSelection)e.getSelection());
			}
		});
		return treeViewer;
	}
	
	public TreeViewer getTreeViewer() {
		return (TreeViewer)getViewer();
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
