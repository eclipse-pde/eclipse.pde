/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public abstract class PDEOutlinePage extends ContentOutlinePage {

	protected PDEFormEditor fEditor;

	public PDEOutlinePage(PDEFormEditor editor) {
		fEditor = editor;
	}

	public PDEOutlinePage() {
	}

	@Override
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {

		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = manager -> {
			ISelection selection = getSelection();
			PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
			if (fEditor != null)
				actionGroup.setBaseModel(fEditor.getAggregateModel());
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
		};

		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = getTreeViewer().getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
	}

}
