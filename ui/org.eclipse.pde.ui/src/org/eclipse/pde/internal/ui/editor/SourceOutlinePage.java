/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;

/**
 * Content outline page for the XML editor.
 */
public class SourceOutlinePage extends PDEOutlinePage implements IReconcilingParticipant, ISortableContentOutlinePage{
	
	private IEditingModel fModel;
	private IBaseLabelProvider fLabelProvider;
	private IContentProvider fContentProvider;
	private ViewerSorter fDefaultSorter;
	private ViewerSorter fViewerSorter;
	private boolean sorted;
	TreeViewer viewer;
	
	public SourceOutlinePage(IEditingModel model, IBaseLabelProvider lProvider,
			IContentProvider cProvider, ViewerSorter defaultSorter,
			ViewerSorter sorter) {
		super();
		fModel = model;
		fLabelProvider = lProvider;
		fContentProvider = cProvider;
		fDefaultSorter = defaultSorter;
		fViewerSorter = sorter;
	}
		
	/**  
	 * Creates the control for this outline page.
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		viewer= getTreeViewer();
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		if(sorted)
			viewer.setSorter(fViewerSorter);
		else
			viewer.setSorter(fDefaultSorter);
		viewer.setInput(fModel);
		viewer.expandAll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#init(org.eclipse.ui.part.IPageSite)
	 */
	public void init(IPageSite pageSite) {
		super.init(pageSite);
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.text.IReconcilingParticipant#reconciled(org.eclipse.jface.text.IDocument)
	 */
	public void reconciled(IDocument document) {
		final Control control = getControl();
		if (control == null || control.isDisposed())
			return;
		control.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if(control.isDisposed()){
					return;
				}
				control.setRedraw(false);
				getTreeViewer().refresh();
				getTreeViewer().expandAll();
				control.setRedraw(true);
			}
		});
	}
	public void sort (boolean sorting){
		sorted = sorting;
		if(viewer!=null)
			if(sorting)
				viewer.setSorter(fViewerSorter);
			else
				viewer.setSorter(fDefaultSorter);
	}
}
