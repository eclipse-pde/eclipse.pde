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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


public class SiteSourceOutlinePage extends ContentOutlinePage {
	private IEditorInput input;
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

public SiteSourceOutlinePage(IEditorInput input, IDocumentProvider provider, AbstractTextEditor editor) {
	this.input = input;
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
