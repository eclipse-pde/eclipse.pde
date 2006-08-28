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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class FormFilteredTree extends FilteredTree {
	
	public FormFilteredTree(Composite parent, int treeStyle,
			PatternFilter filter) {
		super(parent, treeStyle, filter);
	}

	protected Text doCreateFilterText(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Text text = new Text(parent, SWT.SINGLE | toolkit.getBorderStyle());
		toolkit.paintBordersFor(text.getParent());
		setBackground(toolkit.getColors().getBackground());
		return text;
	}

	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		TreeViewer viewer = new TreeViewer(parent, toolkit.getBorderStyle());
		toolkit.paintBordersFor(viewer.getTree().getParent());
		return viewer;
	}
	
}
