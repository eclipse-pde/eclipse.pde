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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.PlatformUI;

public class HistoryListAction extends Action {

	private DependenciesView fView;

	public HistoryListAction(DependenciesView view) {
		fView = view;
		setText(PDEUIMessages.HistoryListAction_label);
		setImageDescriptor(PDEPluginImages.DESC_HISTORY_LIST);
		setDisabledImageDescriptor(PDEPluginImages.DESC_HISTORY_LIST_DISABLED);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IHelpContextIds.HISTORY_LIST_ACTION);
	}

	@Override
	public void run() {
		String[] historyEntries = fView.getHistoryEntries();
		HistoryListDialog dialog = new HistoryListDialog(PDEPlugin.getActiveWorkbenchShell(), historyEntries);
		if (dialog.open() == Window.OK) {
			fView.setHistoryEntries(dialog.getRemaining());
			String id = dialog.getResult();
			if (id == null) {
				fView.openTo(null);
			} else {
				fView.openTo(PluginRegistry.findModel(id));
			}
		}
	}

}
