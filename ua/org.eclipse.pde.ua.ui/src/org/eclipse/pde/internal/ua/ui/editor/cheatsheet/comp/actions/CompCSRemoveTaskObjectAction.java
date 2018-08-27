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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;

public class CompCSRemoveTaskObjectAction extends Action {

	private ICompCSTaskObject fTaskObject;

	private ICompCSObject fObjectToSelect;

	public CompCSRemoveTaskObjectAction() {
		setText(ActionsMessages.CompCSRemoveTaskObjectAction_delete);
		fTaskObject = null;
		fObjectToSelect = null;
	}

	public void setTaskObject(ICompCSTaskObject taskObject) {
		fTaskObject = taskObject;
	}

	@Override
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

	public ICompCSObject getObjectToSelect() {
		return fObjectToSelect;
	}

}
