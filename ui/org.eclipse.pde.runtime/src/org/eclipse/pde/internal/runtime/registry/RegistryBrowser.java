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
package org.eclipse.pde.internal.runtime.registry;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.pde.internal.runtime.IHelpContextIds;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.properties.*;
import org.osgi.framework.*;
public class RegistryBrowser extends ViewPart
		implements
			BundleListener,
			IRegistryChangeListener {
	public static final String KEY_REFRESH_LABEL = "RegistryView.refresh.label";
	public static final String KEY_REFRESH_TOOLTIP = "RegistryView.refresh.tooltip";
	public static final String SEARCH_BY = "RegistryBrowser.menu.searchBy";
	public static final String SHOW_ONLY = "RegistryBrowser.menu.show";
	private TreeViewer treeViewer;
	private Action refreshAction;
	private DrillDownAdapter drillDownAdapter;
	//attributes view
	private SashForm fSashForm;
	private Label fPropertyLabel;
	private Label fPropertyImage;
	private PropertySheetPage fPropertySheet;
	public RegistryBrowser() {
		super();
		makeActions();
		Platform.getExtensionRegistry().addRegistryChangeListener(this);
		PDEPlugin.getDefault().getBundleContext().addBundleListener(this);
	}
	public void makeActions() {
		refreshAction = new Action("refresh") {
			public void run() {
				BusyIndicator.showWhile(treeViewer.getTree().getDisplay(),
						new Runnable() {
							public void run() {
								((RegistryBrowserContentProvider) treeViewer
										.getContentProvider())
										.setShowType(ShowPluginsMenu.SHOW_ALL_PLUGINS);
								treeViewer.refresh();
							}
						});
			}
		};
		refreshAction.setText(PDERuntimePlugin
				.getResourceString(KEY_REFRESH_LABEL));
		refreshAction.setToolTipText(PDERuntimePlugin
				.getResourceString(KEY_REFRESH_TOOLTIP));
		refreshAction.setImageDescriptor(PDERuntimePluginImages.DESC_REFRESH);
		refreshAction
				.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_DISABLED);
		refreshAction
				.setHoverImageDescriptor(PDERuntimePluginImages.DESC_REFRESH_HOVER);
	}
	public void dispose() {
		super.dispose();
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
		PDEPlugin.getDefault().getBundleContext().removeBundleListener(this);
	}
	public void setFocus() {
	}
	public void createPartControl(Composite parent) {
		// create the sash form that will contain the tree viewer & text viewer
		fSashForm = new SashForm(parent, SWT.HORIZONTAL);
		fSashForm.setLayout(new GridLayout());
		fSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		setSashForm(fSashForm);
		createTreeViewer();
		createAttributesViewer();
	}
	private void createTreeViewer() {
		Tree tree = new Tree(getSashForm(), SWT.FLAT);
		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new RegistryBrowserContentProvider(
				treeViewer));
		treeViewer
				.setLabelProvider(new RegistryBrowserLabelProvider(treeViewer));
		treeViewer.setUseHashlookup(true);
		treeViewer.setSorter(new ViewerSorter() {
		});
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
		drillDownAdapter = new DrillDownAdapter(treeViewer);
		IViewSite site = getViewSite();
		IToolBarManager mng = site.getActionBars().getToolBarManager();
		drillDownAdapter.addNavigationActions(mng);
		mng.add(new Separator());
		mng.add(refreshAction);
		treeViewer.setInput(new PluginObjectAdapter(Platform
				.getPluginRegistry()));
		site.setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((IStructuredSelection) event.getSelection())
						.getFirstElement();
				updateAttributesView(selection);
			}
		});
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection) event.getSelection())
						.getFirstElement();
				updateAttributesView(selection);
				if (selection != null && treeViewer.isExpandable(selection))
					treeViewer.setExpandedState(selection, !treeViewer
							.getExpandedState(selection));
				boolean isOpeningExtensionSet = (selection != null
						&& selection instanceof IPluginFolder && ((IPluginFolder) selection)
						.getFolderId() == 1);
				((RegistryBrowserLabelProvider) treeViewer.getLabelProvider())
						.setIsInExtensionSet(isOpeningExtensionSet);
			}
		});
		treeViewer.addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}
			public void treeExpanded(TreeExpansionEvent event) {
				Object selection = event.getElement();
				boolean isOpeningExtensionSet = (selection instanceof IPluginFolder && ((IPluginFolder) selection)
						.getFolderId() == 1);
				((RegistryBrowserLabelProvider) treeViewer.getLabelProvider())
						.setIsInExtensionSet(isOpeningExtensionSet);
			}
		});
		WorkbenchHelp.setHelp(treeViewer.getControl(),
				IHelpContextIds.REGISTRY_VIEW);
	}
	public void fillContextMenu(IMenuManager manager) {
		MenuManager showMenu = new MenuManager(PDERuntimePlugin
				.getResourceString(SHOW_ONLY));
		manager.add(showMenu);
		new ShowPluginsMenu(showMenu, false, treeViewer);
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
	}
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	/* add attributes viewer */
	protected void createAttributesViewer() {
		Composite composite = new Composite(getSashForm(), SWT.FLAT);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		fPropertyImage = new Label(composite, SWT.NONE);
		GridData gd = new GridData(GridData.FILL);
		gd.widthHint = 20;
		fPropertyImage.setLayoutData(gd);
		fPropertyLabel = new Label(composite, SWT.NULL);
		fPropertyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createPropertySheet(composite);
	}
	protected void createPropertySheet(Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);
		fPropertySheet = new PropertySheetPage();
		fPropertySheet.createControl(composite);
		gd = new GridData(GridData.FILL_BOTH);
		fPropertySheet.getControl().setLayoutData(gd);
		fPropertySheet.makeContributions(new MenuManager(),
				new ToolBarManager(), null);
	}
	public void updateAttributesView(Object selection) {
		fPropertyImage.setImage(((RegistryBrowserLabelProvider) treeViewer
				.getLabelProvider()).getImage(selection));
		fPropertyLabel.setText(((RegistryBrowserLabelProvider) treeViewer
				.getLabelProvider()).getText(selection));
		if (selection != null)
			fPropertySheet.selectionChanged(null, new StructuredSelection(
					selection));
		else
			fPropertySheet.selectionChanged(null, new StructuredSelection(
					new Object()));
	}
	protected SashForm getSashForm() {
		return fSashForm;
	}
	private void setSashForm(SashForm sashForm) {
		fSashForm = sashForm;
	}
	/*
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {
		final RegistryBrowserContentProvider provider = ((RegistryBrowserContentProvider) treeViewer
				.getContentProvider());
		final IPluginDescriptor descriptor = Platform.getPluginRegistry()
				.getPluginDescriptor(event.getBundle().getGlobalName());
		if (descriptor == null)
			return;
		final PluginObjectAdapter adapter = new PluginObjectAdapter(descriptor);
		treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				TreeItem[] items = treeViewer.getTree().getItems();
				if (items != null) {
					for (int i = 0; i < items.length; i++) {
						PluginObjectAdapter plugin = (PluginObjectAdapter) items[i]
								.getData();
						if (adapter != null) {
							Object object = plugin.getObject();
							if (object instanceof IPluginDescriptor) {
								IPluginDescriptor desc = (IPluginDescriptor) object;
								if (desc.equals(descriptor)) {
									treeViewer.remove(plugin);
									break;
								}
							}
						}
					}
				}
				switch (provider.getShowType()) {
					case ShowPluginsMenu.SHOW_ALL_PLUGINS :
						treeViewer.add(treeViewer.getInput(), adapter);
						break;
					case ShowPluginsMenu.SHOW_RUNNING_PLUGINS :
						if (descriptor.isPluginActivated())
							treeViewer.add(treeViewer.getInput(), adapter);
						break;
					case ShowPluginsMenu.SHOW_NON_RUNNING_PLUGINS :
						if (!descriptor.isPluginActivated())
							treeViewer.add(treeViewer.getInput(), adapter);
						break;
					default :
						break;
				}
			}
		});
	}
	/*
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public void registryChanged(IRegistryChangeEvent event) {
		final IExtensionDelta[] deltas = event.getExtensionDeltas();
		treeViewer.getTree().getDisplay().syncExec(new Runnable() {
			public void run() {
				for (int i = 0; i < deltas.length; i++) {
					IExtension ext = deltas[i].getExtension();
					IExtensionPoint extPoint = deltas[i].getExtensionPoint();
					IPluginDescriptor descriptor = extPoint
							.getDeclaringPluginDescriptor();
					PluginObjectAdapter adapter = new PluginObjectAdapter(
							descriptor);

					if (deltas[i].getKind() == IExtensionDelta.ADDED) {
						if (ext != null)
							treeViewer.add(adapter, ext);
						if (extPoint != null)
							treeViewer.add(adapter, extPoint);
					} else { 
						if (ext != null)
							treeViewer.remove(ext);
						if (extPoint != null)
							treeViewer.remove(extPoint);
						treeViewer.refresh();
					}

				}
			}
		});
	}
}
