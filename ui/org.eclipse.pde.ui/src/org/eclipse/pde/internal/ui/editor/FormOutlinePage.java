/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.editor.IFormPage;

public class FormOutlinePage extends PDEOutlinePage implements IModelChangedListener, ISortableContentOutlinePage {

	private boolean fStale;
	private ViewerComparator fViewerComparator;
	private boolean fSorted;

	public class BasicContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object obj) {
			return getPages();
		}

		@Override
		public Object[] getChildren(Object obj) {
			return FormOutlinePage.this.getChildren(obj);
		}

		@Override
		public boolean hasChildren(Object obj) {
			return getChildren(obj).length > 0;
		}

		@Override
		public Object getParent(Object obj) {
			return null;
		}
	}

	public static class BasicLabelProvider extends LabelProvider {
		private ILabelProvider fWrappedLabelProvider;

		public BasicLabelProvider(ILabelProvider ilp) {
			fWrappedLabelProvider = ilp;
		}

		@Override
		public String getText(Object obj) {
			if (obj instanceof IFormPage)
				return ((IFormPage) obj).getTitle();
			return fWrappedLabelProvider.getText(obj);
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof IFormPage)
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PAGE_OBJ);
			return fWrappedLabelProvider.getImage(obj);
		}
	}

	public class BasicComparator extends ViewerComparator {
		@Override
		public int category(Object element) {
			Object[] pages = getPages();
			for (int i = 0; i < pages.length; i++) {
				if (pages[i] == element) {
					return i;
				}
			}
			return Integer.MAX_VALUE;
		}
	}

	protected TreeViewer fTreeViewer;
	protected boolean fEditorSelection = false;
	protected boolean fOutlineSelection = false;

	public FormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	public ITreeContentProvider createContentProvider() {
		return new BasicContentProvider();
	}

	public ViewerComparator createOutlineSorter() {
		return new BasicComparator();
	}

	@Override
	public void createControl(Composite parent) {
		Tree widget = new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		fTreeViewer = new TreeViewer(widget);
		fTreeViewer.addSelectionChangedListener(this);
		fTreeViewer.setContentProvider(createContentProvider());
		fTreeViewer.setLabelProvider(createLabelProvider());
		fViewerComparator = createOutlineSorter();
		if (fSorted)
			fTreeViewer.setComparator(fViewerComparator);
		else
			fTreeViewer.setComparator(null);
		fTreeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setInput(fEditor);
		IBaseModel model = fEditor.getAggregateModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).addModelChangedListener(this);
	}

	public ILabelProvider createLabelProvider() {
		return new BasicLabelProvider(PDEPlugin.getDefault().getLabelProvider());
	}

	@Override
	public void dispose() {
		IBaseModel model = fEditor.getAggregateModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public Control getControl() {
		return fTreeViewer != null ? fTreeViewer.getControl() : null;
	}

	private Object[] getPages() {
		ArrayList<IFormPage> formPages = new ArrayList<>();
		IFormPage[] pages = fEditor.getPages();
		for (IFormPage page : pages) {
			if (page.isEditor() == false)
				formPages.add(page);
		}
		return formPages.toArray();
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		IFormPage page = fEditor.getActivePageInstance();
		fStale = true;
		if (page.isEditor() == false)
			refresh();
	}

	public void refresh() {
		if (fStale) {
			final Control control = getControl();
			if (control == null || control.isDisposed())
				return;
			control.getDisplay().asyncExec(() -> {
				if (!fTreeViewer.getControl().isDisposed()) {
					fTreeViewer.refresh();
					fTreeViewer.expandAll();
					fStale = false;
				}
			});
		}
	}

	protected String getParentPageId(Object item) {
		if (item instanceof IFormPage)
			return ((IFormPage) item).getId();
		return null;
	}

	protected Object[] getChildren(Object parent) {
		return new Object[0];
	}

	public void selectionChanged(Object item) {
		IFormPage page = fEditor.getActivePageInstance();
		String id = getParentPageId(item);
		IFormPage newPage = null;
		if (id != null && (page == null || !page.getId().equals(id)))
			newPage = fEditor.setActivePage(id);
		IFormPage revealPage = newPage != null ? newPage : page;
		if (revealPage != null && !(item instanceof IFormPage))
			revealPage.selectReveal(item);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (fEditorSelection)
			return;
		fOutlineSelection = true;
		try {
			ISelection selection = event.getSelection();
			if (selection.isEmpty() == false && selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				Object item = ssel.getFirstElement();
				selectionChanged(item);
			}
			fireSelectionChanged(selection);
		} finally {
			fOutlineSelection = false;
		}
	}

	@Override
	public void setFocus() {
		if (fTreeViewer != null)
			fTreeViewer.getTree().setFocus();
	}

	@Override
	public ISelection getSelection() {
		if (fTreeViewer == null)
			return StructuredSelection.EMPTY;
		return fTreeViewer.getSelection();
	}

	@Override
	public void sort(boolean sorting) {
		fSorted = sorting;
		if (fTreeViewer != null)
			if (sorting)
				fTreeViewer.setComparator(fViewerComparator);
			else
				fTreeViewer.setComparator(null);
	}

	@Override
	public void setSelection(ISelection selection) {
		if (fOutlineSelection)
			return;
		fEditorSelection = true;
		try {
			if (fTreeViewer == null)
				return;
			if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
				Object item = ((IStructuredSelection) selection).getFirstElement();
				if (item instanceof ImportObject) {
					selection = new StructuredSelection(((ImportObject) item).getImport());
				}
				if (item instanceof IDocumentElementNode) {
					while (null == fTreeViewer.testFindItem(item)) {
						item = ((IDocumentElementNode) item).getParentNode();
						if (item == null) {
							break;
						}
						selection = new StructuredSelection(item);
					}
				}
			}
			fTreeViewer.setSelection(selection);
		} finally {
			fEditorSelection = false;
		}
	}

	@Override
	protected TreeViewer getTreeViewer() {
		return fTreeViewer;
	}
}
