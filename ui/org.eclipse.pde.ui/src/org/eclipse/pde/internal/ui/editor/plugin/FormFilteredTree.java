/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class FormFilteredTree extends FilteredTree {
	
	private FormToolkit toolkit;
	
	public FormFilteredTree(Composite parent, int treeStyle,
			PatternFilter filter) {
		super(parent, treeStyle, filter);
	}
	
	protected void createControl(Composite parent, int treeStyle) {
		toolkit = new FormToolkit(parent.getDisplay());
		GridLayout layout = FormLayoutFactory.createClearGridLayout(false, 1);
		// Space between filter text field and tree viewer
		layout.verticalSpacing = 3;
		setLayout(layout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (showFilterControls){
        	filterComposite = new Composite(this, SWT.NONE);
        	GridLayout filterLayout = FormLayoutFactory.createClearGridLayout(false, 2);
        	filterLayout.horizontalSpacing = 5;
        	filterComposite.setLayout(filterLayout);
            filterComposite.setFont(parent.getFont());
        	createFilterControls(filterComposite);
        	filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING,
					true, false));
        }
        
        treeComposite = new Composite(this, SWT.NONE);
		treeComposite.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        treeComposite.setLayoutData(data);
        createTreeControl(treeComposite, treeStyle); 
	}

	public void dispose() {
		toolkit.dispose();
		toolkit = null;
		super.dispose();
	}

	protected Text doCreateFilterText(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | toolkit.getBorderStyle());
		toolkit.paintBordersFor(text.getParent());
		setBackground(toolkit.getColors().getBackground());
		return text;
	}

	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		TreeViewer viewer = new TreeViewer(parent, toolkit.getBorderStyle());
		toolkit.paintBordersFor(viewer.getTree().getParent());
		return viewer;
	}
	
}
