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
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.part.*;
public class RegistryBrowser extends ViewPart {
	public static final String KEY_REFRESH_LABEL = "RegistryView.refresh.label";
	public static final String KEY_REFRESH_TOOLTIP = "RegistryView.refresh.tooltip";
	public static final String SEARCH_BY = "RegistryBrowser.menu.searchBy";
	public static final String SHOW_ONLY = "RegistryBrowser.menu.show";
	private TreeViewer treeViewer;
	private Action refreshAction;
	private DrillDownAdapter drillDownAdapter;
	
	public RegistryBrowser() {
		super();
		makeActions();
	}
	
	public void makeActions(){
		refreshAction = new Action("refresh") {
			public void run() {
				BusyIndicator.showWhile(treeViewer.getTree().getDisplay(),
						new Runnable() {
							public void run() {
								((RegistryBrowserContentProvider)treeViewer.getContentProvider()).setShowPlugins(ShowPluginsMenu.SHOW_ALL_PLUGINS);
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
	public void createPartControl(Composite parent) {
		Tree tree = new Tree(parent, SWT.NONE);
		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new RegistryBrowserContentProvider(treeViewer));
		treeViewer.setLabelProvider(new RegistryBrowserLabelProvider(treeViewer));
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
		
		treeViewer.addDoubleClickListener(new IDoubleClickListener(){

			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection) event.getSelection())
				.getFirstElement();
				if (selection!=null && treeViewer.isExpandable(selection))
					treeViewer.setExpandedState(selection, !treeViewer.getExpandedState(selection));
				
				boolean isOpeningExtensionSet = (selection!= null && 
						selection instanceof IPluginFolder && 
						((IPluginFolder)selection).getFolderId() ==1);
				
				((RegistryBrowserLabelProvider)treeViewer.getLabelProvider()).setIsInExtensionSet(isOpeningExtensionSet);
			}
		});

		
		treeViewer.addTreeListener(new ITreeViewerListener(){

			public void treeCollapsed(TreeExpansionEvent event) {
			}

			public void treeExpanded(TreeExpansionEvent event) {
				 Object selection = event.getElement();
				 boolean isOpeningExtensionSet = (selection instanceof IPluginFolder && 
						((IPluginFolder)selection).getFolderId() ==1);
				
				((RegistryBrowserLabelProvider)treeViewer.getLabelProvider()).setIsInExtensionSet(isOpeningExtensionSet);
			}
		});
		WorkbenchHelp.setHelp(treeViewer.getControl(),
				IHelpContextIds.REGISTRY_VIEW);
	}
	public void fillContextMenu(IMenuManager manager) {
		
		MenuManager showMenu = new MenuManager(PDERuntimePlugin.getResourceString(SHOW_ONLY));
		manager.add(showMenu);
		new ShowPluginsMenu(showMenu, false, treeViewer);
		
		MenuManager searchMenu = new MenuManager(PDERuntimePlugin.getResourceString(SEARCH_BY));
		manager.add(searchMenu);
		new RegistrySearchMenu(searchMenu, getViewSite().getWorkbenchWindow(),
				false, treeViewer,!drillDownAdapter.canGoHome());
		
		manager.add(refreshAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
	}
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	public void setFocus() {
	}
	private void createVerticalLine(Composite parent) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.widthHint = 1;
		line.setLayoutData(gd);
	}
}
