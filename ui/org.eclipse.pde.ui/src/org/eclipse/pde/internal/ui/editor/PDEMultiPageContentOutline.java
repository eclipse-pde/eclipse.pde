package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.swt.*;

public class PDEMultiPageContentOutline implements IContentOutlinePage, ISelectionChangedListener {
	private PageBook pagebook;
	private SelectionProvider selectionProvider;
	private PDEMultiPageEditor editor;
	private IContentOutlinePage currentPage;
	private boolean disposed;


public PDEMultiPageContentOutline(PDEMultiPageEditor editor) {
	this.editor = editor;
	selectionProvider = new SelectionProvider();
}
public void addFocusListener(org.eclipse.swt.events.FocusListener listener) {}
public void addSelectionChangedListener(ISelectionChangedListener listener) {
	selectionProvider.addSelectionChangedListener(listener);
}
public void createControl(Composite parent) {
	pagebook = new PageBook(parent, SWT.NONE);
	if (currentPage!=null) setPageActive(currentPage);
}
public void dispose() {
	if (pagebook != null && !pagebook.isDisposed())
		pagebook.dispose();
	pagebook = null;
	disposed = true;
}

public boolean isDisposed() {
	return disposed;
}

public Control getControl() {
	return pagebook;
}
public PageBook getPagebook() {
	return pagebook;
}
public ISelection getSelection() {
	return selectionProvider.getSelection();
}
public void makeContributions(
	IMenuManager menuManager,
	IToolBarManager toolBarManager,
	IStatusLineManager statusLineManager) {
}
public void removeFocusListener(FocusListener listener) {}
public void removeSelectionChangedListener(ISelectionChangedListener listener) {
	selectionProvider.removeSelectionChangedListener(listener);
}
public void selectionChanged(SelectionChangedEvent event) {
	selectionProvider.setSelection(event.getSelection());
}
public void setActionBars(org.eclipse.ui.IActionBars actionBars) {}
public void setFocus() {
	if (currentPage != null)
		currentPage.setFocus();
}
public void setPageActive(IContentOutlinePage page) {
	if (currentPage!=null) {
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
		control = page.getControl();

	}
	pagebook.showPage(control);
	this.currentPage = page;
}
	/**
	 * Set the selection.
	 */
public void setSelection(ISelection selection) {
	selectionProvider.setSelection(selection);
}
}
