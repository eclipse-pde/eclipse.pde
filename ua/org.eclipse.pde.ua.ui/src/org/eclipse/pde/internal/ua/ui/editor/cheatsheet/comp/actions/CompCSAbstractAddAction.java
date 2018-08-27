/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;

public abstract class CompCSAbstractAddAction extends Action {

	protected ICompCSObject fParentObject;

	public CompCSAbstractAddAction() {
		// intentionally left empty
	}

	public void setParentObject(ICompCSObject object) {
		fParentObject = object;
	}

	protected String[] getTaskObjectNames(ICompCSTaskGroup parent) {
		ICompCSTaskObject[] taskObjects = parent.getFieldTaskObjects();
		String[] taskObjectNames = new String[taskObjects.length];
		for (int i = 0; i < taskObjects.length; ++i) {
			taskObjectNames[i] = taskObjects[i].getFieldName();
		}

		return taskObjectNames;
	}
}
