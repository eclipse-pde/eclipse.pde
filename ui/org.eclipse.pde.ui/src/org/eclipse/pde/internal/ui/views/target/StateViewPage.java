/*******************************************************************************
 * Copyright (c) 2009, 20168IBM Corporation and others.
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
 *     Anyware Technologies - ongoing enhancements
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.target;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferencePage;
import org.eclipse.pde.internal.ui.shared.target.CopyLocationAction;
import org.eclipse.pde.internal.ui.views.plugins.ImportActionGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBookView;

public class StateViewPage extends Page implements IStateDeltaListener, IPluginModelListener {

	private final IPropertyChangeListener fPropertyListener;
	private StateTree fFilteredTree;
	private TreeViewer fTreeViewer = null;
	private final PageBookView fView;
	private Composite fComposite;
	private Action fOpenAction;
	private final String DIALOG_SETTINGS = "targetStateView"; //$NON-NLS-1$

	private static final String HIDE_RESOLVED = "hideResolved"; //$NON-NLS-1$
	private static final String SHOW_NONLEAF = "hideNonLeaf"; //$NON-NLS-1$

	private final ViewerFilter fHideResolvedFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return ((element instanceof BundleDescription && !((BundleDescription) element).isResolved()) || parentElement instanceof BundleDescription && !((BundleDescription) parentElement).isResolved());
		}
	};

	private final ViewerFilter fShowLeaves = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof BundleDescription) {
				return ((BundleDescription) element).getDependents().length == 0;
			}
			return true;
		}
	};

	static class DependencyGroup {
		Object[] dependencies;

		public DependencyGroup(Object[] constraints) {
			dependencies = constraints;
		}

		public Object[] getChildren() {
			return dependencies;
		}

		@Override
		public String toString() {
			return (dependencies[0] instanceof BundleSpecification) ? PDEUIMessages.StateViewPage_requiredBundles : PDEUIMessages.StateViewPage_importedPackages;
		}
	}

	public StateViewPage(PageBookView view) {
		fView = view;
		fPropertyListener = event -> {
			String property = event.getProperty();
			if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
				fTreeViewer.refresh();
			}
		};
	}

	@Override
	public void createControl(Composite parent) {
		fComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		fComposite.setLayout(layout);
		fComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fFilteredTree = new StateTree(fComposite);

		fFilteredTree.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		fTreeViewer = fFilteredTree.getViewer();

		if (getSettings().getBoolean(HIDE_RESOLVED)) {
			fTreeViewer.addFilter(fHideResolvedFilter);
		}
		if (getSettings().getBoolean(SHOW_NONLEAF)) {
			fTreeViewer.addFilter(fShowLeaves);
		}

		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
		getSite().setSelectionProvider(fTreeViewer);
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
		setActive(true);
	}

	@Override
	public Control getControl() {
		return fComposite;
	}

	@Override
	public void setFocus() {
		if (fFilteredTree != null) {
			Control c = fFilteredTree.getFilterControl();
			if (c != null && !c.isFocusControl()) {
				c.setFocus();
			}
		}
	}



	protected void setActive(boolean active) {
		if (active) {
			State state = PDECore.getDefault().getModelManager().getState().getState();
			state.resolve(true);
			fFilteredTree.setInput(state);
			PDECore.getDefault().getModelManager().addPluginModelListener(this);
		} else {
			PDECore.getDefault().getModelManager().removePluginModelListener(this);
		}
	}

	@Override
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		Action filterResolved = new Action(PDEUIMessages.StateViewPage_showOnlyUnresolved_label, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				getSettings().put(HIDE_RESOLVED, isChecked());
				if (isChecked()) {
					fTreeViewer.addFilter(fHideResolvedFilter);
				} else {
					fTreeViewer.removeFilter(fHideResolvedFilter);
				}
			}
		};
		Action filterLeaves = new Action(PDEUIMessages.StateViewPage_showLeaves, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				getSettings().put(SHOW_NONLEAF, isChecked());
				if (isChecked()) {
					fTreeViewer.addFilter(fShowLeaves);
				} else {
					fTreeViewer.removeFilter(fShowLeaves);
				}
			}
		};

		filterResolved.setChecked(getSettings().getBoolean(HIDE_RESOLVED));
		filterLeaves.setChecked(getSettings().getBoolean(SHOW_NONLEAF));
		menuManager.add(filterResolved);
		menuManager.add(filterLeaves);
		menuManager.add(new Separator());

		Action openPreferences = new Action(PDEUIMessages.StateViewPage_ChangeTargetPlatform, PDEPluginImages.DESC_TARGET_DEFINITION) {
			@Override
			public void run() {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(PDEPlugin.getActiveWorkbenchShell(), TargetPlatformPreferencePage.PAGE_ID, null, null);
				dialog.open();
			}
		};
		menuManager.add(openPreferences);

		hookContextMenu();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(fTreeViewer.getControl());
		fTreeViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(fView.getSite().getId(), menuMgr, fTreeViewer);
	}

	private void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		BundleDescription desc = fFilteredTree.getBundleDescription();
		if (desc != null) {
			if (fOpenAction == null) {
				fOpenAction = new Action(PDEUIMessages.StateViewPage_openItem) {
					@Override
					public void run() {
						ManifestEditor.openPluginEditor(desc);
					}
				};
			}
			menu.add(fOpenAction);
			if (ImportActionGroup.canImport(selection)) {
				ImportActionGroup actionGroup = new ImportActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(menu);
				menu.add(new Separator());
			}
			menu.add(new CopyLocationAction(fTreeViewer));
			menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
	}

	@Override
	public void dispose() {
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
		setActive(false);
		super.dispose();
	}

	@Override
	public void stateResolved(final StateDelta delta) {
		if (!fView.getCurrentPage().equals(this) || fTreeViewer == null || fTreeViewer.getTree().isDisposed()) {
			// if this page is not active, then wait until we call refresh on next activation
			return;
		}
		fTreeViewer.getTree().getDisplay().asyncExec(() -> {
			if (delta == null) {
				fTreeViewer.refresh();
			} else {
				BundleDelta[] deltas = delta.getChanges();
				for (BundleDelta d : deltas) {
					int type = d.getType();
					if (type == BundleDelta.REMOVED || type == BundleDelta.RESOLVED || type == BundleDelta.ADDED || type == BundleDelta.UNRESOLVED) {
						fTreeViewer.refresh();
						break;
					}
				}
			}
		});
	}

	@Override
	public void stateChanged(final State newState) {
		if (!this.equals(fView.getCurrentPage()) || fTreeViewer == null || fTreeViewer.getTree().isDisposed()) {
			// if this page is not active, then wait until we call refresh on next activation
			return;
		}
		fTreeViewer.getTree().getDisplay().asyncExec(() -> fFilteredTree.setInput(newState));
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection(DIALOG_SETTINGS);
		if (section == null) {
			section = master.addNewSection(DIALOG_SETTINGS);
		}
		return section;
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		if (fTreeViewer == null || fTreeViewer.getTree().isDisposed()) {
			return;
		}
		if (delta.getAddedEntries().length > 0 || delta.getChangedEntries().length > 0 || delta.getRemovedEntries().length > 0) {
			fTreeViewer.getTree().getDisplay().asyncExec(() -> fTreeViewer.refresh());
		}
	}

}
