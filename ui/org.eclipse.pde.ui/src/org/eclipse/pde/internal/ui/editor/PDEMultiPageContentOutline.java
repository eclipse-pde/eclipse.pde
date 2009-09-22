/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class PDEMultiPageContentOutline extends Page implements IContentOutlinePage, ISelectionProvider, ISelectionChangedListener, ILaunchingPreferenceConstants {
	private PageBook pagebook;
	private ISelection selection;
	private ArrayList listeners;
	private ISortableContentOutlinePage currentPage;
	private ISortableContentOutlinePage emptyPage;
	private IActionBars actionBars;
	private boolean sortingOn;
	private PDEFormEditor editor;

	public PDEMultiPageContentOutline(PDEFormEditor editor) {
		this.editor = editor;
		listeners = new ArrayList();
		sortingOn = PDEPlugin.getDefault().getPreferenceStore().getBoolean("PDEMultiPageContentOutline.SortingAction.isChecked"); //$NON-NLS-1$

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
		if (emptyPage != null) {
			emptyPage.dispose();
			emptyPage = null;
		}
		pagebook = null;
		listeners = null;
	}

	public boolean isDisposed() {
		return listeners == null;
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

	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
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
		registerToolbarActions(actionBars);
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

	private ISortableContentOutlinePage getEmptyPage() {
		if (emptyPage == null)
			emptyPage = new EmptyOutlinePage();
		return emptyPage;
	}

	public void setPageActive(ISortableContentOutlinePage page) {
		if (page == null) {
			page = getEmptyPage();
		}
		if (currentPage != null) {
			currentPage.removeSelectionChangedListener(this);
		}
		//page.init(getSite());
		page.sort(sortingOn);
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
		this.selection = selection;
		if (listeners == null)
			return;
		SelectionChangedEvent e = new SelectionChangedEvent(this, selection);
		for (int i = 0; i < listeners.size(); i++) {
			((ISelectionChangedListener) listeners.get(i)).selectionChanged(e);
		}
	}

	private void registerToolbarActions(IActionBars actionBars) {

		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		if (toolBarManager != null) {
			toolBarManager.add(new ToggleLinkWithEditorAction(editor));
			toolBarManager.add(new SortingAction());
		}
	}

	class SortingAction extends Action {

		public SortingAction() {
			super();
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IHelpContextIds.OUTLINE_SORT_ACTION);
			setText(PDEUIMessages.PDEMultiPageContentOutline_SortingAction_label);
			setImageDescriptor(PDEPluginImages.DESC_ALPHAB_SORT_CO);
			setDisabledImageDescriptor(PDEPluginImages.DESC_ALPHAB_SORT_CO_DISABLED);
			setToolTipText(PDEUIMessages.PDEMultiPageContentOutline_SortingAction_tooltip);
			setDescription(PDEUIMessages.PDEMultiPageContentOutline_SortingAction_description);
			setChecked(sortingOn);
		}

		public void run() {
			setChecked(isChecked());
			valueChanged(isChecked());
		}

		private void valueChanged(final boolean on) {
			sortingOn = on;
			if (currentPage != null)
				currentPage.sort(on);
			PDEPlugin.getDefault().getPreferenceStore().setValue("PDEMultiPageContentOutline.SortingAction.isChecked", on); //$NON-NLS-1$
		}

	}
}
