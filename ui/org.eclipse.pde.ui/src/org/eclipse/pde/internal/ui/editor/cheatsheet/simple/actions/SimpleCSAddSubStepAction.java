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

	private ISimpleCSObject fParentObject;
	
	/**
	 * 
	 */
	public SimpleCSAddSubStepAction() {
		setText(PDEUIMessages.SimpleCSAddSubStepAction_0);
	}

	/**
	 * @param cheatsheet
	 */
	public void setParentObject(ISimpleCSObject object) {
		fParentObject = object;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		
		if (fParentObject == null) {
			return;
		}
		
		ISimpleCSModelFactory factory = fParentObject.getModel().getFactory();
		
		// Element: subitem
		ISimpleCSSubItem subitem = factory.createSimpleCSSubItem(fParentObject);
		// Set on the proper parent object
		if (fParentObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem item = (ISimpleCSItem)fParentObject;
			subitem.setLabel(generateSubItemLabel(item, PDEUIMessages.SimpleCSAddSubStepAction_1));

			item.addSubItem(subitem);
		} else if (fParentObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) {
			// Not supported by editor
		} else if (fParentObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) {
			// Note supported by editor
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
