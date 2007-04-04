/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

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
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.refactoring.RenamePluginAction;
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
	
	protected RenamePluginAction fRefactorAction;

	private IPropertyChangeListener fPropertyListener;

	private DependenciesView fView;

	protected StructuredViewer fViewer;
	
	protected IContentProvider fContentProvider;

	/**
	 * 
	 */
	public DependenciesViewPage(DependenciesView view, IContentProvider contentProvider) {
		this.fView = view;
		this.fContentProvider = contentProvider;
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
		// only show Find Dependency Extent when in Callees view
		if (selection.size() == 1 && fViewer.getContentProvider() instanceof CalleesContentProvider) {
			String id = null;
			if (selectionElement instanceof BundleSpecification) {
				id = ((BundleSpecification)selectionElement).getName();
			} else if (selectionElement instanceof BundleDescription) {
				id = ((BundleDescription)selectionElement).getSymbolicName();
			}
			// don't include find dependency extent for unresolved imports or bundles
			if (id != null && PluginRegistry.findModel(id) != null) {
				Object input = fViewer.getInput();
				if (input instanceof IPluginBase)
					input = ((IPluginBase)input).getModel();
				if (input instanceof IPluginModelBase) {
					IPluginModelBase base = (IPluginModelBase)input;
					IResource res = (base == null) ? null : base.getUnderlyingResource();
					if (res != null)
						manager.add(new DependencyExtentAction(res
								.getProject(), id));
				}
			}
		}
		// Unused Dependencies Action, only for worskpace plug-ins
		ISharedPluginModel model = null;
		if (selectionElement instanceof BundleSpecification) {
			model = PluginRegistry.findModel(((BundleSpecification)selectionElement).getName());
		} else if (selectionElement instanceof BundleDescription) {
			model = PluginRegistry.findModel((BundleDescription)selectionElement);
		} else if (selectionElement instanceof IPluginBase) {
			// root
			model = ((IPluginBase)selectionElement).getModel();
		}
		if (model != null && model.getUnderlyingResource() != null) {
			manager.add(new UnusedDependenciesAction(
					(IPluginModelBase) model, true));
		}
		if (enableRename(selection)) {
			manager.add(new Separator());
			manager.add(fRefactorAction);
		}

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
		if (selection != null && !selection.isEmpty())
			fViewer.setSelection(selection, true);
	}
	
	/**
	 * @return Returns the view.
	 */
	public DependenciesView getView() {
		return fView;
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		BundleDescription desc = null;
		if (obj instanceof BundleSpecification) {
			desc = (BundleDescription)((BundleSpecification)obj).getSupplier();
		} else if (obj instanceof BundleDescription) {
			desc = (BundleDescription)obj;
			
		} else if (obj instanceof IPluginBase) {
			// root object
			desc = ((IPluginModelBase)((IPluginBase)obj).getModel()).getBundleDescription();
		}
		if (desc != null)
			ManifestEditor.openPluginEditor(desc.getSymbolicName());
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
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				fView.openTo(model);
			} else {
				fView.openTo(null);
			}
		}
		BundleDescription desc = null;
		if (newFocus instanceof BundleSpecification) {
			desc = (BundleDescription)((BundleSpecification)newFocus).getSupplier();
			if (desc == null)
				fView.openTo(null);
		}
		if (newFocus instanceof BundleDescription) {
			desc = (BundleDescription)newFocus;
		}
		if (desc != null)
			fView.openTo(PluginRegistry.findModel(desc.getSymbolicName()));
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
		
		fRefactorAction = new RenamePluginAction();
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
	
	// returns true if Rename Action is valid.
	protected boolean enableRename(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object selectionElement = selection.getFirstElement();
			IPluginModelBase base = null;
			if (selectionElement instanceof IPluginImport) {
				String id = ((IPluginImport)selectionElement).getId();
				base = PluginRegistry.findModel(id);
			} else if (selectionElement instanceof IPluginObject) {
				base = (IPluginModelBase)((IPluginObject) selectionElement).getModel();
			} else if (selectionElement instanceof BundleSpecification) {
				BundleDescription desc = (BundleDescription)((BundleSpecification)selectionElement).getSupplier();
				if (desc != null)
					base = PluginRegistry.findModel(desc);
			} else if (selectionElement instanceof BundleDescription) {
				base = PluginRegistry.findModel((BundleDescription)selectionElement);
			}
			if (base != null && base.getUnderlyingResource() != null) {
				fRefactorAction.setPlugin(base);
				return true;
			}
		}
		return false;
	}
	
	public void setActive(boolean active) {
		if (fContentProvider instanceof DependenciesViewPageContentProvider) {
			if (active) {
				// when a page is activated, we need to have the content provider listen for changes and refresh the view to get current data
				((DependenciesViewPageContentProvider)fContentProvider).attachModelListener();
				fViewer.refresh();
			} else
				// when page is deactivated, we need to remove model listener from content manager.  Otherwise model changes will be sent to all 
				// DependenciesViewPageContentProvider (including inactive ones).  This will cause problems with the content provider's logic!!
				((DependenciesViewPageContentProvider)fContentProvider).removeModelListener();
		}
	}
}
