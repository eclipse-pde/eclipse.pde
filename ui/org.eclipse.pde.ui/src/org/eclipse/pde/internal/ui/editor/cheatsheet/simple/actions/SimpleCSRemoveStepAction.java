/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSRemoveStepAction extends Action {

	private ISimpleCSItem fItem;
	
	/**
	 * 
	 */
	public SimpleCSRemoveStepAction() {
		// TODO: MP: Update
		setText(PDEUIMessages.SimpleCSRemoveStepAction_0);
//		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
	}

	/**
	 * @param cheatsheet
	 */
	public void setItem(ISimpleCSItem item) {
		fItem = item;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fItem != null) {
			// TODO: MP: Do not remove the last step
			// Parent can only be a cheat sheet
			ISimpleCS cheatsheet = (ISimpleCS)fItem.getParent(); 
			cheatsheet.removeItem(fItem);
		}
	}
}
