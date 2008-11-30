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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;

/**
 * SimpleCSAddStepAction
 */
public class CompCSRemoveTaskObjectAction extends Action {

	private ICompCSTaskObject fTaskObject;

	private ICompCSObject fObjectToSelect;

	/**
	 * 
	 */
	public CompCSRemoveTaskObjectAction() {
		setText(ActionsMessages.CompCSRemoveTaskObjectAction_delete);
		fTaskObject = null;
		fObjectToSelect = null;
	}

	/**
	 * @param subitem
	 */
	public void setTaskObject(ICompCSTaskObject taskObject) {
		fTaskObject = taskObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fTaskObject != null) {
			ICompCSObject parent = fTaskObject.getParent();
			if (parent.getType() == ICompCSConstants.TYPE_TASKGROUP) {
				// Parent is a group
				ICompCSTaskGroup group = (ICompCSTaskGroup) parent;
				// Determine the object to select after the deletion 
				// takes place 
				determineItemToSelect(group);
				// Remove the subitem
				group.removeFieldTaskObject(fTaskObject);
			}
		}
	}

	/**
	 * @param item
	 */
	private void determineItemToSelect(ICompCSTaskGroup group) {
		// Select the next sibling
		fObjectToSelect = group.getNextSibling(fTaskObject);
		if (fObjectToSelect == null) {
			// No next sibling
			// Select the previous sibling
			fObjectToSelect = group.getPreviousSibling(fTaskObject);
			if (fObjectToSelect == null) {
				// No previous sibling
				// Select the parent
				fObjectToSelect = group;
			}
		}
	}

	/**
	 * @return
	 */
	public ICompCSObject getObjectToSelect() {
		return fObjectToSelect;
	}

}
