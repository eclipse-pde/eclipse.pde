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

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.contentoutline.*;

/**
 * Content outline page for the XML editor.
 */
public class SourceOutlinePage extends ContentOutlinePage implements IModelChangedListener {
	
	private IEditingModel fModel;
	private IBaseLabelProvider fLabelProvider;
	private IContentProvider fContentProvider;
	private ViewerSorter fViewerSorter;
	
	/**
	 * Creates a new XMLContentOutlinePage.
	 */
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
		fModel.addModelChangedListener(this);
		TreeViewer viewer= getTreeViewer();
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		viewer.setSorter(fViewerSorter);
		viewer.setInput(fModel);
		viewer.expandAll();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			getControl().getDisplay().asyncExec(new Runnable() {
				public void run() {
					getControl().setRedraw(false);
					getTreeViewer().refresh();
					getTreeViewer().expandAll();
					getControl().setRedraw(true);
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#init(org.eclipse.ui.part.IPageSite)
	 */
	public void init(IPageSite pageSite) {
		super.init(pageSite);
	}
	
}
