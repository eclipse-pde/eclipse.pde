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
package org.eclipse.pde.internal.ui.editor;

import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


public class FormOutlinePage extends ContentOutlinePage implements IModelChangedListener {

	protected PDEFormPage formPage;

	public class BasicContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getElements(Object obj) {
			return getPages();
		}
		public Object[] getChildren(Object obj) {
			return new Object[0];
		}
		public boolean hasChildren(Object obj) {
			return getChildren(obj).length >0;
		}
		public Object getParent(Object obj) {
			return null;
		}
	}
	public class BasicLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}
		public Image getImage(Object obj) {
			return PDEPlugin.getDefault().getLabelProvider().getImage(obj);
		}
	}
	protected TreeViewer treeViewer;

public FormOutlinePage(PDEFormPage formPage) {
	this.formPage = formPage;
}
protected ITreeContentProvider createContentProvider() {
	return new BasicContentProvider();
}
public void createControl(Composite parent) {
	Tree widget = new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	treeViewer = new TreeViewer(widget);
	treeViewer.addSelectionChangedListener(this);
	treeViewer.setContentProvider(createContentProvider());
	treeViewer.setLabelProvider(createLabelProvider());
	treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	treeViewer.setUseHashlookup(true);
	treeViewer.setInput(formPage.getEditor());

	//tree.setBackground(formPage.getForm().getFactory().getBackgroundColor());
	Object model = formPage.getModel();
	if (model instanceof IModelChangeProvider) {
		((IModelChangeProvider)model).addModelChangedListener(this);
	}
}
protected ILabelProvider createLabelProvider() {
	return new BasicLabelProvider();
}
public void dispose() {
	super.dispose();
	Object model = formPage.getModel();
	if (model instanceof IModelChangeProvider) {
		((IModelChangeProvider) model).removeModelChangedListener(this);
	}
}
public Control getControl() {
	return treeViewer!=null?treeViewer.getControl():null;
}
private Object[] getPages() {
	Vector formPages = new Vector();
	for (Iterator iter = formPage.getEditor().getPages(); iter.hasNext();) {
		IPDEEditorPage page = (IPDEEditorPage) iter.next();
		if (!page.isSource())
			formPages.addElement(page);
	}
	Object [] result = new Object[formPages.size()];
	formPages.copyInto(result);
	return result;
}
public IPDEEditorPage getParentPage(Object item) {
	if (item instanceof IPDEEditorPage)
		return (IPDEEditorPage) item;
	return null;
}
public void modelChanged(IModelChangedEvent event) {
	// a really suboptimal refresh - subclasses should be more selective
	treeViewer.refresh();
	treeViewer.expandAll();
}
public void selectionChanged(Object item) {
	IPDEEditorPage page = formPage.getEditor().getCurrentPage();
	IPDEEditorPage newPage = getParentPage(item);
	if (newPage!=page) formPage.getEditor().showPage(newPage);
	if (newPage != item) newPage.openTo(item);
}
public void selectionChanged(SelectionChangedEvent event) {
	ISelection selection = event.getSelection();
	if (selection.isEmpty() == false
		&& selection instanceof IStructuredSelection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		Object item = ssel.getFirstElement();
		selectionChanged(item);
	}
	fireSelectionChanged(selection);
}
public void setFocus() {
	if (treeViewer != null)
		treeViewer.getTree().setFocus();
}
public ISelection getSelection() {
	if (treeViewer == null)
		return StructuredSelection.EMPTY;
	return treeViewer.getSelection();
}
}
