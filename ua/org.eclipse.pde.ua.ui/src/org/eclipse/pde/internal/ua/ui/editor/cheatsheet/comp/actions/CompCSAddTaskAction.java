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

import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.ui.wizards.cheatsheet.CompCSCreationOperation;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class CompCSAddTaskAction extends CompCSAbstractAddAction {

	public CompCSAddTaskAction() {
		setText(ActionsMessages.CompCSAddTaskAction_task);
	}

	@Override
	public void run() {

		if (fParentObject == null) {
			return;
		}
		ICompCSTask task = CompCSCreationOperation.createBasicTask(fParentObject);
		// Set on the proper parent object
		if (fParentObject.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			ICompCSTaskGroup parent = (ICompCSTaskGroup) fParentObject;

			String name = PDELabelUtility.generateName(getTaskObjectNames(parent), ActionsMessages.CompCSAddTaskAction_task);
			task.setFieldName(name);
			parent.addFieldTaskObject(task);
		} else if (fParentObject.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			// Not supported by editor
		}
	}

}
