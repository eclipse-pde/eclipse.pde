/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItemObject;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSRemoveSubStepAction extends Action {

	private ISimpleCSSubItemObject fSubItem;

	private ISimpleCSObject fObjectToSelect;

	/**
	 * 
	 */
	public SimpleCSRemoveSubStepAction() {
		// TODO: MP: LOW: SimpleCS:  Add tool-tip / image ?
		setText(SimpleActionMessages.SimpleCSRemoveSubStepAction_actionText);
//		setImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
		fSubItem = null;
		fObjectToSelect = null;
	}

	/**
	 * @param subitem
	 */
	public void setSubItem(ISimpleCSSubItemObject subitem) {
		fSubItem = subitem;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fSubItem != null) {
			// Determine parent type and remove accordingly 
			ISimpleCSObject parent = fSubItem.getParent();
			if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Parent is an item
				ISimpleCSItem item = (ISimpleCSItem) parent;
				// Determine the item to select after the deletion takes place 
				determineItemToSelect(item);
				// Remove the subitem
				item.removeSubItem(fSubItem);
			} else if ((parent.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) && (fSubItem.getType() == ISimpleCSConstants.TYPE_SUBITEM)) {
				// Parent is a repeated subitem
				ISimpleCSRepeatedSubItem subitem = (ISimpleCSRepeatedSubItem) parent;
				// Determine the item to select after the deletion takes place 
				determineItemToSelect(subitem);
				// Remove the subitem
				subitem.setSubItem(null);
			} else if ((parent.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) && (fSubItem.getType() == ISimpleCSConstants.TYPE_SUBITEM)) {
				// Parent is a conditional subitem
				ISimpleCSConditionalSubItem subitem = (ISimpleCSConditionalSubItem) parent;
				// Determine the item to select after the deletion takes place 
				determineItemToSelect(subitem);
				// Remove the subitem
				subitem.removeSubItem((ISimpleCSSubItem) fSubItem);
			}
		}
	}

	/**
	 * @param item
	 */
	private void determineItemToSelect(ISimpleCSItem item) {
		// Select the next sibling
		fObjectToSelect = item.getNextSibling(fSubItem);
		if (fObjectToSelect == null) {
			// No next sibling
			// Select the previous sibling
			fObjectToSelect = item.getPreviousSibling(fSubItem);
			if (fObjectToSelect == null) {
				// No previous sibling
				// Select the parent
				fObjectToSelect = item;
			}
		}
	}

	/**
	 * @param item
	 */
	private void determineItemToSelect(ISimpleCSObject object) {
		// The parent itself
		fObjectToSelect = object;
	}

	/**
	 * @return
	 */
	public ISimpleCSObject getObjectToSelect() {
		return fObjectToSelect;
	}

}
