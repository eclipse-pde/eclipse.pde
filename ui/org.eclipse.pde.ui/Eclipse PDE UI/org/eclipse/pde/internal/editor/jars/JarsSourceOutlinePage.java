package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;


public class JarsSourceOutlinePage extends ContentOutlinePage {
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

public JarsSourceOutlinePage(IEditorInput input, IDocumentProvider provider, AbstractTextEditor editor) {
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
