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
package org.eclipse.pde.internal.runtime.registry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.DisabledInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.internal.runtime.IHelpContextIds;
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
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;

public class RegistryBrowser extends ViewPart implements BundleListener, IRegistryChangeListener {
	
	public static final String SHOW_RUNNING_PLUGINS = "RegistryView.showRunning.label"; //$NON-NLS-1$
	public static final String SHOW_ADVANCED_MODE = "RegistryView.showAdvancedMode.label"; //$NON-NLS-1$
	
	private FilteredTree fFilteredTree;
	private TreeViewer fTreeViewer;
	private IMemento fMemento;
	private int fTotalBundles = 0;
	
	// menus and action items
	private Action fRefreshAction;
	private Action fShowPluginsAction;
	private Action fShowAdvancedOperationsAction;
	private Action fCollapseAllAction;
	
	// advanced actions
	private Action fStartAction;
	private Action fStopAction;
	private Action fEnableAction;
	private Action fDisableAction;
	
	private DrillDownAdapter drillDownAdapter;
	private ViewerFilter fActiveFilter = new ViewerFilter() {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof PluginObjectAdapter)
				element = ((PluginObjectAdapter)element).getObject();
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
	public RegistryBrowser() {
		super();
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
		// show all plug-ins by default (i.e. not just activated ones)
		if (fMemento.getString(SHOW_RUNNING_PLUGINS) == null)
			fMemento.putString(SHOW_RUNNING_PLUGINS, "false"); //$NON-NLS-1$
		
		// default to not showing advanced options to users
		if (fMemento.getString(SHOW_ADVANCED_MODE) == null)
			fMemento.putString(SHOW_ADVANCED_MODE, "false"); //$NON-NLS-1$
	}
	
	public void dispose() {
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().removeBundleListener(this);
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
		updateTitle();
		
		Platform.getExtensionRegistry().addRegistryChangeListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().addBundleListener(this);
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
		fTreeViewer.setContentProvider(new RegistryBrowserContentProvider(fTreeViewer));
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
		if (fMemento.getString(SHOW_RUNNING_PLUGINS).equals("true")) //$NON-NLS-1$)
			fTreeViewer.addFilter(fActiveFilter);
		
		PluginObjectAdapter[] adapters = getBundles();
		fTotalBundles = adapters.length;
		fTreeViewer.setInput(new PluginObjectAdapter(adapters));
		
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
		drillDownAdapter = new RegistryDrillDownAdapter(fTreeViewer);
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager mng = bars.getToolBarManager();
		drillDownAdapter.addNavigationActions(mng);
		mng.add(fRefreshAction);
		mng.add(new Separator());
		mng.add(fCollapseAllAction);
		IMenuManager mgr = bars.getMenuManager();
		mgr.add(new Separator());
		mgr.add(fShowPluginsAction);
		mgr.add(fShowAdvancedOperationsAction);
		
	}
	public void fillContextMenu(IMenuManager manager) {
		manager.add(fRefreshAction);
		Tree tree = getUndisposedTree();
		// TODO remove testing actions from code after delta is fixed
		// bug 130655
		if (tree != null && false) {
			TreeItem[] selection = tree.getSelection();
			boolean allRemoveable = true;
			boolean canAdd = true;
			for (int i = 0; i < selection.length; i++) {
				Object obj = selection[i].getData();
				if (obj instanceof PluginObjectAdapter)
					obj = ((PluginObjectAdapter)obj).getObject();
				if (!(obj instanceof Bundle) || ((Bundle)obj).getState() < Bundle.RESOLVED)
					canAdd = false;
				if (!(obj instanceof IExtensionPoint) && !(obj instanceof IExtension)) {
					allRemoveable = false;
					break;
				}
			}
		}
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		
		// check if we should enable advanced actions
		if(fShowAdvancedOperationsAction.isChecked() && isBundleSelected()) {
			// control bundle state actions
			if(selectedBundlesStopped())
				manager.add(fStartAction);
			if(selectedBundlesStarted())
				manager.add(fStopAction);
			
			// security related actions
			if(selectedBundlesDisabled())
				manager.add(fEnableAction);
			if(selectedBundlesEnabled())
				manager.add(fDisableAction);
		}
		
		manager.add(new Separator());
		manager.add(fShowPluginsAction);
		manager.add(fShowAdvancedOperationsAction);
	}
	public TreeViewer getTreeViewer() {
		return fTreeViewer;
	}
	
