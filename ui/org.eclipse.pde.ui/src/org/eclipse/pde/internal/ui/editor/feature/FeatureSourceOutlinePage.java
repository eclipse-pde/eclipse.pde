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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;


public class FeatureSourceOutlinePage extends ContentOutlinePage {
	private IDocumentProvider documentProvider;
	private IEditorInput input;
	private AbstractTextEditor textEditor;
	private TreeViewer treeViewer;

	class ContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getElements(Object obj) {
			return new Object[0];
		}
		public Object[] getChildren(Object obj) {
			return new Object[0];
		}
		public boolean hasChildren(Object obj) {
			return getChildren(obj).length>0;
		}
		public Object getParent(Object obj) {
			return null;
		}
	}

public FeatureSourceOutlinePage(IEditorInput input, IDocumentProvider provider, AbstractTextEditor editor) {
	this.input = input;
	this.documentProvider = provider;
	this.textEditor = editor;
}
public void createControl(Composite parent) {
	treeViewer = new TreeViewer(new Tree(parent, SWT.MULTI));
	treeViewer.addSelectionChangedListener(this);

	treeViewer.setContentProvider(new ContentProvider());
	treeViewer.setLabelProvider(new LabelProvider());
	treeViewer.setInput(input);
}
public Control getControl() {
	if (treeViewer!=null) return treeViewer.getControl();
	return null;
}
public void selectionChanged(SelectionChangedEvent event) {
	super.selectionChanged(event);
}
}
