/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.refactoring.PDERefactoringAction;
import org.eclipse.pde.internal.ui.refactoring.RefactoringActionFactory;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyExtentAction;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.views.plugins.ImportActionGroup;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.Page;
import org.osgi.resource.Resource;

public abstract class DependenciesViewPage extends Page {
	class FocusOnSelectionAction extends Action {
		@Override
		public void run() {
			handleFocusOn(getSelectedObject());
		}

		public void update(Object object) {
			setEnabled(object != null);
			String name = ((LabelProvider) fViewer.getLabelProvider()).getText(object);
			setText(NLS.bind(PDEUIMessages.DependenciesViewPage_focusOnSelection, name));
			setImageDescriptor(PDEPluginImages.DESC_FOCUS_ON);
		}
	}

	private Action fFocusOnAction;

	private FocusOnSelectionAction fFocusOnSelectionAction;

	private Action fOpenAction;

	protected PDERefactoringAction fRefactorAction;

	private final IPropertyChangeListener fPropertyListener;

	private final DependenciesView fView;

	protected StructuredViewer fViewer;

	protected IContentProvider fContentProvider;

	private Action fHideFragmentFilterAction;

	protected Action fHideOptionalFilterAction;

	private final FragmentFilter fHideFragmentFilter = new FragmentFilter();

	private static final String HIDE_FRAGMENTS = "hideFrags"; //$NON-NLS-1$

	private static final String HIDE_OPTIONAL = "hideOptional"; //$NON-NLS-1$

