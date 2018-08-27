/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.ui.forms.widgets.Form;

/**
 * CompCSTreeValidator
 *
 */
public class CompCSGroupValidator {

	private Form fForm;

	private HashSet<ICompCSTaskGroup> fGroups;

	private String fErrorCategory;

	public CompCSGroupValidator(ICompCS cheatsheet, Form form, String errorCategory) {
		fForm = form;
		fErrorCategory = errorCategory;

		fGroups = new HashSet<>();
		populateGroups(cheatsheet);
	}

	private void populateGroups(ICompCS cheatsheet) {
		// Register all existing groups in the present workspace model to be
		// validated
		if (cheatsheet.getFieldTaskObject().getType() == ICompCSConstants.TYPE_TASKGROUP) {
			addGroup((ICompCSTaskGroup) cheatsheet.getFieldTaskObject());
		}
	}

	public void addGroup(ICompCSTaskGroup group) {
		fGroups.add(group);
		// Check to see if the group has any children
		if (group.hasFieldTaskObjects() == false) {
			return;
		}
		// Recursively add any sub-groups
		ICompCSTaskObject[] taskObjects = group.getFieldTaskObjects();
		for (ICompCSTaskObject taskObject : taskObjects) {
			if (taskObject.getType() == ICompCSConstants.TYPE_TASKGROUP) {
				addGroup((ICompCSTaskGroup) taskObject);
			}
		}
	}

	public void removeGroup(ICompCSTaskGroup group) {
		fGroups.remove(group);
		// Check to see if the group has any children
		if (group.hasFieldTaskObjects() == false) {
			return;
		}
		// Recursively remove any sub-groups
		ICompCSTaskObject[] taskObjects = group.getFieldTaskObjects();
		for (ICompCSTaskObject taskObject : taskObjects) {
			if (taskObject.getType() == ICompCSConstants.TYPE_TASKGROUP) {
				removeGroup((ICompCSTaskGroup) taskObject);
			}
		}
	}

	public boolean validate() {
		// Check to see if there is anything to validate
		if (fGroups.isEmpty()) {
			fForm.setMessage(null);
			return true;
		}
		Iterator<ICompCSTaskGroup> iterator = fGroups.iterator();
		// Validate all registered groups
		while (iterator.hasNext()) {
			ICompCSTaskGroup group = iterator.next();
			if (validate(group) == false) {
				return false;
			}
		}
		fForm.setMessage(null);
		return true;
	}

	private boolean validate(ICompCSTaskGroup group) {
		if (group.getFieldTaskObjectCount() == 0) {
			String message = '[' + fErrorCategory + ']' + ' ' + PDETextHelper.translateReadText(group.getFieldName()) + ':' + ' ' + Messages.CompCSGroupValidator_errorChildlessGroup;
			fForm.setMessage(message, IMessageProvider.INFORMATION);
			return false;
		}
		return true;
	}

}
