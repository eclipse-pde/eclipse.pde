/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 218293
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import java.util.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;

/**
 * Imports log to Log View from such sources as file in local file system, file in workspace,
 * files from log files manager.
 */
public class ImportLogAction extends Action implements IMenuCreator {

	private Menu toolbarMenu = null;
	private Menu popupMenu = null;

	/**
	 * View to import logs to.
	 */
	private final LogView logView;
	private ImportConfigurationLogAction[] actions;
	private IMemento fMemento;

	/**
	 * Action imports log file from given location to Log View.
	 */
	private class ImportConfigurationLogAction extends Action {
		private String name;
		private String location;

		public ImportConfigurationLogAction(String name, String location) {
			super(name, AS_RADIO_BUTTON);
			this.name = name;
			this.location = location;
			setId(name + "#" + location); //$NON-NLS-1$
		}

		protected void doRun() {
			logView.handleImportPath(location);
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			doRun();

			// remember we clicked on that item
			if (isChecked()) {
				fMemento.putString(LogView.P_IMPORT_LOG, getId());
			}
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			if (o instanceof ImportConfigurationLogAction) {
				ImportConfigurationLogAction action = (ImportConfigurationLogAction) o;
				return name.equals(action.name) && location.equals(action.name);
			}

			return false;
		}
	}

	public ImportLogAction(LogView logView, String text, IMemento memento) {
		super(text);
		this.logView = logView;
		this.fMemento = memento;
		setMenuCreator(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		// by default import file selected by user
		logView.handleImport();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
	 */
	public Menu getMenu(Control parent) {
		if (menuUpdateNeeded(toolbarMenu)) {
			toolbarMenu = new Menu(parent);
			createMenuItems(toolbarMenu);
		}
		return toolbarMenu;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	public Menu getMenu(Menu parent) {
		if (menuUpdateNeeded(popupMenu)) {
			popupMenu = new Menu(parent);
			createMenuItems(popupMenu);
		}
		return popupMenu;
	}

	/**
	 * Returns whether menu should be updated or not. Menu should be updated
	 * if either number of actions or any of actions has been changed. 
	 * @return true if menu should be updated, false otherwise
	 */
	private boolean menuUpdateNeeded(Menu menu) {
		boolean result = false;

		ImportConfigurationLogAction[] currActions = getLogActions();

		if (menu == null) {
			result = true;
		} else if (actions == null) {
			result = true;
		} else if (currActions.length != actions.length) {
			result = true;
		} else {
			for (int i = 0; i < currActions.length; i++) {
				if (!currActions[i].equals(actions[i])) {
					result = true;
				}
			}
		}

		if (result == true) {
			actions = currActions;

			if (toolbarMenu != null) {
				toolbarMenu.dispose();
				toolbarMenu = null;
			}
			if (popupMenu != null) {
				popupMenu.dispose();
				popupMenu = null;
			}
		}

		return result;
	}

	/**
	 * Returns list of all actions from LogFilesManager.
	 * @return list of all actions from LogFilesManager
	 */
	private ImportConfigurationLogAction[] getLogActions() {
		List result = new ArrayList();
		Map sources = LogFilesManager.getLogSources();

		for (Iterator j = sources.keySet().iterator(); j.hasNext();) {
			String name = (String) j.next();
			String location = (String) sources.get(name);
			result.add(new ImportConfigurationLogAction(name, location));
		}

		return (ImportConfigurationLogAction[]) result.toArray(new ImportConfigurationLogAction[result.size()]);
	}

	/**
	 * Builds menu of ImportLogAction actions from log files provided by LogFilesManager.
	 * 
	 * @see IMenuCreator#getMenu(Control)
	 */
	private void createMenuItems(Menu menu) {
		String previouslyCheckedActionId = fMemento.getString(LogView.P_IMPORT_LOG);
		if (actions.length == 0) {
			Action action = new Action(Messages.ImportLogAction_noLaunchHistory) {
				// dummy action
			};
			action.setEnabled(false);
			ActionContributionItem actionItem = new ActionContributionItem(action);
			actionItem.fill(menu, -1);
		} else {
			for (int i = 0; i < actions.length; i++) {
				actions[i].setChecked(actions[i].getId().equals(previouslyCheckedActionId) && !logView.isPlatformLogOpen());
				ActionContributionItem item = new ActionContributionItem(actions[i]);
				item.fill(menu, -1);
			}
		}

		(new Separator()).fill(menu, -1);
		ImportConfigurationLogAction importWorkspaceLogAction = new ImportConfigurationLogAction(Messages.ImportLogAction_reloadWorkspaceLog, Platform.getLogFileLocation().toFile().getAbsolutePath()) {

			public void doRun() {
				logView.setPlatformLog();
			}

		};
		importWorkspaceLogAction.setChecked(logView.isPlatformLogOpen());
		ActionContributionItem item = new ActionContributionItem(importWorkspaceLogAction);
		item.fill(menu, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuCreator#dispose()
	 */
	public void dispose() {
		if (toolbarMenu != null) {
			toolbarMenu.dispose();
			toolbarMenu = null;
		}
		if (popupMenu != null) {
			popupMenu.dispose();
			popupMenu = null;
		}
	}
}
