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
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.ui.*;
public class RegistrySearchMenu extends ContributionItem {
	private Action useUniqueIdSearchAction;
	private Action usePluginNameSearchAction;
	private Action refreshAction;
	private boolean enabled;
	private IWorkbenchWindow window;
	private TreeViewer treeViewer;
	private boolean isId;
	private static final String SEARCH_REGISTRY = "RegistrySearchMenu.dialog.title";
	private static final String PLUGIN_NAME = "RegistryMenu.pluginName";
	private static final String PLUGIN_ID = "RegistryMenu.pluginId";
	private String searchText;
	public class UniqueIdSearchAction extends Action {
		public UniqueIdSearchAction(String text) {
			super(text);
		}
		public void run() {
			handlePluginIdSearch();
		}
	}
	public class PluginNameSearchAction extends Action {
		public PluginNameSearchAction(String text) {
			super(text);
		}
		public void run() {
			handlePluginNameSearch();
		}
	}
	/**
	 * Create a new wizard shortcut menu.
	 * <p>
	 * If the menu will appear on a semi-permanent basis, for instance within a
	 * toolbar or menubar, the value passed for <code>register</code> should
	 * be true. If set, the menu will listen to perspective activation and
	 * update itself to suit. In this case clients are expected to call <code>deregister</code>
	 * when the menu is no longer needed. This will unhook any perspective
	 * listeners.
	 * </p>
	 * 
	 * @param innerMgr
	 *            the location for the shortcut menu contents
	 * @param window
	 *            the window containing the menu
	 * @param register
	 *            if <code>true</code> the menu listens to perspective
	 *            changes in the window
	 * @param treeViewer
	 *            the viewer which this context menu belongs to
	 * @param refreshAction
	 *            the action that refreshes the viewer
	 * @param isEnabled
	 *            true if menu actions should be enabled
	 */
	public RegistrySearchMenu(IMenuManager innerMgr, IWorkbenchWindow window,
			boolean register, TreeViewer treeViewer,
			Action refreshAction, boolean isEnabled) {
		this(window);
		this.treeViewer = treeViewer;
		this.refreshAction = refreshAction;
		this.enabled = isEnabled;
		fillMenu(innerMgr);
		// Must be done after constructor to ensure field initialization.
	}
	public RegistrySearchMenu(IWorkbenchWindow window) {
		super();
		this.window = window;
		//		showDlgAction = ActionFactory.NEW.create(window);
		useUniqueIdSearchAction = new UniqueIdSearchAction("unique id");
		useUniqueIdSearchAction.setText(PDERuntimePlugin
				.getResourceString(PLUGIN_ID));
		usePluginNameSearchAction = new PluginNameSearchAction("plug-in name");
		usePluginNameSearchAction.setText(PDERuntimePlugin
				.getResourceString(PLUGIN_NAME));
	}
	private void fillMenu(IContributionManager innerMgr) {
		// Remove all.
		innerMgr.removeAll();
		innerMgr.add(useUniqueIdSearchAction);
		useUniqueIdSearchAction.setEnabled(enabled);
		innerMgr.add(usePluginNameSearchAction);
		usePluginNameSearchAction.setEnabled(enabled);
	}
	private void handlePluginIdSearch() {
		isId = true;
		openSearchDialog();
		((RegistryBrowserContentProvider) treeViewer.getContentProvider())
				.setUniqueIdSearch(searchText);
		if (searchText!=null && searchText.length()!=0){
			((RegistryBrowserLabelProvider)treeViewer.getLabelProvider()).setUseUniqueId(true);
			((RegistryBrowserContentProvider) treeViewer.getContentProvider()).setViewerPlugins(null);
		}
		treeViewer.refresh();
	}
	private void handlePluginNameSearch() {
		isId = false;
		openSearchDialog();
		((RegistryBrowserContentProvider) treeViewer.getContentProvider())
				.setNameSearch(searchText);
		if (searchText!=null && searchText.length()!=0){
			((RegistryBrowserLabelProvider)treeViewer.getLabelProvider()).setUseUniqueId(false);
			((RegistryBrowserContentProvider) treeViewer.getContentProvider()).setViewerPlugins(null);
		}
		treeViewer.refresh();
	}
	private void openSearchDialog() {
		RegistrySearchDialog dialog = new RegistrySearchDialog(window
				.getShell(), isId);
		dialog.create();
		dialog.getShell().setText(
				PDERuntimePlugin.getResourceString(SEARCH_REGISTRY)); //$NON-NLS-1$
		dialog.getShell().setSize(250, 150);
		if (dialog.open() == Dialog.OK) {
			searchText = dialog.getSearchText();
		}
	}
}
