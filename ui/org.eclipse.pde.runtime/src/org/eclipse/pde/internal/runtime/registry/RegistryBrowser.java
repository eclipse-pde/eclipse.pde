/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.DisabledInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.IHelpContextIds;
import org.eclipse.pde.internal.runtime.MessageHelper;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
public class RegistryBrowser extends ViewPart {
	
	public static final String SHOW_RUNNING_PLUGINS = "RegistryView.showRunning.label"; //$NON-NLS-1$
	public static final String SHOW_ADVANCED_MODE = "RegistryView.showAdvancedMode.label"; //$NON-NLS-1$
	public static final String SHOW_EXTENSIONS_ONLY = "RegistryView.showExtensions.label"; //$NON-NLS-1$ 
	
	private RegistryBrowserListener fListener;
	private FilteredTree fFilteredTree;
	private TreeViewer fTreeViewer;
	private IMemento fMemento;
	private int fTotalItems = 0;
	
	// menus and action items
	private Action fRefreshAction;
	private Action fShowPluginsAction;
	private Action fCollapseAllAction;
	private Action fShowAdvancedOperationsAction;
	private Action fShowExtensionsOnlyAction;

	// advanced actions
	private Action fStartAction;
	private Action fStopAction;
	private Action fEnableAction;
	private Action fDisableAction;
	private Action fDiagnoseAction;
	
