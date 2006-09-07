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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * SimpleCSAddStepAction
 *
 */
public class SimpleCSAddSubStepAction extends Action {

	private ISimpleCSObject fObject;
	
	/**
	 * 
	 */
	public SimpleCSAddSubStepAction() {
		// TODO: MP: Update
		setText(PDEUIMessages.SimpleCSAddSubStepAction_0);
//		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
//		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
	}

	/**
	 * @param cheatsheet
	 */
	public void setObject(ISimpleCSObject object) {
		fObject = object;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		
		if (fObject == null) {
			return;
		}
		
		ISimpleCSModelFactory factory = fObject.getModel().getFactory();
		
		// Element: subitem
		ISimpleCSSubItem subitem = factory.createSimpleCSSubItem(fObject);
		subitem.setLabel(PDEUIMessages.SimpleCSAddSubStepAction_1);
		// Set on the proper parent object
		if (fObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			((ISimpleCSItem)fObject).addSubItem(subitem);
		} else if (fObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) {
			// TODO: MP: Do for conditional subitem
		} else if (fObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) {
			// TODO: MP: Do for repeated subitem
		}
		
	}
}
