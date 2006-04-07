/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.plugin.PluginReference;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyExtentAction;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.Page;

public abstract class DependenciesViewPage extends Page {
	class FocusOnSelectionAction extends Action {
		public void run() {
			handleFocusOn(getSelectedObject());
		}

		public void update(Object object) {
			setEnabled(object != null);
			String name = ((LabelProvider) fViewer.getLabelProvider())
					.getText(object);
			setText(NLS.bind(PDEUIMessages.DependenciesViewPage_focusOnSelection, name)); 
		}
	}

	private Action fFocusOnAction;

	private FocusOnSelectionAction fFocusOnSelectionAction;

	private Action fOpenAction;

	private IPropertyChangeListener fPropertyListener;

	private DependenciesView fView;

	protected StructuredViewer fViewer;

	/**
	 * 
	 */
	public DependenciesViewPage(DependenciesView view) {
		this.fView = view;
		fPropertyListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
					fViewer.refresh();
				}
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fViewer = createViewer(parent);
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
				fPropertyListener);
		getSite().setSelectionProvider(fViewer);
	}

	protected abstract StructuredViewer createViewer(Composite parent);

	public void dispose() {
		PDEPlugin.getDefault().getPreferenceStore()
				.removePropertyChangeListener(fPropertyListener);
		super.dispose();
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) fViewer
				.getSelection();

		if (selection.size() == 1) {
			manager.add(fOpenAction);
			manager.add(new Separator());
		}
		fFocusOnSelectionAction.update(getSelectedObject());
		if (fFocusOnSelectionAction.isEnabled())
			manager.add(fFocusOnSelectionAction);
		manager.add(fFocusOnAction);
		Object selectionElement = selection.getFirstElement();

		manager.add(new Separator());
		if (selection.size() == 1) {
			// Compute dep extent
			// manager.add(new Separator());
			// PluginSearchActionGroup actionGroup = new
			// PluginSearchActionGroup();
			// actionGroup.setContext(new ActionContext(selection));
			// actionGroup.fillContextMenu(manager);
			Object importObj = selectionElement;
			if (importObj instanceof IPluginImport) {
				String id = ((IPluginImport) importObj).getId();
				IResource resource = ((IPluginImport) importObj).getModel()
						.getUnderlyingResource();
				if (resource != null) {
					manager.add(new DependencyExtentAction(resource
							.getProject(), id));
				}
			}

		}
		// Unused Dependencies Action, only for worskpace plug-ins
		ISharedPluginModel model = null;
		if (selectionElement instanceof PluginReference) {
			selectionElement = ((PluginReference) selectionElement).getPlugin();
		}
		if (selectionElement instanceof IPluginObject) {
			model = ((IPluginObject) selectionElement).getModel();
		}
		if (model != null && model.getUnderlyingResource() != null) {
			manager.add(new UnusedDependenciesAction(
					(IPluginModelBase) model, true));
		}
		//
		manager.add(new Separator());
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return fViewer.getControl();
	}

	private Object getSelectedObject() {
		IStructuredSelection selection = getSelection();
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		return selection.getFirstElement();
	}

	protected IStructuredSelection getSelection() {
		return (IStructuredSelection) fViewer.getSelection();
	}
	
	protected void setSelection(IStructuredSelection selection) {
		fViewer.setSelection(selection, false);
	}
	
	/**
	 * @return Returns the view.
	 */
	public DependenciesView getView() {
		return fView;
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		if (obj instanceof IPluginImport) {
			ManifestEditor.openPluginEditor(((IPluginImport) obj).getId());
		} else {
			ManifestEditor.open(obj, false);
		}
	}

	private void handleFocusOn() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(fViewer
				.getControl().getShell(), true, false);
		dialog.create();
		if (dialog.open() == Window.OK) {
			handleFocusOn(dialog.getFirstResult());
		}
	}

	private void handleFocusOn(Object newFocus) {
		if (newFocus instanceof IPluginModelBase) {
			fView.openTo(newFocus);
		}
		if (newFocus instanceof IPluginBase) {
			fView.openTo(((IPluginBase) newFocus).getModel());
		}
		if (newFocus instanceof IPluginImport) {
			IPluginImport pluginImport = ((IPluginImport) newFocus);
			String id = pluginImport.getId();
			IPlugin importedPlugin = PDECore.getDefault().findPlugin(id);
			if (importedPlugin != null) {
				fView.openTo(importedPlugin.getModel());
			} else {
				fView.openTo(null);
			}
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DependenciesViewPage.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);

		getSite()
				.registerContextMenu(fView.getSite().getId(), menuMgr, fViewer);
	}

	private void hookDoubleClickAction() {
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});
	}

	private void makeActions() {
		fOpenAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		fOpenAction.setText(PDEUIMessages.DependenciesView_open); 

		fFocusOnSelectionAction = new FocusOnSelectionAction();

		fFocusOnAction = new Action() {
			public void run() {
				handleFocusOn();
			}
		};
		fFocusOnAction.setText(PDEUIMessages.DependenciesViewPage_focusOn); 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.Page#makeContributions(org.eclipse.jface.action.IMenuManager,
	 *      org.eclipse.jface.action.IToolBarManager,
	 *      org.eclipse.jface.action.IStatusLineManager)
	 */
	public void makeContributions(IMenuManager menuManager,
			IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		if (fViewer != null) {
			Control c = fViewer.getControl();
			if (!c.isFocusControl()) {
				c.setFocus();
			}
		}
	}

	public void setInput(Object object) {
		if (object != fViewer.getInput())
			fViewer.setInput(object);
	}
}