	private DrillDownAdapter fDrillDownAdapter;
	private ViewerFilter fActiveFilter = new ViewerFilter() {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof PluginObjectAdapter)
				element = ((PluginObjectAdapter)element).getObject();
			if (element instanceof IExtensionPoint)
				element = Platform.getBundle(((IExtensionPoint)element).getNamespaceIdentifier());
			else if (element instanceof IExtension)
				element = Platform.getBundle(((IExtension)element).getNamespaceIdentifier());
			if (element instanceof Bundle)
				return ((Bundle)element).getState() == Bundle.ACTIVE;
			return true;
		}
	};
	
	
	/*
	 * customized DrillDownAdapter which modifies enabled state of showing active/inactive
	 * plug-ins action - see Bug 58467
	 */
	class RegistryDrillDownAdapter extends DrillDownAdapter{
		public RegistryDrillDownAdapter(TreeViewer tree){
			super(tree);
		}

		public void goInto() {
			super.goInto();
			fShowPluginsAction.setEnabled(!canGoHome());
		}

		public void goBack() {
			super.goBack();
			fShowPluginsAction.setEnabled(!canGoHome());
		}

		public void goHome() {
			super.goHome();
			fShowPluginsAction.setEnabled(!canGoHome());
		}

		public void goInto(Object newInput) {
			super.goInto(newInput);
			fShowPluginsAction.setEnabled(!canGoHome());
		}
	}
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento == null)
			this.fMemento = XMLMemento.createWriteRoot("REGISTRYVIEW"); //$NON-NLS-1$
		else
			this.fMemento = memento;
		initializeMemento();

		fListener = new RegistryBrowserListener(this);
	}
	
	private void initializeMemento() {
		// show all bundles by default (i.e. not just activated ones)
		if (fMemento.getString(SHOW_RUNNING_PLUGINS) == null)
			fMemento.putString(SHOW_RUNNING_PLUGINS, "false"); //$NON-NLS-1$
		if (fMemento.getString(SHOW_EXTENSIONS_ONLY) == null)
			fMemento.putString(SHOW_EXTENSIONS_ONLY, "false"); //$NON-NLS-1$
		
		// default to not showing advanced options to users
		if (fMemento.getString(SHOW_ADVANCED_MODE) == null)
			fMemento.putString(SHOW_ADVANCED_MODE, "false"); //$NON-NLS-1$
	}
	
	public void dispose() {
		if (fListener != null) {
			Platform.getExtensionRegistry().removeRegistryChangeListener(fListener);
			PDERuntimePlugin.getDefault().getBundleContext().removeBundleListener(fListener);
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
		fillToolBar();
		
		PDERuntimePlugin.getDefault().getBundleContext().addBundleListener(fListener);
		Platform.getExtensionRegistry().addRegistryChangeListener(fListener);
	}
	private void createTreeViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));	
		
		fFilteredTree = new RegistryFilteredTree(composite, SWT.MULTI, new PatternFilter());
		fFilteredTree.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		Tree tree = fFilteredTree.getViewer().getTree();
		GridData gd = new GridData(GridData.FILL_BOTH);
		fFilteredTree.setLayoutData(gd);
		fTreeViewer = fFilteredTree.getViewer();
		fTreeViewer.setContentProvider(new RegistryBrowserContentProvider());
		fTreeViewer.setLabelProvider(new RegistryBrowserLabelProvider(fTreeViewer));
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof PluginObjectAdapter)
					e1 = ((PluginObjectAdapter)e1).getObject();
				if (e2 instanceof PluginObjectAdapter)
					e2 = ((PluginObjectAdapter)e2).getObject();
				if (e1 instanceof IBundleFolder && e2 instanceof IBundleFolder)
					return ((IBundleFolder)e1).getFolderId() - ((IBundleFolder)e2).getFolderId();
				if (e1 instanceof Bundle && e2 instanceof Bundle) {
					e1 = ((Bundle)e1).getSymbolicName();
					e2 = ((Bundle)e2).getSymbolicName();
				}
				return super.compare(viewer, e1, e2);
			}
		});
		if (fShowPluginsAction.isChecked())
			fTreeViewer.addFilter(fActiveFilter);
		
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
		
	private PluginObjectAdapter[] getBundles() {
		Bundle[] bundles = PDERuntimePlugin.getDefault().getBundleContext().getBundles();
		ArrayList list = new ArrayList();
		for (int i = 0; i < bundles.length; i++)
			if (bundles[i].getHeaders().get(Constants.FRAGMENT_HOST) == null)
				list.add(new PluginObjectAdapter(bundles[i]));
		return (PluginObjectAdapter[]) list.toArray(new PluginObjectAdapter[list.size()]);
	}
	
	private void fillToolBar(){
		fDrillDownAdapter = new RegistryDrillDownAdapter(fTreeViewer);
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mng = bars.getToolBarManager();
		fDrillDownAdapter.addNavigationActions(mng);
		mng.add(fRefreshAction);
		mng.add(new Separator());
		mng.add(fCollapseAllAction);
		IMenuManager mgr = bars.getMenuManager();
		mgr.add(new Separator());
		mgr.add(fShowPluginsAction);
		mgr.add(fShowExtensionsOnlyAction);
		mgr.add(fShowAdvancedOperationsAction);
		
	}
	public void fillContextMenu(IMenuManager manager) {
		manager.add(fRefreshAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		// check if we should enable advanced actions
		if(fShowAdvancedOperationsAction.isChecked() && isBundleSelected()) {
			// control bundle state actions
			if(selectedBundlesStopped())
				manager.add(fStartAction);
			if(selectedBundlesStarted())
				manager.add(fStopAction);
			
			if(getSelectedBundles().size() == 1)
				manager.add(fDiagnoseAction);
			
			// security related actions
			if(selectedBundlesDisabled())
				manager.add(fEnableAction);
			if(selectedBundlesEnabled())
				manager.add(fDisableAction);
		}
		
		manager.add(new Separator());
		manager.add(fShowPluginsAction);
		manager.add(fShowExtensionsOnlyAction);
		manager.add(fShowAdvancedOperationsAction);
	}
	
	public void saveState(IMemento memento) {
		if (memento == null || fMemento == null || fTreeViewer == null)
			return;
		fMemento.putString(SHOW_RUNNING_PLUGINS, Boolean.toString(fShowPluginsAction.isChecked()));
		fMemento.putString(SHOW_EXTENSIONS_ONLY, Boolean.toString(fShowExtensionsOnlyAction.isChecked()));
		fMemento.putBoolean(SHOW_ADVANCED_MODE, fShowAdvancedOperationsAction.isChecked());
		memento.putMemento(fMemento);
	}
	
	public void setFocus() {
		fFilteredTree.getFilterControl().setFocus();
	}
	
	/*
	 * toolbar and context menu actions
	 */
	public void makeActions() {
		fRefreshAction = new Action("refresh") { //$NON-NLS-1$
			public void run() {
				BusyIndicator.showWhile(fTreeViewer.getTree().getDisplay(),	new Runnable() {
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
		
		fShowPluginsAction = new Action(PDERuntimeMessages.RegistryView_showRunning_label){
			public void run() {
				if (fShowPluginsAction.isChecked())
					fTreeViewer.addFilter(fActiveFilter);
				else
					fTreeViewer.removeFilter(fActiveFilter);
				updateTitle();
			}
		};
		fShowPluginsAction.setChecked(fMemento.getString(SHOW_RUNNING_PLUGINS).equals("true")); //$NON-NLS-1$
		
		fShowExtensionsOnlyAction = new Action(PDERuntimeMessages.RegistryBrowser_showExtOnlyLabel) {
			public void run() {
				// refreshAction takes into account checked state of fShowExtensionsOnlyAction
				// (via updateItems(true)
				fRefreshAction.run();
			}
		};
		fShowExtensionsOnlyAction.setChecked(fMemento.getString(SHOW_EXTENSIONS_ONLY).equals("true")); //$NON-NLS-1$
		
		fShowAdvancedOperationsAction = new Action(PDERuntimeMessages.RegistryView_showAdvanced_label) {
			public void run() {}
		};
		fShowAdvancedOperationsAction.setChecked(fMemento.getString(SHOW_ADVANCED_MODE).equals("true")); //$NON-NLS-1$
		
		fStartAction = new Action(PDERuntimeMessages.RegistryView_startAction_label) {
			public void run() {
				try {
					List bundles = getSelectedBundles();
					for(Iterator it = bundles.iterator(); it.hasNext();) {
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
					for(Iterator it = bundles.iterator(); it.hasNext();) {
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
				State state = PDERuntimePlugin.getDefault().getState();
				for(Iterator it = bundles.iterator(); it.hasNext();) {
					Bundle bundle = (Bundle) it.next();
					BundleDescription desc = state.getBundle(bundle.getBundleId());
					DisabledInfo[] infos = state.getDisabledInfos(desc);
					for (int i = 0; i < infos.length; i++) {
						PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
						platformAdmin.removeDisabledInfo(infos[i]);
					}
				}
			}
		};
		
		fDisableAction = new Action(PDERuntimeMessages.RegistryView_disableAction_label) {
			public void run() {
				List bundles = getSelectedBundles();
				State state = PDERuntimePlugin.getDefault().getState();
				for(Iterator it = bundles.iterator(); it.hasNext();) {
					Bundle bundle = (Bundle) it.next();
					BundleDescription desc = state.getBundle(bundle.getBundleId());
					DisabledInfo info = new DisabledInfo("org.eclipse.pde.ui", "Disabled via PDE", desc); //$NON-NLS-1$ //$NON-NLS-2$
					PlatformAdmin platformAdmin = 
						PDERuntimePlugin.getDefault().getPlatformAdmin();
					platformAdmin.addDisabledInfo(info);
				}
			}
		};
		
		fDiagnoseAction = new Action(PDERuntimeMessages.RegistryView_diagnoseAction_label) {
			public void run() {
				List bundles = getSelectedBundles();
				State state = PDERuntimePlugin.getDefault().getState();
				for (Iterator it = bundles.iterator(); it.hasNext();) {
					Bundle bundle = (Bundle) it.next();
					BundleDescription desc = state.getBundle(bundle.getBundleId());
					PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
					VersionConstraint[] unsatisfied = platformAdmin
							.getStateHelper().getUnsatisfiedConstraints(desc);
					ResolverError[] resolverErrors = platformAdmin.getState(false).getResolverErrors(desc);
					MultiStatus problems = new MultiStatus(PDERuntimePlugin.ID,
							IStatus.INFO,
							PDERuntimeMessages.RegistryView_found_problems,
							null);
					for (int i = 0; i < resolverErrors.length; i++) {
						if ((resolverErrors[i].getType() & (ResolverError.MISSING_FRAGMENT_HOST
								| ResolverError.MISSING_GENERIC_CAPABILITY
								| ResolverError.MISSING_IMPORT_PACKAGE | ResolverError.MISSING_REQUIRE_BUNDLE)) != 0)
							continue;
						IStatus status = new Status(IStatus.WARNING,
								PDERuntimePlugin.ID, resolverErrors[i]
										.toString());
						problems.add(status);
					}

					for (int i = 0; i < unsatisfied.length; i++) {
						IStatus status = new Status(
								IStatus.WARNING,
								PDERuntimePlugin.ID,
								MessageHelper
										.getResolutionFailureMessage(unsatisfied[i]));
						problems.add(status);
					}
					Dialog dialog;
					if (unsatisfied.length != 0 || resolverErrors.length != 0) {
						dialog = new DiagnosticsDialog(
								getSite().getShell(),
								PDERuntimeMessages.RegistryView_diag_dialog_title,
								null, problems, IStatus.WARNING);
						dialog.open();
					} else {
						MessageDialog
								.openInformation(
										getSite().getShell(),
										PDERuntimeMessages.RegistryView_diag_dialog_title,
										PDERuntimeMessages.RegistryView_no_unresolved_constraints);
					}

				}
			}
		};
		
		fCollapseAllAction = new Action("collapseAll"){ //$NON-NLS-1$
			public void run(){
				fTreeViewer.collapseAll();
			}
		};
		fCollapseAllAction.setText(PDERuntimeMessages.RegistryView_collapseAll_label);
		fCollapseAllAction.setImageDescriptor(PDERuntimePluginImages.DESC_COLLAPSE_ALL);
		fCollapseAllAction.setToolTipText(PDERuntimeMessages.RegistryView_collapseAll_tooltip);
	}
	
	protected void updateItems(boolean resetInput) {
		Object[] input = null;
		boolean extOnly = fShowExtensionsOnlyAction.isChecked();
		if (extOnly)
			input = Platform.getExtensionRegistry().getExtensionPoints();
		else
			input = getBundles();
		fListener.fExtOnly = extOnly;
		fTotalItems = input.length;
		if (resetInput)
			fTreeViewer.setInput(new PluginObjectAdapter(input));
		updateTitle();
	}
	
	private void updateTitle(){
		setContentDescription(getTitleSummary());
	}
	
	protected Tree getUndisposedTree() {
		if (fTreeViewer == null || fTreeViewer.getTree() == null || fTreeViewer.getTree().isDisposed())
			return null;
		return fTreeViewer.getTree();
	}
	
	public String getTitleSummary(){
		Tree tree = getUndisposedTree();
		String type = fShowExtensionsOnlyAction.isChecked() ? PDERuntimeMessages.RegistryView_folders_extensionPoints : PDERuntimeMessages.RegistryBrowser_plugins;
		if (tree == null)
			return NLS.bind(PDERuntimeMessages.RegistryView_titleSummary, (new String[] {"0", "0", type})); //$NON-NLS-1$ //$NON-NLS-2$
		return NLS.bind(PDERuntimeMessages.RegistryView_titleSummary, (new String[] {
				Integer.toString(tree.getItemCount()), 
				Integer.toString(fTotalItems),
				type}));
	}
	
	// TODO hackish, should rewrite
	private boolean isBundleSelected() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if(selection != null) {			
			Object[] elements = selection.toArray();
			for(int i = 0; i < elements.length; i++) {
				if(elements[i] instanceof PluginObjectAdapter) {
					PluginObjectAdapter adapter = (PluginObjectAdapter) elements[i];
					Object object = adapter.getObject();
					if(!(object instanceof Bundle))
						return false;
				} else {
					return false;
				}
			}
		}
		return true;
	}
	
	private List getSelectedBundles() {
		List bundles = new ArrayList();
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if(selection != null) {
			Object[] elements = selection.toArray();
			for(int i = 0; i < elements.length; i++) {
				if(elements[i] instanceof PluginObjectAdapter) {
					PluginObjectAdapter adapter = (PluginObjectAdapter) elements[i];
					Object object = adapter.getObject();
					if(object instanceof Bundle)
						bundles.add(object);
				}
			}
		}
		return bundles;
	}
	
	private boolean selectedBundlesStarted() {
		List bundles = getSelectedBundles();
		for(Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			if(bundle.getState() != Bundle.ACTIVE)
				return false;
		}
		return true;
	}
	
	private boolean selectedBundlesStopped() {
		List bundles = getSelectedBundles();
		for(Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			if(bundle.getState() == Bundle.ACTIVE)
				return false;
		}
		return true;
	}
	
	private boolean selectedBundlesDisabled() {
		List bundles = getSelectedBundles();
		for(Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			State state = 
				PDERuntimePlugin.getDefault().getState();
			BundleDescription desc = 
				state.getBundle(bundle.getBundleId());
			DisabledInfo[] infos = state.getDisabledInfos(desc);
			if(infos.length == 0)
				return false;
		}
		return true;
	}
	
	private boolean selectedBundlesEnabled() {
		List bundles = getSelectedBundles();
		for(Iterator it = bundles.iterator(); it.hasNext();) {
			Bundle bundle = (Bundle) it.next();
			State state = 
				PDERuntimePlugin.getDefault().getState();
			BundleDescription desc = 
				state.getBundle(bundle.getBundleId());
			DisabledInfo[] infos = state.getDisabledInfos(desc);
			if(infos.length > 0)
				return false;
		}
		return true;
	}
	
	protected void add(Object object) {
		add(fTreeViewer.getInput(), object);
	}
	
	protected void add(Object parent, Object object) {
		if (fDrillDownAdapter.canGoHome())
			return;
		fTotalItems += 1;
		fTreeViewer.add(parent, object);
		updateTitle();
	}
	
	protected void remove(Object object) {
		if (fDrillDownAdapter.canGoHome())
			return;
		fTotalItems -= 1;
		fTreeViewer.remove(object);
		updateTitle();
	}
	
	protected void update(Object object) {
		fTreeViewer.update(object, null);
	}
	
	protected void refresh(Object object) {
		fTreeViewer.refresh(object);
	}
	
	protected TreeItem[] getTreeItems() {
		return fTreeViewer.getTree().getItems();
	}
}
