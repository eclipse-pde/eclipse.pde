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
package org.eclipse.pde.internal.ui.neweditor.schema;

import org.eclipse.jface.viewers.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;


public class SchemaSourceOutlinePage extends ContentOutlinePage {
	private IEditorInput input;
	private TreeViewer treeViewer;

public SchemaSourceOutlinePage(IEditorInput input, IDocumentProvider provider, AbstractTextEditor editor) {
	this.input = input;
}
public void createControl(Composite parent) {
	treeViewer = new TreeViewer(new Tree(parent, SWT.MULTI));
	treeViewer.addSelectionChangedListener(this);

	treeViewer.setContentProvider(new SchemaContentProvider());
	treeViewer.setLabelProvider(new LabelProvider());
	treeViewer.setInput(input);
}
public Control getControl() {
	return treeViewer != null ? treeViewer.getControl() : null;
}
public void selectionChanged(SelectionChangedEvent event) {
	super.selectionChanged(event);
}
}
