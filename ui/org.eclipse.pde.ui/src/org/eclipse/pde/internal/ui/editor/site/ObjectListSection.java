package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.SiteObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class ObjectListSection
	extends TableSection
	implements IModelProviderListener {
	private static final String POPUP_NEW = "Menus.new.label";
	private static final String POPUP_DELETE = "Actions.delete.label";
	private boolean updateNeeded;

	protected TableViewer tableViewer;
	private Action openAction;
	private Action newAction;
	private Action deleteAction;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return ObjectListSection.this.getElements(parent);
		}
	}

	public ObjectListSection(
		PDEFormPage page,
		String title,
		String desc,
		String[] buttons) {
		super(page, buttons);
		setHeaderText(title);
		setDescription(desc);
		getTablePart().setEditable(false);
	}

	protected abstract Object[] getElements(Object parent);

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 9;

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		EditableTablePart tablePart = getTablePart();
		tableViewer = tablePart.getTableViewer();
		tableViewer.setContentProvider(new PluginContentProvider());
		tableViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
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
		if (isOpenable())
			manager.add(openAction);
		manager.add(deleteAction);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	protected abstract void handleOpen();
	protected abstract void handleNew();
	protected abstract String getOpenPopupLabel();

	protected void handleDelete() {
		IStructuredSelection ssel =
			(IStructuredSelection) tableViewer.getSelection();

		if (ssel.isEmpty())
			return;
		try {
			remove(tableViewer.getInput(), ssel.toList());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected abstract void remove(Object input, List objects)
		throws CoreException;

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
				.showWhile(tableViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator
				.showWhile(tableViewer.getTable().getDisplay(), new Runnable() {
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
		boolean singleSelection = selection.size() == 1;
		if (openAction!=null) openAction.setEnabled(singleSelection);
		deleteAction.setEnabled(selection.isEmpty() == false);
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

	private void makeActions() {
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
			openAction.setEnabled(false);
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

	protected abstract boolean isValidObject(Object object);

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object, Object[])
	 */
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (isValidObject(objects[i]) == false)
				return false;
		}
		return true;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISite site = model.getSite();

		ArrayList siteObjects = new ArrayList();
		for (int i = 0; i < objects.length; i++) {
			if (isValidObject(objects[i])) {
				SiteObject sobj = (SiteObject) objects[i];
				sobj.setModel(model);
				sobj.setParent(site);
				siteObjects.add(sobj);
			}
		}
		try {
			accept(site, siteObjects);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	protected abstract void accept(ISite site, ArrayList siteObjects)
		throws CoreException;
}