package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;


public class SchemaSourceOutlinePage extends ContentOutlinePage {
	private IDocumentProvider documentProvider;
	private IEditorInput input;
	private AbstractTextEditor textEditor;
	private TreeViewer treeViewer;

public SchemaSourceOutlinePage(IEditorInput input, IDocumentProvider provider, AbstractTextEditor editor) {
	this.input = input;
	this.documentProvider = provider;
	this.textEditor = editor;
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
