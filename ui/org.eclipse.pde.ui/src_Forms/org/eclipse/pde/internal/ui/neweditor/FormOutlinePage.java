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
package org.eclipse.pde.internal.ui.neweditor;
import java.util.ArrayList;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class FormOutlinePage extends ContentOutlinePage
		implements
			IModelChangedListener {
	private boolean stale;
	public class BasicContentProvider extends DefaultContentProvider
			implements
				ITreeContentProvider {
		public Object[] getElements(Object obj) {
			return getPages();
		}
		public Object[] getChildren(Object obj) {
			return FormOutlinePage.this.getChildren(obj);
		}
		public boolean hasChildren(Object obj) {
			return getChildren(obj).length > 0;
		}
		public Object getParent(Object obj) {
			return null;
		}
	}
	public class BasicLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IFormPage)
				return ((IFormPage)obj).getTitle();
			return PDEPlugin.getDefault().getLabelProvider().getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof IFormPage)
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PAGE_OBJ);
			return PDEPlugin.getDefault().getLabelProvider().getImage(obj);
		}
	}
	protected TreeViewer treeViewer;
	protected PDEFormEditor editor;
	
	public FormOutlinePage(PDEFormEditor editor) {
		this.editor = editor;
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
		treeViewer.setInput(editor);
		IBaseModel model = editor.getAggregateModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider)model).addModelChangedListener(this);
	}
	protected ILabelProvider createLabelProvider() {
		return new BasicLabelProvider();
	}
	public void dispose() {
		super.dispose();
		IBaseModel model = editor.getAggregateModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider)model).removeModelChangedListener(this);
	}
	
	public Control getControl() {
		return treeViewer != null ? treeViewer.getControl() : null;
	}
	private Object[] getPages() {
		ArrayList formPages = new ArrayList();
		IFormPage [] pages = editor.getPages();
		for (int i=0; i<pages.length; i++) {
			if (pages[i].isEditor()==false)
				formPages.add(pages[i]);
		}
		return formPages.toArray(); 
	}

	public void modelChanged(IModelChangedEvent event) {
		IFormPage page = editor.getActivePageInstance();
		stale=true;
		if (page.isEditor()==false)
			refresh();
	}
	
	public void refresh() {
		if (stale) {
			treeViewer.refresh();
			treeViewer.expandAll();
			stale=false;
		}
	}
	
	protected String getParentPageId(Object item) {
		if (item instanceof IFormPage)
			return ((IFormPage)item).getId();
		return null;
	}
	
	protected Object[] getChildren(Object parent) {
		return new Object[0];
	}

	public void selectionChanged(Object item) {
		IFormPage page = editor.getActivePageInstance();
		String id = getParentPageId(item);
		IFormPage newPage=null;
		if (id!=null && (page==null || !page.getId().equals(id)))
			newPage = editor.setActivePage(id);
		IFormPage revealPage = newPage!=null?newPage:page;
		if (revealPage!=null && !(item instanceof IFormPage))
			revealPage.selectReveal(item);
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
