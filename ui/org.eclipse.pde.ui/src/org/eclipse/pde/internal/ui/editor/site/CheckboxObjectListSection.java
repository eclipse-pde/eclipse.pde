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
package org.eclipse.pde.internal.ui.editor.site;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.CheckboxTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class CheckboxObjectListSection
	extends CheckboxTableSection
	implements IModelProviderListener {
	private static final String POPUP_NEW = "Menus.new.label";
	private static final String POPUP_DELETE = "Actions.delete.label";
	protected boolean updateNeeded;

	protected CheckboxTableViewer tableViewer;
	private Action openAction;
	private Action newAction;
	private Action deleteAction;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return CheckboxObjectListSection.this.getElements(parent);
		}
	}

	public CheckboxObjectListSection(
		PDEFormPage page,
		String title,
		String desc,
		String[] buttons) {
		super(page, buttons);
		setHeaderText(title);
		setDescription(desc);
	}

	protected abstract Object[] getElements(Object parent);

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 9;

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		CheckboxTablePart tablePart = getTablePart();
		tableViewer = tablePart.getTableViewer();
		tableViewer.setContentProvider(new PluginContentProvider());
		tableViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);
		makeActions();
		return container;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen();
	}

	protected void buttonSelected(int index) {
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	protected abstract boolean isApplicable(Object object);

	public void expandTo(Object object) {
		if (isApplicable(object))
			tableViewer.setSelection(new StructuredSelection(object), true);
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(newAction);
		manager.add(new Separator());
		if (isOpenable()) manager.add(openAction);
		if (canDelete((IStructuredSelection)tableViewer.getSelection()))
			manager.add(deleteAction);
		fillClientActions(manager);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	protected void fillClientActions(IMenuManager manager) {
	}
	protected abstract void handleOpen();
	protected abstract void handleNew();
	protected abstract String getOpenPopupLabel();
	
	protected abstract boolean canDelete(IStructuredSelection selection);

	protected void handleDelete() {
		IStructuredSelection ssel =
			(IStructuredSelection) tableViewer.getSelection();

		if (ssel.isEmpty())
			return;
		try {
			if (canDelete(ssel))
				remove(tableViewer.getInput(), ssel.toList());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected abstract void remove(Object input, List objects) throws CoreException;

	private void handleSelectAll() {
		IStructuredContentProvider provider =
			(IStructuredContentProvider) tableViewer.getContentProvider();
		Object[] elements = provider.getElements(tableViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		tableViewer.setSelection(ssel);
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			BusyIndicator
				.showWhile(
					tableViewer.getTable().getDisplay(),
					new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator
				.showWhile(
					tableViewer.getTable().getDisplay(),
					new Runnable() {
				public void run() {
					handleSelectAll();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.CUT)) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(IWorkbenchActionConstants.PASTE)) {
			doPaste();
			return true;
		}
		return false;
	}
	protected void selectionChanged(IStructuredSelection selection) {
		getFormPage().setSelection(selection);
		if (selection.size() == 1)
			fireSelectionNotification(selection.getFirstElement());
		else
			fireSelectionNotification(null);
	}

	public void initialize(Object input) {
		ISiteModel model = (ISiteModel) input;
		update(input);

		if (model.isEditable() == false) {
			setButtonsEnabled(false);
		}
		model.addModelChangedListener(this);
	}

	protected abstract void setButtonsEnabled(boolean enabled);
	protected abstract boolean isOpenable();

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
			if (getFormPage().isVisible()) {
				update();
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (isApplicable(obj)) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					tableViewer.update(obj, null);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					tableViewer.add(e.getChangedObjects());
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					tableViewer.remove(e.getChangedObjects());
				}
			}
		}
	}

	protected void makeActions() {
		IModel model = (IModel) getFormPage().getModel();
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
		newAction.setEnabled(model.isEditable());

		deleteAction = new Action() {
			public void run() {
				BusyIndicator
					.showWhile(
						tableViewer.getTable().getDisplay(),
						new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		deleteAction.setEnabled(model.isEditable());
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));

		if (isOpenable()) {
			openAction = new Action() {
				public void run() {
					handleOpen();
				}
			};
			openAction.setEnabled(model.isEditable());
			openAction.setText(getOpenPopupLabel());
		}
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (tableViewer != null)
			tableViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISite site = model.getSite();
		tableViewer.setInput(site);
		updateNeeded = false;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
	}
}
