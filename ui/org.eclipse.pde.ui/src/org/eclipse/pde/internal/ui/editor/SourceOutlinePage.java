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

import org.eclipse.jface.action.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.contentoutline.*;

/**
 * Content outline page for the XML editor.
 */
public class SourceOutlinePage extends ContentOutlinePage implements IReconcilingParticipant{
	
	private IEditingModel fModel;
	private IBaseLabelProvider fLabelProvider;
	private IContentProvider fContentProvider;
	private ViewerSorter fViewerSorter;
	
	public SourceOutlinePage(IEditingModel model, IBaseLabelProvider lProvider, IContentProvider cProvider, ViewerSorter sorter) {
		super();
		fModel = model;
		fLabelProvider = lProvider;
		fContentProvider = cProvider;
		fViewerSorter = sorter;
	}
		
	/**  
	 * Creates the control for this outline page.
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer= getTreeViewer();
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		viewer.setSorter(fViewerSorter);
		viewer.setInput(fModel);
		viewer.expandAll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#init(org.eclipse.ui.part.IPageSite)
	 */
	public void init(IPageSite pageSite) {
		super.init(pageSite);
	}
	
	public void makeContributions(
			IMenuManager menuManager, 
			IToolBarManager toolBarManager, 
			IStatusLineManager statusLineManager) {
		//Create actions and contribute into the provided managers
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.text.IReconcilingParticipant#reconciled(org.eclipse.jface.text.IDocument)
	 */
	public void reconciled(IDocument document) {
		final Control control = getControl();
		if (control == null)
			return;
		control.getDisplay().asyncExec(new Runnable() {
			public void run() {
				control.setRedraw(false);
				getTreeViewer().refresh();
				getTreeViewer().expandAll();
				control.setRedraw(true);
			}
		});
	}
}
