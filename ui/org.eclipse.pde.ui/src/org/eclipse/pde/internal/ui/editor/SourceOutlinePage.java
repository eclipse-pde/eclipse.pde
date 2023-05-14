/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;

/**
 * Content outline page for the XML editor.
 */
public class SourceOutlinePage extends PDEOutlinePage implements IReconcilingParticipant, ISortableContentOutlinePage {

	private IEditingModel fModel;
	private IBaseLabelProvider fLabelProvider;
	private IContentProvider fContentProvider;
	private ViewerComparator fDefaultComparator;
	private ViewerComparator fViewerComparator;
	private boolean sorted;
	TreeViewer viewer;

	/**
	 * This list is redundant; but, required because we can't access
	 * org.eclipse.ui.views.contentoutline.ContentOutlinePage.selectionChangedListeners
	 * from our parent
	 */
	private ArrayList<ISelectionChangedListener> fListenerList;

	public SourceOutlinePage(PDEFormEditor editor, IEditingModel model, IBaseLabelProvider lProvider, IContentProvider cProvider, ViewerComparator defaultComparator, ViewerComparator comparator) {
		super(editor);
		fModel = model;
		fLabelProvider = lProvider;
		fContentProvider = cProvider;
		fDefaultComparator = defaultComparator;
		fViewerComparator = comparator;
		fListenerList = new ArrayList<>();
	}

	/**
	 * Creates the control for this outline page.
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		viewer = getTreeViewer();
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		if (sorted)
			viewer.setComparator(fViewerComparator);
		else
			viewer.setComparator(fDefaultComparator);
		viewer.setInput(fModel);
		viewer.expandAll();
	}

	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);
	}

	@Override
	public void reconciled(IDocument document) {
		final Control control = getControl();
		if (control == null || control.isDisposed())
			return;
		control.getDisplay().asyncExec(() -> {
			if (control.isDisposed())
				return;
			control.setRedraw(false);
			// Temporarily remove all selection listeners from the tree
			// viewer.  This is required because the refresh fires a
			// selection event back to the source page (observered in
			// the bundle source page) when typing
			removeAllSelectionChangedListeners();
			getTreeViewer().refresh();
			addAllSelectionChangedListeners();
			getTreeViewer().expandAll();
			control.setRedraw(true);
		});
	}

	@Override
	public void sort(boolean sorting) {
		sorted = sorting;
		if (isViewerDefined()) {
			if (sorting) {
				viewer.setComparator(fViewerComparator);
			} else {
				viewer.setComparator(fDefaultComparator);
			}
		}
	}

	/**
	 * Used for restoration after temporary removal.  Uses listeners cached.
	 */
	public void addAllSelectionChangedListeners() {
		// Re-add the tree listener added by our parent for our parent:
		// org.eclipse.ui.views.contentoutline.ContentOutlinePage
		if (isViewerDefined()) {
			viewer.addSelectionChangedListener(this);
		}
		// Add all current listeners
		for (int i = 0; i < fListenerList.size(); i++) {
			super.addSelectionChangedListener(fListenerList.get(i));
		}
	}

	private boolean isViewerDefined() {
		if (viewer == null) {
			return false;
		} else if (viewer.getTree().isDisposed()) {
			return false;
		}
		return true;
	}

	/**
	 * Used for temporary removal.  Listeners cached.
	 */
	public void removeAllSelectionChangedListeners() {
		// Remove the tree listener added by our parent for our parent:
		// org.eclipse.ui.views.contentoutline.ContentOutlinePage
		if (isViewerDefined()) {
			viewer.removeSelectionChangedListener(this);
		}
		// Remove all current listeners
		for (int i = 0; i < fListenerList.size(); i++) {
			super.removeSelectionChangedListener(fListenerList.get(i));
		}
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		// Add the listener to our private list
		fListenerList.add(listener);
		super.addSelectionChangedListener(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		// Remove the listener from our private list
		fListenerList.remove(listener);
		super.removeSelectionChangedListener(listener);
	}
}
