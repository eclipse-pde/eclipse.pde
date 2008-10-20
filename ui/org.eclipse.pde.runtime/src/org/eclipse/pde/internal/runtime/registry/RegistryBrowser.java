/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 211127
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.pde.internal.runtime.registry.model.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleException;

public class RegistryBrowser extends ViewPart {

	public static final String SHOW_RUNNING_PLUGINS = "RegistryView.showRunning.label"; //$NON-NLS-1$
	public static final String SHOW_ADVANCED_MODE = "RegistryView.showAdvancedMode.label"; //$NON-NLS-1$
	public static final String SHOW_EXTENSIONS_ONLY = "RegistryView.showExtensions.label"; //$NON-NLS-1$ 
	public static final String SHOW_DISABLED_MODE = "RegistryView.showDisabledMode.label"; //$NON-NLS-1$

	private FilteredTree fFilteredTree;
	private TreeViewer fTreeViewer;
	private IMemento fMemento;
	private int fTotalItems = 0;

	private RegistryModel model;
	private ModelChangeListener listener;

	private RegistryBrowserContentProvider fContentProvider;

	// menus and action items
	private Action fRefreshAction;
	private Action fShowPluginsAction;
	private Action fCollapseAllAction;
	private Action fShowAdvancedOperationsAction;
	private Action fShowExtensionsOnlyAction;
	private Action fShowDisabledAction;
	private Action fCopyAction;

	// advanced actions
	private Action fStartAction;
	private Action fStopAction;
	private Action fEnableAction;
	private Action fDisableAction;
	private Action fDiagnoseAction;

	private Clipboard fClipboard;

