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
import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
public class ListingMenu extends ContributionItem {
	private Action useUniqueIdAction;
	private Action usePluginNameAction;
	private Action refreshAction;
	private boolean enabled;
	private IWorkbenchWindow window;
	private TreeViewer treeViewer;
	public static final String PLUGIN_ID = "RegistryMenu.pluginId";
	public static final String PLUGIN_NAME = "RegistryMenu.pluginName";

	public class UniqueIdAction extends Action {
		public UniqueIdAction(String text) {
			super(text);
		}
		public void run() {
			handlePluginId();
		}
	}
	public class PluginNameAction extends Action {
		public PluginNameAction(String text) {
			super(text);
		}
		public void run() {
			handlePluginName();
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
	 * @param treeViewre
	 * 				the viewer to which this context menu belongs to
	 * @param refreshAction
	 * 				the action that refreshes the viewer
	 * @param isEnabled
	 * 				true if menu actions should be enabled
	 */
	public ListingMenu(IMenuManager innerMgr, IWorkbenchWindow window,
			boolean register, TreeViewer treeViewer, Action refreshAction, boolean isEnabled) {
		this(window);
		this.treeViewer = treeViewer;
		this.refreshAction = refreshAction;
		this.enabled = isEnabled;
		fillMenu(innerMgr);
		// Must be done after constructor to ensure field initialization.
	}
	public ListingMenu(IWorkbenchWindow window) {
		super();
		this.window = window;
		//		showDlgAction = ActionFactory.NEW.create(window);
		useUniqueIdAction = new UniqueIdAction("unique id");
		useUniqueIdAction.setText(PDERuntimePlugin.getResourceString(PLUGIN_ID));
		usePluginNameAction = new PluginNameAction("plug-in name");
		usePluginNameAction.setText(PDERuntimePlugin.getResourceString(PLUGIN_NAME));
	}
	/*
	 * (non-Javadoc) Fills the menu with New Wizards.
	 */
	private void fillMenu(IContributionManager innerMgr) {
		// Remove all.
		innerMgr.removeAll();
		innerMgr.add(useUniqueIdAction);
		useUniqueIdAction.setEnabled(this.enabled);
		innerMgr.add(usePluginNameAction);
		usePluginNameAction.setEnabled(this.enabled);
	}
	private void handlePluginId() {
		TreeItem[] items = treeViewer.getTree().getItems();
		ArrayList list = new ArrayList();
		for (int i =0 ; i<items.length; i++){
			PluginObjectAdapter adapter = (PluginObjectAdapter)items[i].getData();
			list.add(adapter);
		}
		((RegistryBrowserLabelProvider) treeViewer.getLabelProvider()).setUseUniqueId(true);
		((RegistryBrowserContentProvider) treeViewer.getContentProvider()).setViewerPlugins((PluginObjectAdapter[])list.toArray(new PluginObjectAdapter[list.size()]));
		treeViewer.refresh();
	}
	private void handlePluginName() {
		TreeItem[] items = treeViewer.getTree().getItems();
		ArrayList list = new ArrayList();
		for (int i =0 ; i<items.length; i++){
			PluginObjectAdapter adapter = (PluginObjectAdapter)items[i].getData();
			list.add(adapter);
		}
		((RegistryBrowserLabelProvider) treeViewer.getLabelProvider()).setUseUniqueId(false);
		((RegistryBrowserContentProvider) treeViewer.getContentProvider()).setViewerPlugins((PluginObjectAdapter[])list.toArray(new PluginObjectAdapter[list.size()]));
		treeViewer.refresh();
	}
}
