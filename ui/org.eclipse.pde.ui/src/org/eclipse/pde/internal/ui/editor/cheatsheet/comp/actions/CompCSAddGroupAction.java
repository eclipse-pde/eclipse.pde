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

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp.actions;

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
import org.eclipse.pde.internal.ui.wizards.cheatsheet.CompCSCreationOperation;


/**
 * CompCSAddTaskAction
 *
 */
public class CompCSAddGroupAction extends CompCSAbstractAddAction {

	/**
	 * 
	 */
	public CompCSAddGroupAction() {
		setText(PDEUIMessages.CompCSCreationOperation_group);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		
		if (fParentObject == null) {
			return;
		}
		ICompCSTaskGroup group = 
			CompCSCreationOperation.createBasicGroup(fParentObject);
		// Set on the proper parent object
		if (fParentObject.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			ICompCSTaskGroup parent = (ICompCSTaskGroup)fParentObject;

			String name = PDELabelUtility.generateName(getTaskObjectNames(parent), PDEUIMessages.CompCSCreationOperation_group);
			group.setFieldName(name);
			parent.addFieldTaskObject(group);
		} else if (fParentObject.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			// Not supported by editor
		} 
	}	
	
}
