/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.contentoutline.*;

public class PDEMultiPageContentOutline
	implements IContentOutlinePage, ISelectionProvider, ISelectionChangedListener {
	private PageBook pagebook;
	private ISelection selection;
	private ArrayList listeners;
	private IContentOutlinePage currentPage;
	private IContentOutlinePage emptyPage;
	private IActionBars actionBars;

	public PDEMultiPageContentOutline() {
		listeners = new ArrayList();
	}
	
	public void addFocusListener(FocusListener listener) {
	}
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}
	public void createControl(Composite parent) {
		pagebook = new PageBook(parent, SWT.NONE);
	}

	public void dispose() {
		if (pagebook != null && !pagebook.isDisposed())
			pagebook.dispose();
		if (emptyPage!=null) {
			emptyPage.dispose();
			emptyPage=null;
		}
		pagebook = null;
		listeners = null;
	}

	public boolean isDisposed() {
		return listeners==null;
	}

	public Control getControl() {
		return pagebook;
	}
	public PageBook getPagebook() {
		return pagebook;
	}
	public ISelection getSelection() {
		return selection;
	}
	public void makeContributions(
		IMenuManager menuManager,
		IToolBarManager toolBarManager,
		IStatusLineManager statusLineManager) {
	}
	public void removeFocusListener(FocusListener listener) {
	}
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}
	public void selectionChanged(SelectionChangedEvent event) {
		setSelection(event.getSelection());
	}

	public void setActionBars(IActionBars actionBars) {
		this.actionBars = actionBars;
		if (currentPage != null)
			setPageActive(currentPage);
	}
	public IActionBars getActionBars() {
		return actionBars;
	}
	public void setFocus() {
		if (currentPage != null)
			currentPage.setFocus();
	}
	private IContentOutlinePage getEmptyPage() {
		if (emptyPage==null)
			emptyPage = new EmptyOutlinePage();
		return emptyPage;
	}
	public void setPageActive(IContentOutlinePage page) {
		if (page==null) {
			page = getEmptyPage();
		}
		if (currentPage != null) {
			currentPage.removeSelectionChangedListener(this);
		}
		page.addSelectionChangedListener(this);
		this.currentPage = page;
		if (pagebook == null) {
			// still not being made
			return;
		}
		Control control = page.getControl();
		if (control == null || control.isDisposed()) {
			// first time
			page.createControl(pagebook);
			page.setActionBars(getActionBars());			
			control = page.getControl();
		}
		pagebook.showPage(control);
		this.currentPage = page;
	}
	/**
	 * Set the selection.
	 */
	public void setSelection(ISelection selection) {
		this.selection =selection;
		if (listeners == null)
			return;
		SelectionChangedEvent e = new SelectionChangedEvent(this, selection);
		for (int i=0; i<listeners.size(); i++) {
			((ISelectionChangedListener)listeners.get(i)).selectionChanged(e);
		}	
	}

}
