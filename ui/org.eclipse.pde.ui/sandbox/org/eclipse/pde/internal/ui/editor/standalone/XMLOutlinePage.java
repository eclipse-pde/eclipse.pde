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

package org.eclipse.pde.internal.ui.editor.standalone;

import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.pde.internal.ui.editor.standalone.text.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.contentoutline.*;

/**
 * Content outline page for the XML editor.
 */
public class XMLOutlinePage extends ContentOutlinePage implements IReconcilingParticipant {
	
	
	private IContentProvider fContentProvider;
	private IBaseLabelProvider fLabelProvider;
	private IDocumentNode fModel;
	private NotifyingReconciler fReconciler;
	
	/**
	 * Creates a new XMLContentOutlinePage.
	 */
	public XMLOutlinePage(IReconciler reconciler) {
		super();
		fReconciler = (NotifyingReconciler)reconciler;
		fReconciler.addReconcilingParticipant(this);	
	}
	
	public void setContentProvider(IContentProvider contentProvider) {
		fContentProvider= contentProvider;
	}
	
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		fLabelProvider= labelProvider;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.text.IReconcilingParticipant#reconciled()
	 */
	public void reconciled() {
		final Control control = getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					control.setRedraw(false);
					getTreeViewer().refresh();
					control.setRedraw(true);
					getTreeViewer().expandAll();
				}
			});
		}
	}
	
	/**
	 * Sets the input of this page.
	 * @param xmlElement
	 */
	public void setPageInput(IDocumentNode xmlModel) {
		fModel= xmlModel;
		if (getTreeViewer() != null)
			getTreeViewer().setInput(fModel);
	}
	
	/*
	 * @see org.eclipse.ui.part.IPage#dispose()
	 */
	public void dispose() {
		fReconciler.removeReconcilingParticipant(this);
	}
	
	/**  
	 * Creates the control for this outline page.
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);	
		TreeViewer viewer= getTreeViewer();
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		
		if (fModel != null) {
			viewer.setInput(fModel);	
			viewer.expandAll();
		}
	}
	
}
