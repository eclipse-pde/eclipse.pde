/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.help.WorkbenchHelp;

public class HistoryListAction extends Action {

	private DependenciesView fView;

	public HistoryListAction(DependenciesView view) {
		fView = view;
		setText(PDEPlugin.getResourceString("HistoryListAction.label")); //$NON-NLS-1$
		setImageDescriptor(PDEPluginImages.DESC_HISTORY_LIST);
		setDisabledImageDescriptor(PDEPluginImages.DESC_HISTORY_LIST_DISABLED);
		WorkbenchHelp.setHelp(this, IHelpContextIds.HISTORY_LIST_ACTION);
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		String[] historyEntries = fView.getHistoryEntries();
		HistoryListDialog dialog = new HistoryListDialog(PDEPlugin
				.getActiveWorkbenchShell(), historyEntries);
		if (dialog.open() == Window.OK) {
			fView.setHistoryEntries(dialog.getRemaining());
			String id = dialog.getResult();
			if (id == null) {
				fView.openTo(null);
			} else {
				fView.openTo(PDECore.getDefault().getModelManager().findModel(
						id));
			}
		}
	}

}
