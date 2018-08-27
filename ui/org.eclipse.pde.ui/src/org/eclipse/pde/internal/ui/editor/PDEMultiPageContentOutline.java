/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Denis Zygann <d.zygann@web.de> - 482155
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class PDEMultiPageContentOutline extends Page implements IContentOutlinePage, ISelectionChangedListener, ILaunchingPreferenceConstants {
	private PageBook pagebook;
	private ISelection selection;
	private ArrayList<ISelectionChangedListener> listeners;
	private ISortableContentOutlinePage currentPage;
	private ISortableContentOutlinePage emptyPage;
	private boolean sortingOn;
	private PDEFormEditor editor;
	private ToggleLinkWithEditorAction fToggleLinkWithEditorAction;

	public PDEMultiPageContentOutline(PDEFormEditor editor) {
		this.editor = editor;
		listeners = new ArrayList<>();
		selection = StructuredSelection.EMPTY;
		sortingOn = PDEPlugin.getDefault().getPreferenceStore().getBoolean("PDEMultiPageContentOutline.SortingAction.isChecked"); //$NON-NLS-1$

	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void createControl(Composite parent) {
		pagebook = new PageBook(parent, SWT.NONE);
		if (currentPage != null) {
			setPageActive(currentPage);
		}
	}

	@Override
	public void dispose() {
		if (pagebook != null && !pagebook.isDisposed())
			pagebook.dispose();
		if (emptyPage != null) {
			emptyPage.dispose();
			emptyPage = null;
		}
		if (currentPage != null) {
			currentPage.removeSelectionChangedListener(this);
			currentPage = null;
		}

		pagebook = null;
		listeners = null;
		editor = null;

		// Workaround for leak in Platform
		if (fToggleLinkWithEditorAction != null) {
			fToggleLinkWithEditorAction.fEditor = null;
			fToggleLinkWithEditorAction = null;
		}
	}

	public boolean isDisposed() {
		return listeners == null;
	}

	@Override
	public Control getControl() {
		return pagebook;
	}

	public PageBook getPagebook() {
		return pagebook;
	}

	@Override
	public ISelection getSelection() {
		return selection;
	}


	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		setSelection(event.getSelection());
	}


    @Override
    public void init(IPageSite pageSite) {
        super.init(pageSite);
        registerToolbarActions(pageSite.getActionBars());
    }

	@Override
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
		// page.init(getSite());
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
			control = page.getControl();
		}
		pagebook.showPage(control);
		this.currentPage = page;
	}

	/**
	 * Set the selection.
	 */
	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;
		if (listeners == null)
			return;
		SelectionChangedEvent e = new SelectionChangedEvent(this, selection);
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).selectionChanged(e);
		}
	}

	private void registerToolbarActions(IActionBars actionBars) {

		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		if (toolBarManager != null) {
			fToggleLinkWithEditorAction = new ToggleLinkWithEditorAction(editor);
			toolBarManager.add(fToggleLinkWithEditorAction);
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

		@Override
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