	private DrillDownAdapter fDrillDownAdapter;
	private ViewerFilter fActiveFilter = new ViewerFilter() {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof ExtensionPoint)
				element = Platform.getBundle(((ExtensionPoint) element).getNamespaceIdentifier());
			else if (element instanceof Extension)
				element = Platform.getBundle(((Extension) element).getNamespaceIdentifier());
			if (element instanceof Bundle)
				return ((Bundle) element).getState() == Bundle.ACTIVE;
			return true;
		}
	};

	private ViewerFilter fDisabledFilter = new ViewerFilter() {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof Bundle) {
				return !((Bundle) element).isEnabled();
			}
			return false;
		}
	};

	/*
	 * customized DrillDownAdapter which modifies enabled state of showing active/inactive
	 * plug-ins action - see Bug 58467
	 */
	class RegistryDrillDownAdapter extends DrillDownAdapter {
		public RegistryDrillDownAdapter(TreeViewer tree) {
			super(tree);
		}

		public void goInto() {
			super.goInto();
			fShowPluginsAction.setEnabled(!canGoHome());
			fShowDisabledAction.setEnabled(!canGoHome());
		}

		public void goBack() {
			super.goBack();
			fShowPluginsAction.setEnabled(!canGoHome());
			fShowDisabledAction.setEnabled(!canGoHome());
		}

		public void goHome() {
			super.goHome();
			fShowPluginsAction.setEnabled(!canGoHome());
			fShowDisabledAction.setEnabled(!canGoHome());
		}

		public void goInto(Object newInput) {
			super.goInto(newInput);
			fShowPluginsAction.setEnabled(!canGoHome());
			fShowDisabledAction.setEnabled(!canGoHome());
		}
	}

	public RegistryBrowser() {
		try {
			model = RegistryModelFactory.getRegistryModel(new URI("local"));
		} catch (URISyntaxException e) {
			PDERuntimePlugin.log(e);
		}
		model.connect();

		listener = new RegistryBrowserModelChangeListener(this);
		model.addModelChangeListener(listener);
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento == null)
			this.fMemento = XMLMemento.createWriteRoot("REGISTRYVIEW"); //$NON-NLS-1$
		else
			this.fMemento = memento;
		initializeMemento();
	}

	private void initializeMemento() {
		// show all bundles by default (i.e. not just activated ones)
		if (fMemento.getString(SHOW_RUNNING_PLUGINS) == null)
			fMemento.putString(SHOW_RUNNING_PLUGINS, "false"); //$NON-NLS-1$
		if (fMemento.getString(SHOW_EXTENSIONS_ONLY) == null)
			fMemento.putString(SHOW_EXTENSIONS_ONLY, "false"); //$NON-NLS-1$
		if (fMemento.getString(SHOW_DISABLED_MODE) == null)
			fMemento.putString(SHOW_DISABLED_MODE, "false"); //$NON-NLS-1$

		// default to not showing advanced options to users
		if (fMemento.getString(SHOW_ADVANCED_MODE) == null)
			fMemento.putString(SHOW_ADVANCED_MODE, "false"); //$NON-NLS-1$
	}

	public void dispose() {
		model.disconnect();
		model.removeModelChangeListener(listener);
		if (fClipboard != null) {
			fClipboard.dispose();
		}
		super.dispose();
	}

	public void createPartControl(Composite parent) {
		// create the sash form that will contain the tree viewer & text viewer
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		makeActions();
		createTreeViewer(composite);
		fClipboard = new Clipboard(fTreeViewer.getTree().getDisplay());
		fillToolBar();
	}

	private void createTreeViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fFilteredTree = new RegistryFilteredTree(this, composite, SWT.MULTI, new PatternFilter());
		fFilteredTree.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		Tree tree = fFilteredTree.getViewer().getTree();
		GridData gd = new GridData(GridData.FILL_BOTH);
		fFilteredTree.setLayoutData(gd);
		fTreeViewer = fFilteredTree.getViewer();
		fContentProvider = new RegistryBrowserContentProvider(this);
		fTreeViewer.setContentProvider(fContentProvider);
		fTreeViewer.setLabelProvider(new RegistryBrowserLabelProvider(fTreeViewer));
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof Folder && e2 instanceof Folder)
					return ((Folder) e1).getId() - ((Folder) e2).getId();
				if (e1 instanceof Bundle && e2 instanceof Bundle) {
					e1 = ((Bundle) e1).getSymbolicName();
					e2 = ((Bundle) e2).getSymbolicName();
				}
				return super.compare(viewer, e1, e2);
			}
		});
		if (fShowPluginsAction.isChecked())
			fTreeViewer.addFilter(fActiveFilter);

		if (fShowDisabledAction.isChecked())
			fTreeViewer.addFilter(fDisabledFilter);

		updateItems(true);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(fTreeViewer.getControl(), IHelpContextIds.REGISTRY_VIEW);

		getViewSite().setSelectionProvider(fTreeViewer);

		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.setRemoveAllWhenShown(true);
		popupMenuManager.addMenuListener(listener);
		Menu menu = popupMenuManager.createContextMenu(tree);
		tree.setMenu(menu);
	}

	private void fillToolBar() {
		fDrillDownAdapter = new RegistryDrillDownAdapter(fTreeViewer);
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		IToolBarManager mng = bars.getToolBarManager();
		fDrillDownAdapter.addNavigationActions(mng);
		mng.add(fRefreshAction);
		mng.add(new Separator());
		mng.add(fCollapseAllAction);
		IMenuManager mgr = bars.getMenuManager();
		mgr.add(new Separator());
		mgr.add(fShowPluginsAction);
		mgr.add(fShowDisabledAction);
		mgr.add(new Separator());
		mgr.add(fShowExtensionsOnlyAction);
		mgr.add(new Separator());
		mgr.add(fShowAdvancedOperationsAction);

	}

	public void fillContextMenu(IMenuManager manager) {
		manager.add(fRefreshAction);
		manager.add(new Separator());
		manager.add(fCopyAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		// check if we should enable advanced actions
		if (fShowAdvancedOperationsAction.isChecked() && isBundleSelected()) {
			// control bundle state actions
			if (selectedBundlesStopped())
				manager.add(fStartAction);
			if (selectedBundlesStarted())
				manager.add(fStopAction);

			if (getSelectedBundles().size() == 1)
				manager.add(fDiagnoseAction);

			// security related actions
			if (selectedBundlesDisabled())
				manager.add(fEnableAction);
			if (selectedBundlesEnabled())
				manager.add(fDisableAction);
		}

		manager.add(new Separator());
		manager.add(fShowPluginsAction);
		manager.add(fShowDisabledAction);
		manager.add(new Separator());
		manager.add(fShowExtensionsOnlyAction);
		manager.add(new Separator());
		manager.add(fShowAdvancedOperationsAction);
	}

	public void saveState(IMemento memento) {
		if (memento == null || fMemento == null || fTreeViewer == null)
			return;
		fMemento.putString(SHOW_RUNNING_PLUGINS, Boolean.toString(fShowPluginsAction.isChecked()));
		fMemento.putString(SHOW_DISABLED_MODE, Boolean.toString(fShowDisabledAction.isChecked()));
		fMemento.putString(SHOW_EXTENSIONS_ONLY, Boolean.toString(fShowExtensionsOnlyAction.isChecked()));
		fMemento.putBoolean(SHOW_ADVANCED_MODE, fShowAdvancedOperationsAction.isChecked());
		memento.putMemento(fMemento);
	}

	public void setFocus() {
		Text filterText = fFilteredTree.getFilterControl();
		if (filterText != null) {
			filterText.setFocus();
		}
	}

	/*
	 * toolbar and context menu actions
	 */
	public void makeActions() {
		fRefreshAction = new Action("refresh") { //$NON-NLS-1$
			public void run() {
				BusyIndicator.showWhile(fTreeViewer.getTree().getDisplay(), new Runnable() {
					public void run() {
						updateItems(true);
					}
				});
			}
		};
		fRefreshAction.setText(PDERuntimeMessages.RegistryView_refresh_label);
		fRefreshAction.setToolTipText(PDERuntimeMessages.RegistryView_refresh_tooltip);
		fRefreshAction.setImageDescriptor(PDERuntimePluginImages.DESC_REFRESH);
		fRefreshAction.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_DISABLED);

		fShowPluginsAction = new Action(PDERuntimeMessages.RegistryView_showRunning_label) {
			public void run() {
				if (fShowPluginsAction.isChecked()) {
					fTreeViewer.addFilter(fActiveFilter);
				} else {
					fTreeViewer.removeFilter(fActiveFilter);
				}
				updateTitle();
			}
		};
		fShowPluginsAction.setChecked(fMemento.getString(SHOW_RUNNING_PLUGINS).equals("true")); //$NON-NLS-1$

		fShowDisabledAction = new Action(PDERuntimeMessages.RegistryView_showDisabled_label) {
			public void run() {
				if (fShowDisabledAction.isChecked()) {
					fTreeViewer.addFilter(fDisabledFilter);
				} else {
					fTreeViewer.removeFilter(fDisabledFilter);
				}
				updateTitle();
			}
		};
		fShowDisabledAction.setChecked(fMemento.getString(SHOW_DISABLED_MODE).equals("true")); //$NON-NLS-1$

		fCopyAction = new Action(PDERuntimeMessages.RegistryBrowser_copy_label) {
			public void run() {
				ITreeSelection selection = (ITreeSelection) fFilteredTree.getViewer().getSelection();
				if (selection.isEmpty()) {
					return;
				}

				String textVersion = ((ILabelProvider) fTreeViewer.getLabelProvider()).getText(selection.getFirstElement());
				if ((textVersion != null) && (textVersion.trim().length() > 0)) {
					// set the clipboard contents
					fClipboard.setContents(new Object[] {textVersion}, new Transfer[] {TextTransfer.getInstance()});
				}
			}
		};
		fCopyAction.setImageDescriptor(PDERuntimePluginImages.COPY_QNAME);

		fShowExtensionsOnlyAction = new Action(PDERuntimeMessages.RegistryBrowser_showExtOnlyLabel) {
			public void run() {
				// refreshAction takes into account checked state of fShowExtensionsOnlyAction
				// (via updateItems(true)
				fRefreshAction.run();
			}
		};
		fShowExtensionsOnlyAction.setChecked(fMemento.getString(SHOW_EXTENSIONS_ONLY).equals("true")); //$NON-NLS-1$

		fShowAdvancedOperationsAction = new Action(PDERuntimeMessages.RegistryView_showAdvanced_label) {
			public void run() { // do nothing
			}
		};
		fShowAdvancedOperationsAction.setChecked(fMemento.getString(SHOW_ADVANCED_MODE).equals("true")); //$NON-NLS-1$

		fStartAction = new Action(PDERuntimeMessages.RegistryView_startAction_label) {
			public void run() {
				try {
					List bundles = getSelectedBundles();
					for (Iterator it = bundles.iterator(); it.hasNext();) {
						Bundle bundle = (Bundle) it.next();
						bundle.start();
					}
				} catch (BundleException e) {
					PDERuntimePlugin.log(e);
				}
			}
		};

		fStopAction = new Action(PDERuntimeMessages.RegistryView_stopAction_label) {
			public void run() {
				try {
					List bundles = getSelectedBundles();
					for (Iterator it = bundles.iterator(); it.hasNext();) {
						Bundle bundle = (Bundle) it.next();
						bundle.stop();
					}
				} catch (BundleException e) {
					PDERuntimePlugin.log(e);
				}
			}
		};

		fEnableAction = new Action(PDERuntimeMessages.RegistryView_enableAction_label) {
			public void run() {
				List bundles = getSelectedBundles();
				for (Iterator it = bundles.iterator(); it.hasNext();) {
					Bundle bundle = (Bundle) it.next();
					bundle.setEnabled(true);
				}
			}
		};

		fDisableAction = new Action(PDERuntimeMessages.RegistryView_disableAction_label) {
			public void run() {
				List bundles = getSelectedBundles();
				for (Iterator it = bundles.iterator(); it.hasNext();) {
					Bundle bundle = (Bundle) it.next();
					bundle.setEnabled(false);
				}
			}
		};

		fDiagnoseAction = new Action(PDERuntimeMessages.RegistryView_diagnoseAction_label) {
			public void run() {
				List bundles = getSelectedBundles();
				for (Iterator it = bundles.iterator(); it.hasNext();) {
					Bundle bundle = (Bundle) it.next();
					MultiStatus problems = bundle.diagnose();

					Dialog dialog;
					if (problems.getChildren().length > 0) {
						dialog = new DiagnosticsDialog(getSite().getShell(), PDERuntimeMessages.RegistryView_diag_dialog_title, null, problems, IStatus.WARNING);
						dialog.open();
					} else {
						MessageDialog.openInformation(getSite().getShell(), PDERuntimeMessages.RegistryView_diag_dialog_title, PDERuntimeMessages.RegistryView_no_unresolved_constraints);
					}

				}
			}
		};

		fCollapseAllAction = new Action("collapseAll") { //$NON-NLS-1$
			public void run() {
				fTreeViewer.collapseAll();
			}
		};
		fCollapseAllAction.setText(PDERuntimeMessages.RegistryView_collapseAll_label);
		fCollapseAllAction.setImageDescriptor(PDERuntimePluginImages.DESC_COLLAPSE_ALL);
		fCollapseAllAction.setToolTipText(PDERuntimeMessages.RegistryView_collapseAll_tooltip);
	}

	public boolean showExtensionsOnly() {
		return fShowExtensionsOnlyAction.isChecked();
	}

	protected void updateItems(boolean resetInput) {
		Object[] input = null;
		if (showExtensionsOnly())
			input = model.getExtensionPoints();
		else
			input = model.getBundles();
		fTotalItems = input.length;
		if (resetInput)
			fTreeViewer.setInput(input);
		updateTitle();
	}

	public void updateTitle() {
		setContentDescription(getTitleSummary());
	}

	protected Tree getUndisposedTree() {
		if (fTreeViewer == null || fTreeViewer.getTree() == null || fTreeViewer.getTree().isDisposed())
			return null;
		return fTreeViewer.getTree();
	}

	public String getTitleSummary() {
		Tree tree = getUndisposedTree();
		String type = fShowExtensionsOnlyAction.isChecked() ? PDERuntimeMessages.RegistryView_folders_extensionPoints : PDERuntimeMessages.RegistryBrowser_plugins;
		if (tree == null)
			return NLS.bind(PDERuntimeMessages.RegistryView_titleSummary, (new String[] {"0", "0", type})); //$NON-NLS-1$ //$NON-NLS-2$
		return NLS.bind(PDERuntimeMessages.RegistryView_titleSummary, (new String[] {Integer.toString(tree.getItemCount()), Integer.toString(fTotalItems), type}));
	}

	private boolean isBundleSelected() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (selection != null) {
			Object[] elements = selection.toArray();
			for (int i = 0; i < elements.length; i++) {
				if (!(elements[i] instanceof Bundle)) {
					return false;
				}
			}
		}

		return true;
	}

	private List getSelectedBundles() {
		List bundles = new ArrayList();
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (selection != null) {
			Object[] elements = selection.toArray();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof Bundle) {
					bundles.add(elements[i]);
				}
			}
		}
		return bundles;
	}

	/**
	 * @return true if none is stopped, false if at least one is stopped
	 */
	private boolean selectedBundlesStarted() {
		List bundles = getSelectedBundles();
		for (Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			if (bundle.getState() != Bundle.ACTIVE)
				return false;
		}
		return true;
	}

	/**
	 * @return true if none is active, false if at least one is active
	 */
	private boolean selectedBundlesStopped() {
		List bundles = getSelectedBundles();
		for (Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			if (bundle.getState() == Bundle.ACTIVE)
				return false;
		}
		return true;
	}

	/**
	 * @return true if none is enabled, false if at least one is enabled
	 */
	private boolean selectedBundlesDisabled() {
		List bundles = getSelectedBundles();
		for (Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			if (bundle.isEnabled())
				return false;
		}
		return true;
	}

	/**
	 * @return true if none is disabled, false if at least one is disabled
	 */
	private boolean selectedBundlesEnabled() {
		List bundles = getSelectedBundles();
		for (Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			if (!bundle.isEnabled())
				return false;
		}
		return true;
	}

	public void add(Object object) {
		Object parent = fContentProvider.getParent(object);
		if (parent == null) {
			add(fTreeViewer.getInput(), object);
		} else {
			refresh(parent);
		}
	}

	protected void add(Object parent, Object object) {
		if (fDrillDownAdapter.canGoHome())
			return;
		fTotalItems += 1;
		fTreeViewer.add(parent, object);
		updateTitle();
	}

	public void remove(Object object) {
		if (fDrillDownAdapter.canGoHome())
			return;
		fTotalItems -= 1;
		fTreeViewer.remove(object);
		updateTitle();
	}

	public void update(Object object) {
		fTreeViewer.update(object, null);
	}

	public void refresh(Object object) {
		fTreeViewer.refresh(object);
	}
}
