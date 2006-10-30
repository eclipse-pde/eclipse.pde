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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractAddAction;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSAddStepAction extends CSAbstractAddAction {

	private ISimpleCS fCheatsheet;
	
	/**
	 * 
	 */
	public SimpleCSAddStepAction() {
		setText(PDEUIMessages.SimpleCSAddStepAction_0);
	}

	/**
	 * @param cheatsheet
	 */
	public void setSimpleCS(ISimpleCS cheatsheet) {
		fCheatsheet = cheatsheet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		ISimpleCSModelFactory factory = fCheatsheet.getModel().getFactory();

		// Element: item
		ISimpleCSItem item = factory.createSimpleCSItem(fCheatsheet);
		item.setTitle(generateItemTitle(PDEUIMessages.SimpleCheatSheetCreationOperation_1));
		// Element: description
		ISimpleCSDescription description = factory.createSimpleCSDescription(item);
		description.setContent(PDEUIMessages.SimpleCheatSheetCreationOperation_2);
		item.setDescription(description);		
		// TODO: MP: Can configure to add at a specific index
		fCheatsheet.addItem(item);
	}
	
	/**
	 * @return
	 */
	private String generateItemTitle(String base) {
		StringBuffer result = new StringBuffer(base);
		ISimpleCSItem[] items = fCheatsheet.getItems();
		// Used to track auto-generated numbers used
		HashSet set = new HashSet();

		// Linear search O(n).  
		// Performance hit unnoticeable because number of items per cheatsheet
		// should be minimal.
		for (int i = 0; i < items.length; i++) {
			ISimpleCSItem item = items[i];
			compareTitleWithBase(base, set, item.getTitle());
		}
		// Add an auto-generated number
		addNumberToBase(result, set);
		
		return result.toString();
	}


	
}
