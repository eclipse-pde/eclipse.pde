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
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
public class ShowPluginsMenu extends ContributionItem {
	private TreeViewer treeViewer;
	private IAction refreshAction, showRunningPluginsAction,
			showNonRunningPluginsAction;
	private static final String SHOW_RUNNING = "ShowPluginsMenu.showRunning";
	private static final String SHOW_NON_RUNNING = "ShowPluginsMenu.showNonRunning";
	public static final byte SHOW_RUNNING_PLUGINS = 0x0;
	public static final byte SHOW_NON_RUNNING_PLUGINS = 0x1;
	public static final byte SHOW_ALL_PLUGINS = 0x2;
	public class ShowRunningPluginsAction extends Action {
		public ShowRunningPluginsAction(String text) {
			super(text);
		}
		public void run() {
			handleShowPlugins(SHOW_RUNNING_PLUGINS);
		}
	}
	public class ShowNonRunningPluginsAction extends Action {
		public ShowNonRunningPluginsAction(String text) {
			super(text);
		}
		public void run() {
			handleShowPlugins(SHOW_NON_RUNNING_PLUGINS);
		}
	}
	
	public ShowPluginsMenu(IMenuManager innerMgr, boolean register,
			TreeViewer treeViewer) {
		super();
		showRunningPluginsAction = new ShowRunningPluginsAction(
				"show running plug-ins");
		showRunningPluginsAction.setText(PDERuntimePlugin
				.getResourceString(SHOW_RUNNING));
		showNonRunningPluginsAction = new ShowNonRunningPluginsAction(
				"show non-running plug-ins");
		showNonRunningPluginsAction.setText(PDERuntimePlugin
				.getResourceString(SHOW_NON_RUNNING));
		this.treeViewer = treeViewer;
		fillMenu(innerMgr);
		// Must be done after constructor to ensure field initialization.
	}
	private void fillMenu(IContributionManager innerMgr) {
		RegistryBrowserContentProvider provider = ((RegistryBrowserContentProvider)treeViewer.getContentProvider());
		innerMgr.removeAll();
		innerMgr.add(showRunningPluginsAction);
		showRunningPluginsAction.setEnabled(true);
		showRunningPluginsAction.setChecked(provider.getShowType() == SHOW_RUNNING_PLUGINS);
		innerMgr.add(showNonRunningPluginsAction);
		showNonRunningPluginsAction.setEnabled(true);
		showNonRunningPluginsAction.setChecked(provider.getShowType() == SHOW_NON_RUNNING_PLUGINS);
	}
	private void handleShowPlugins(byte type) {
		((RegistryBrowserContentProvider) treeViewer.getContentProvider())
				.setShowPlugins(type);
		treeViewer.refresh();
	}
}
