/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;

public class SimpleCSRemoveSubStepAction extends Action {

	private ISimpleCSSubItemObject fSubItem;

	private ISimpleCSObject fObjectToSelect;

	public SimpleCSRemoveSubStepAction() {
		setText(SimpleActionMessages.SimpleCSRemoveSubStepAction_actionText);
		fSubItem = null;
		fObjectToSelect = null;
	}

	public void setSubItem(ISimpleCSSubItemObject subitem) {
		fSubItem = subitem;
	}

	@Override
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

	private void determineItemToSelect(ISimpleCSObject object) {
		// The parent itself
		fObjectToSelect = object;
	}

	public ISimpleCSObject getObjectToSelect() {
		return fObjectToSelect;
	}

}
