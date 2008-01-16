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
import org.eclipse.pde.internal.core.icheatsheet.simple.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSRemoveStepAction extends Action {

	private ISimpleCSItem fItem;

	private ISimpleCSObject fObjectToSelect;

	/**
	 * 
	 */
	public SimpleCSRemoveStepAction() {
		setText(PDEUIMessages.SimpleCSRemoveStepAction_0);
		// TODO: MP: LOW: SimpleCS:  Add tool-tip / image ?
//		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
		fItem = null;
		fObjectToSelect = null;
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
			// Parent can only be a cheat sheet
			ISimpleCS cheatsheet = (ISimpleCS) fItem.getParent();
			// Determine the item to select after the deletion takes place 
			determineItemToSelect(cheatsheet);
			// Remove the item
			cheatsheet.removeItem(fItem);
		}
	}

	/**
	 * @param cheatsheet
	 */
	private void determineItemToSelect(ISimpleCS cheatsheet) {
		// Select the next sibling
		fObjectToSelect = cheatsheet.getNextSibling(fItem);
		if (fObjectToSelect == null) {
			// No next sibling
			// Select the previous sibling
			fObjectToSelect = cheatsheet.getPreviousSibling(fItem);
			if (fObjectToSelect == null) {
				// No previous sibling
				// Select the parent
				fObjectToSelect = cheatsheet;
			}
		}
	}

	/**
	 * @return
	 */
	public ISimpleCSObject getObjectToSelect() {
		return fObjectToSelect;
	}
}
