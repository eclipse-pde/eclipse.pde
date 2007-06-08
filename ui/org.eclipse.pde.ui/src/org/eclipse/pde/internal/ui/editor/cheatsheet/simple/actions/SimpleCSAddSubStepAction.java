/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions;

import java.util.HashSet;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractAddAction;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSAddSubStepAction extends CSAbstractAddAction {

	private ISimpleCSItem fItem;
	
	private ISimpleCSSubItem fSubitem;
	
	/**
	 * 
	 */
	public SimpleCSAddSubStepAction() {
		setText(PDEUIMessages.SimpleCSAddSubStepAction_0);
	}

	/**
	 * @param cheatsheet
	 */
	public void setDataObject(ISimpleCSObject csObject) {
		// Determine input
		if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			fSubitem = null;
			fItem = (ISimpleCSItem)csObject;
		} else if (csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			fSubitem = (ISimpleCSSubItem)csObject;
			ISimpleCSObject parentObject = fSubitem.getParent();
			// Determine input's parent object
			if (parentObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				fItem = (ISimpleCSItem)parentObject;
			} else if (parentObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) {
				// Not supported by editor, action will not run
				fItem = null;
			} else if (parentObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) {
				// Note supported by editor, action will not run
				fItem = null;
			}
		} else {
			// Invalid input, action will not run
			fSubitem = null;
			fItem = null;
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		// Ensure we have valid input
		if (fItem == null) {
			return;
		}
		// Create the new subitem
		ISimpleCSSubItem newSubItem = createNewSubItem();		
		// Insert the new subitem
		insertNewSubItem(newSubItem);
	}

	/**
	 * @return
	 */
	private ISimpleCSSubItem createNewSubItem() {
		ISimpleCSModelFactory factory = fItem.getModel().getFactory();
		// Element: subitem
		ISimpleCSSubItem subitem = factory.createSimpleCSSubItem(fItem);
		// Set on the proper parent object
		subitem.setLabel(generateSubItemLabel(fItem, PDEUIMessages.SimpleCSAddSubStepAction_1));
		return subitem;
	}
	
	/**
	 * @param newSubItem
	 */
	private void insertNewSubItem(ISimpleCSSubItem newSubItem) {
		// Insert the new subitem depending on the input specfied
		if (fSubitem != null) {
			// Subitem input object
			// Insert subitem right after the input item object
			int index = fItem.indexOfSubItem(fSubitem) + 1;
			fItem.addSubItem(index, newSubItem);
		} else {
			// Item input object
			// Insert subitem as the last child subitem
			fItem.addSubItem(newSubItem);
		}
	}	
	
	/**
	 * @return
	 */
	private String generateSubItemLabel(ISimpleCSItem item, String base) {
		StringBuffer result = new StringBuffer(base);
		ISimpleCSSubItemObject[] subitems = item.getSubItems();
		// Used to track auto-generated numbers used
		HashSet set = new HashSet();

		// Linear search O(n).  
		// Performance hit unnoticeable because number of items per cheatsheet
		// should be minimal.
		for (int i = 0; i < subitems.length; i++) {
			ISimpleCSSubItemObject object = subitems[i];
			if (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem)object;
				compareTitleWithBase(base, set, subitem.getLabel());
			}
		}
		// Add an auto-generated number
		addNumberToBase(result, set);
		
		return result.toString();
	}	
}
