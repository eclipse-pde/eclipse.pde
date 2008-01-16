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

import org.eclipse.pde.internal.core.icheatsheet.comp.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
import org.eclipse.pde.internal.ui.wizards.cheatsheet.CompCSCreationOperation;

/**
 * CompCSAddTaskAction
 *
 */
public class CompCSAddTaskAction extends CompCSAbstractAddAction {

	/**
	 * 
	 */
	public CompCSAddTaskAction() {
		setText(PDEUIMessages.CompCSCreationOperation_task);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {

		if (fParentObject == null) {
			return;
		}
		ICompCSTask task = CompCSCreationOperation.createBasicTask(fParentObject);
		// Set on the proper parent object
		if (fParentObject.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			ICompCSTaskGroup parent = (ICompCSTaskGroup) fParentObject;

			String name = PDELabelUtility.generateName(getTaskObjectNames(parent), PDEUIMessages.CompCSCreationOperation_task);
			task.setFieldName(name);
			parent.addFieldTaskObject(task);
		} else if (fParentObject.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			// Not supported by editor
		}
	}

}
