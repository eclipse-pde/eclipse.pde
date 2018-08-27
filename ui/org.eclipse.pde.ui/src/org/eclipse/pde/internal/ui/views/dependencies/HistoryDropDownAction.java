/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

public class HistoryDropDownAction extends Action implements IMenuCreator {

	public static final int RESULTS_IN_DROP_DOWN = 10;

	private Menu fMenu;

	private DependenciesView fView;

	public HistoryDropDownAction(DependenciesView view) {
		fView = view;
		fMenu = null;
		setToolTipText(PDEUIMessages.HistoryDropDownAction_tooltip);
		setImageDescriptor(PDEPluginImages.DESC_HISTORY_LIST);
		setDisabledImageDescriptor(PDEPluginImages.DESC_HISTORY_LIST_DISABLED);
		setMenuCreator(this);
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	private boolean addEntries(Menu menu, String[] elements) {
		boolean checked = false;

		int min = Math.min(elements.length, RESULTS_IN_DROP_DOWN);
		for (int i = 0; i < min; i++) {
			HistoryAction action = new HistoryAction(fView, elements[i]);
			action.setChecked(elements[i].equals(fView.getInput()));
			checked = checked || action.isChecked();
			addActionToMenu(menu, action);
		}
		return checked;
	}

	@Override
	public void dispose() {
		// action is reused, can be called several times.
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	@Override
	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		String[] elements = fView.getHistoryEntries();
		boolean checked = addEntries(fMenu, elements);
		if (elements.length > RESULTS_IN_DROP_DOWN) {
			new MenuItem(fMenu, SWT.SEPARATOR);
			Action others = new HistoryListAction(fView);
			others.setChecked(checked);
			addActionToMenu(fMenu, others);
		}
		return fMenu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public void run() {
		(new HistoryListAction(fView)).run();
	}
}