	public void saveState(IMemento memento) {
		if (memento == null || this.fMemento == null || fTreeViewer == null)
			return;
		fMemento.putBoolean(SHOW_RUNNING_PLUGINS, fShowPluginsAction.isChecked());
		fMemento.putBoolean(SHOW_ADVANCED_MODE, fShowAdvancedOperationsAction.isChecked());
		memento.putMemento(fMemento);
	}
	
	public void setFocus() {
		fFilteredTree.getFilterControl().setFocus();
	}
	
	/*
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(final BundleEvent event) {
		final Tree tree = getUndisposedTree();
		if (tree == null)
			return;
		
		final Bundle bundle = event.getBundle();
		tree.getDisplay().asyncExec(new Runnable() {
			public void run() {
				Object data = null;
				switch (event.getType()) {
				case BundleEvent.INSTALLED:
					data = findTreeBundleData(bundle);
					if (data == null) { // bundle doesn't exist in tree - add it
						fTreeViewer.add(fTreeViewer.getInput(), new PluginObjectAdapter(bundle));
						fTotalBundles += 1;
						updateTitle();
					}
					break;
				case BundleEvent.LAZY_ACTIVATION:
					break;
				case BundleEvent.RESOLVED:
					break;
				case BundleEvent.STARTED:
					// TODO - look over this (installing new bundles during runtime results in bad "location")
					// removing and adding the tree item to update it with a platform bundle object
					data = findTreeBundleData(bundle);
					final Bundle platformBundle = Platform.getBundle(bundle.getSymbolicName());
					if (data != null && platformBundle != null) {
						fTreeViewer.remove(data);
						fTreeViewer.add(fTreeViewer.getInput(), new PluginObjectAdapter(platformBundle));
					}
					break;
				case BundleEvent.STARTING:
					break;
				case BundleEvent.STOPPED:
					data = findTreeBundleData(bundle);
					if (data != null)
						fTreeViewer.update(data, null);
					break;
				case BundleEvent.STOPPING:
					break;
				case BundleEvent.UNINSTALLED:
					data = findTreeBundleData(bundle);
					if (data != null) {
						fTreeViewer.remove(data);
						fTotalBundles -= 1;
						updateTitle();
					}
					break;
				case BundleEvent.UNRESOLVED:
					break;
				case BundleEvent.UPDATED:
					break;
				}
			}
			private Object findTreeBundleData(Bundle bundle) {
				Object data = null;
				boolean found = false;
				TreeItem[] items = tree.getItems();
				if (items == null)
					return null;
				for (int i = 0; i < items.length; i++) {
					Object object = items[i].getData();
					data = object;
					if (object instanceof PluginObjectAdapter)
						object = ((PluginObjectAdapter) object).getObject();
					if (bundle.equals(object)) {
						found = true;
						break;
					}
				}
				return found ? data : null;
			}
		});
	}
	
	/*
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public void registryChanged(IRegistryChangeEvent event) {
		Tree tree = getUndisposedTree();
		if (tree == null)
			return;
		final IExtensionDelta[] deltas = event.getExtensionDeltas();
		tree.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (getUndisposedTree() == null)
					return;
				for (int i = 0; i < deltas.length; i++)
					handleDelta(deltas[i]);
			}
		});
	}
	
	private void handleDelta(IExtensionDelta delta) {
		IExtension ext = delta.getExtension();
		IExtensionPoint extPoint = delta.getExtensionPoint();
		if (delta.getKind() == IExtensionDelta.ADDED) {
			addToTree(ext);
			addToTree(extPoint);
		} else if (delta.getKind() == IExtensionDelta.REMOVED) {
			removeFromTree(ext);
			removeFromTree(extPoint);
		}
	}
	
	private void addToTree(Object object) {
		String namespace = getNamespaceIdentifier(object);
		if (namespace == null)
			return;
		TreeItem[] bundles = fTreeViewer.getTree().getItems();
		for (int i = 0; i < bundles.length; i++) {
			Object data = bundles[i].getData();
			Object adapted = null;
			if (data instanceof PluginObjectAdapter)
				adapted = ((PluginObjectAdapter)data).getObject();
			if (adapted instanceof Bundle && ((Bundle)adapted).getSymbolicName().equals(namespace)) {
				// TODO fix this method
				if (true) {
					fTreeViewer.refresh(data);
					return;
				}
				TreeItem[] folders = bundles[i].getItems();
				for (int j = 0; j < folders.length; j++) {
					IBundleFolder folder = (IBundleFolder)folders[j].getData();
					if (correctFolder(folder, object)) {
						fTreeViewer.add(folder, object);
						return;
					}
				}
				// folder not found - 1st extension - refresh bundle item
				fTreeViewer.refresh(data);
			}
		}
	}
	
	private String getNamespaceIdentifier(Object object) {
		if (object instanceof IExtensionPoint)
			return ((IExtensionPoint)object).getNamespaceIdentifier();
		if (object instanceof IExtension)
			return ((IExtension)object).getContributor().getName();
		return null;
	}
	
	private boolean correctFolder(IBundleFolder folder, Object child) {
		if (folder == null)
			return false;
		if (child instanceof IExtensionPoint)
			return folder.getFolderId() == IBundleFolder.F_EXTENSION_POINTS;
		if (child instanceof IExtension)
			return folder.getFolderId() == IBundleFolder.F_EXTENSIONS;
		return false;
	}
	
	private void removeFromTree(Object object) {
		String namespace = getNamespaceIdentifier(object);
		if (namespace == null)
			return;
		TreeItem[] bundles = fTreeViewer.getTree().getItems();
		for (int i = 0; i < bundles.length; i++) {
			Object data = bundles[i].getData();
			Object adapted = null;
			if (data instanceof PluginObjectAdapter)
				adapted = ((PluginObjectAdapter)data).getObject();
			if (adapted instanceof Bundle && ((Bundle)adapted).getSymbolicName().equals(namespace)) {
				TreeItem[] folders = bundles[i].getItems();
				// TODO fix this method
				if (true) {
					fTreeViewer.refresh(data);
					return;
				}
				for (int j = 0; j < folders.length; j++) {
					IBundleFolder folder = (IBundleFolder)folders[j].getData();
					if (correctFolder(folder, object)) {
						fTreeViewer.remove(object);
						return;
					}
				}
				// folder not found - 1st extension - refresh bundle item
				fTreeViewer.refresh(data);
			}
		}
	}
	
	/*
	 * toolbar and context menu actions
	 */
	public void makeActions() {
		fRefreshAction = new Action("refresh") { //$NON-NLS-1$
			public void run() {
				BusyIndicator.showWhile(fTreeViewer.getTree().getDisplay(),
						new Runnable() {
					public void run() {
						fTreeViewer.refresh();
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
		
		fShowAdvancedOperationsAction = new Action(PDERuntimeMessages.RegistryView_showAdvanced_label) {
			public void run() {
				if (fShowAdvancedOperationsAction.isChecked())
					createAdvancedActions();
				else
					disabledAdvancedActions();
			}
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
					DisabledInfo info = new DisabledInfo("ui", "Bundle disabled via PDE", desc); //$NON-NLS-1$ //$NON-NLS-2$
					PlatformAdmin platformAdmin = 
						PDERuntimePlugin.getDefault().getPlatformAdmin();
					platformAdmin.addDisabledInfo(info);
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
	
	protected void createAdvancedActions() {
		// TODO Auto-generated method stub
		
	}

	protected void disabledAdvancedActions() {
		// TODO Auto-generated method stub
		
	}

	public void updateTitle(){
		if (fTreeViewer == null || fTreeViewer.getContentProvider() == null)
			return;
		setContentDescription(((RegistryBrowserContentProvider)fTreeViewer.getContentProvider()).getTitleSummary(fTotalBundles));
	}
	
	private Tree getUndisposedTree() {
		if (fTreeViewer == null || fTreeViewer.getTree() == null || fTreeViewer.getTree().isDisposed())
			return null;
		return fTreeViewer.getTree();
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
	
}
