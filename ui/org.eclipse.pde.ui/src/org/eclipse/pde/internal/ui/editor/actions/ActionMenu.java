/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.actions;

import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionMenu extends Action implements IMenuCreator {

	Action[] fActions;
	Menu fMenu;

	public ActionMenu(Action[] actions) {
		fActions = actions;
		if (fActions.length > 0) {
			setToolTipText(fActions[0].getToolTipText());
			setImageDescriptor(fActions[0].getImageDescriptor());
			if (fActions.length > 1)
				setMenuCreator(this);
		}
	}

	public void run() {
		if (fActions.length > 0)
			fActions[0].run();
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null)
			fMenu.dispose();
		fMenu = new Menu(parent);

		for (int i = 0; i < fActions.length; i++) {
			addActionToMenu(fMenu, fActions[i]);
		}
		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(parent, -1);
	}
}