	static class FragmentFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer v, Object parent, Object element) {
			BundleDescription desc = null;
			if (element instanceof BundleSpecification) {
				BaseDescription supplier = ((BundleSpecification) element).getSupplier();
				if (supplier instanceof BundleDescription)
					desc = (BundleDescription) supplier;
			} else if (element instanceof BundleDescription) {
				desc = (BundleDescription) element;
			} else if (element instanceof ImportPackageSpecification) {
				BaseDescription export = ((ImportPackageSpecification) element).getSupplier();
				desc = ((ExportPackageDescription) export).getExporter();
			}
			if (desc != null) {
				return desc.getHost() == null;
			}
			return true;
		}
	}

	public DependenciesViewPage(DependenciesView view, IContentProvider contentProvider) {
		this.fView = view;
		this.fContentProvider = contentProvider;
		fPropertyListener = event -> {
			String property = event.getProperty();
			if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
				fViewer.refresh();
			}
		};
	}

	@Override
	public void createControl(Composite parent) {
		fViewer = createViewer(parent);
		fViewer.setComparator(DependenciesViewComparator.getViewerComparator());
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
		getSite().setSelectionProvider(fViewer);
	}

	protected abstract StructuredViewer createViewer(Composite parent);

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		super.dispose();
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = fViewer.getStructuredSelection();

		if (selection.size() == 1) {
			manager.add(fOpenAction);
			manager.add(new Separator());
		}

		if (ImportActionGroup.canImport(selection)) {
			ImportActionGroup actionGroup = new ImportActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
			manager.add(new Separator());
		}

		fFocusOnSelectionAction.update(getSelectedObject());
		if (fFocusOnSelectionAction.isEnabled())
			manager.add(fFocusOnSelectionAction);
		manager.add(fFocusOnAction);
		Object selectionElement = selection.getFirstElement();

		manager.add(new Separator());
		// only show Find Dependency Extent when in Callees view
		if (selection.size() == 1 && !fView.isShowingCallers()) {
			String id = null;
			if (selectionElement instanceof BundleSpecification) {
				id = ((BundleSpecification) selectionElement).getName();
			} else if (selectionElement instanceof BundleDescription) {
				id = ((BundleDescription) selectionElement).getSymbolicName();
			}
			// don't include find dependency extent for unresolved imports or bundles
			if (id != null && PluginRegistry.findModel(id) != null) {
				Object input = fViewer.getInput();
				if (input instanceof IPluginBase)
					input = ((IPluginBase) input).getModel();
				if (input instanceof IPluginModelBase base) {
					IResource res = base.getUnderlyingResource();
					if (res != null)
						manager.add(new DependencyExtentAction(res.getProject(), id));
				}
			}
		}
		// Unused Dependencies Action, only for workspace plug-ins
		ISharedPluginModel model = null;
		if (selectionElement instanceof BundleSpecification) {
			model = PluginRegistry.findModel(((BundleSpecification) selectionElement).getName());
		} else if (selectionElement instanceof Resource) {
			model = PluginRegistry.findModel((Resource) selectionElement);
		} else if (selectionElement instanceof IPluginBase) {
			// root
			model = ((IPluginBase) selectionElement).getModel();
		}
		if (model != null && model.getUnderlyingResource() != null) {
			manager.add(new UnusedDependenciesAction((IPluginModelBase) model, true));
		}
		if (enableRename(selection)) {
			manager.add(new Separator());
			manager.add(fRefactorAction);
		}

		manager.add(new Separator());
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
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
		return fViewer.getStructuredSelection();
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
			desc = (BundleDescription) ((BundleSpecification) obj).getSupplier();
		} else if (obj instanceof BundleDescription) {
			desc = (BundleDescription) obj;

		} else if (obj instanceof IPluginBase) {
			// root object
			desc = ((IPluginModelBase) ((IPluginBase) obj).getModel()).getBundleDescription();
		} else if (obj instanceof ImportPackageSpecification) {
			BaseDescription export = ((ImportPackageSpecification) obj).getSupplier();
			desc = ((ExportPackageDescription) export).getExporter();
		}
		if (desc != null)
			ManifestEditor.openPluginEditor(desc);
	}

	private void handleFocusOn() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(fViewer.getControl().getShell(), true, false);
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
		if (newFocus instanceof IPluginImport pluginImport) {
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
			desc = (BundleDescription) ((BundleSpecification) newFocus).getSupplier();
			if (desc == null)
				fView.openTo(null);
		}
		if (newFocus instanceof BundleDescription) {
			desc = (BundleDescription) newFocus;
		}
		if (desc != null)
			fView.openTo(PluginRegistry.findModel(desc.getSymbolicName()));
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(DependenciesViewPage.this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(fView.getSite().getId(), menuMgr, fViewer);
	}

	private void hookDoubleClickAction() {
		fViewer.addDoubleClickListener(event -> handleDoubleClick());
	}

	private void makeActions() {
		fOpenAction = new Action() {
			@Override
			public void run() {
				handleDoubleClick();
			}
		};
		fOpenAction.setText(PDEUIMessages.DependenciesView_open);

		fFocusOnSelectionAction = new FocusOnSelectionAction();

		fFocusOnAction = new Action() {
			@Override
			public void run() {
				handleFocusOn();
			}
		};
		fFocusOnAction.setText(PDEUIMessages.DependenciesViewPage_focusOn);
		fFocusOnAction.setImageDescriptor(PDEPluginImages.DESC_FOCUS_ON);

		fRefactorAction = RefactoringActionFactory.createRefactorPluginIdAction();

		fHideFragmentFilterAction = new Action() {
			@Override
			public void run() {
				boolean checked = fHideFragmentFilterAction.isChecked();
				if (checked)
					fViewer.removeFilter(fHideFragmentFilter);
				else
					fViewer.addFilter(fHideFragmentFilter);
				getSettings().put(HIDE_FRAGMENTS, !checked);
			}
		};
		fHideFragmentFilterAction.setText(PDEUIMessages.DependenciesViewPage_showFragments);

		fHideOptionalFilterAction = new Action() {
			@Override
			public void run() {
				boolean checked = isChecked();
				handleShowOptional(isChecked(), true);
				getSettings().put(HIDE_OPTIONAL, !checked);
			}
		};
		fHideOptionalFilterAction.setText(PDEUIMessages.DependenciesViewPage_showOptional);
	}

	protected abstract void handleShowOptional(boolean checked, boolean refreshIfNecessary);

	protected abstract boolean isShowingOptional();

	@Override
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars(getSite().getActionBars());
	}

	@Override
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
				String id = ((IPluginImport) selectionElement).getId();
				base = PluginRegistry.findModel(id);
			} else if (selectionElement instanceof IPluginObject) {
				base = (IPluginModelBase) ((IPluginObject) selectionElement).getModel();
			} else if (selectionElement instanceof BundleSpecification) {
				Resource desc = (BundleDescription) ((BundleSpecification) selectionElement).getSupplier();
				if (desc != null)
					base = PluginRegistry.findModel(desc);
			} else if (selectionElement instanceof Resource) {
				base = PluginRegistry.findModel((Resource) selectionElement);
			}
			if (base != null && base.getUnderlyingResource() != null) {
				fRefactorAction.setSelection(base);
				return true;
			}
		}
		return false;
	}

	public void setActive(boolean active) {
		if (active) {
			// update filter actions before updating filters because the filters depend on the state of the filter actions
			// update filter actions - both filter actions are specific to each Page instance
			if (fView.isShowingCallers()) {
				// deactive show optional on Callers view.
				fHideOptionalFilterAction.setChecked(true);
				fHideOptionalFilterAction.setEnabled(false);
			} else {
				fHideOptionalFilterAction.setEnabled(true);
				fHideOptionalFilterAction.setChecked(!getSettings().getBoolean(HIDE_OPTIONAL));
			}
			fHideFragmentFilterAction.setChecked(!getSettings().getBoolean(HIDE_FRAGMENTS));

			// update viewer's fragment filter
			boolean showFragments = fHideFragmentFilterAction.isChecked();
			boolean containsFragments = true;
			ViewerFilter[] filters = fViewer.getFilters();
			for (ViewerFilter filter : filters) {
				if (filter.equals(fHideFragmentFilter)) {
					containsFragments = false;
					break;
				}
			}
			if (showFragments != containsFragments)
				if (showFragments)
					fViewer.removeFilter(fHideFragmentFilter);
				else
					fViewer.addFilter(fHideFragmentFilter);

			// update viewer's optional filtering
			if (fHideOptionalFilterAction.isChecked() != isShowingOptional())
				handleShowOptional(fHideOptionalFilterAction.isChecked(), false);
		}

		if (fContentProvider instanceof DependenciesViewPageContentProvider) {
			if (active) {
				// when a page is activated, we need to have the content provider listen for changes and refresh the view to get current data
				((DependenciesViewPageContentProvider) fContentProvider).attachModelListener();
				fViewer.refresh();
			} else
				// when page is deactivated, we need to remove model listener from content manager.  Otherwise model changes will be sent to all
				// DependenciesViewPageContentProvider (including inactive ones).  This will cause problems with the content provider's logic!!
				((DependenciesViewPageContentProvider) fContentProvider).removeModelListener();
		}
	}

	private void contributeToActionBars(IActionBars actionBars) {
		IToolBarManager manager = actionBars.getToolBarManager();
		manager.add(fFocusOnAction);
		contributeToDropDownMenu(actionBars.getMenuManager());
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
		manager.add(fHideFragmentFilterAction);
		manager.add(fHideOptionalFilterAction);
		IDialogSettings settings = getSettings();
		boolean hideFragments = settings.getBoolean(HIDE_FRAGMENTS);
		boolean hideOptional = settings.getBoolean(HIDE_OPTIONAL);
		fHideFragmentFilterAction.setChecked(!hideFragments);
		fHideOptionalFilterAction.setChecked(!hideOptional);
		// The filtering will be executed in the setActive function when the viewer is displayed
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection("dependenciesView"); //$NON-NLS-1$
		if (section == null) {
			section = master.addNewSection("dependenciesView"); //$NON-NLS-1$
		}
		return section;
	}

}
