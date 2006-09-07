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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSAddStepAction extends Action {

	private ISimpleCS fCheatsheet;
	
	/**
	 * 
	 */
	public SimpleCSAddStepAction() {
		// TODO: MP: Update
		setText(PDEUIMessages.SimpleCSAddStepAction_0);
//		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
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
		item.setTitle(PDEUIMessages.SimpleCheatSheetCreationOperation_1);
		// Element: description
		ISimpleCSDescription description = factory.createSimpleCSDescription(item);
		description.setContent(PDEUIMessages.SimpleCheatSheetCreationOperation_2);
		item.setDescription(description);		
		// TODO: MP: Can configure to add at a specific index
		fCheatsheet.addItem(item);
	}
}
