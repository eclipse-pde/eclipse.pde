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

	private HashSet fGroups;

	private String fErrorCategory;

	// TODO: MP: LOW: CompCS: Can augment the model to have isValid() methods to simplify validation

	/**
	 * 
	 */
	public CompCSGroupValidator(ICompCS cheatsheet, Form form, String errorCategory) {
		fForm = form;
		fErrorCategory = errorCategory;

		fGroups = new HashSet();
		populateGroups(cheatsheet);
	}

	/**
	 * @param cheatsheet
	 */
	private void populateGroups(ICompCS cheatsheet) {
		// Register all existing groups in the present workspace model to be
		// validated
		if (cheatsheet.getFieldTaskObject().getType() == ICompCSConstants.TYPE_TASKGROUP) {
			addGroup((ICompCSTaskGroup) cheatsheet.getFieldTaskObject());
		}
	}

	/**
	 * @param group
	 */
	public void addGroup(ICompCSTaskGroup group) {
		fGroups.add(group);
		// Check to see if the group has any children
		if (group.hasFieldTaskObjects() == false) {
			return;
		}
		// Recursively add any sub-groups
		ICompCSTaskObject[] taskObjects = group.getFieldTaskObjects();
		for (int i = 0; i < taskObjects.length; i++) {
			if (taskObjects[i].getType() == ICompCSConstants.TYPE_TASKGROUP) {
				addGroup((ICompCSTaskGroup) taskObjects[i]);
			}
		}
	}

	/**
	 * @param group
	 */
	public void removeGroup(ICompCSTaskGroup group) {
		fGroups.remove(group);
		// Check to see if the group has any children
		if (group.hasFieldTaskObjects() == false) {
			return;
		}
		// Recursively remove any sub-groups
		ICompCSTaskObject[] taskObjects = group.getFieldTaskObjects();
		for (int i = 0; i < taskObjects.length; i++) {
			if (taskObjects[i].getType() == ICompCSConstants.TYPE_TASKGROUP) {
				removeGroup((ICompCSTaskGroup) taskObjects[i]);
			}
		}
	}

	/**
	 * @return
	 */
	public boolean validate() {
		// Check to see if there is anything to validate
		if (fGroups.isEmpty()) {
			fForm.setMessage(null);
			return true;
		}
		Iterator iterator = fGroups.iterator();
		// Validate all registered groups
		while (iterator.hasNext()) {
			ICompCSTaskGroup group = (ICompCSTaskGroup) iterator.next();
			if (validate(group) == false) {
				return false;
			}
		}
		fForm.setMessage(null);
		return true;
	}

	/**
	 * @param group
	 * @return
	 */
	private boolean validate(ICompCSTaskGroup group) {
		if (group.getFieldTaskObjectCount() == 0) {
			String message = '[' + fErrorCategory + ']' + ' ' + PDETextHelper.translateReadText(group.getFieldName()) + ':' + ' ' + Messages.CompCSGroupValidator_errorChildlessGroup;
			fForm.setMessage(message, IMessageProvider.INFORMATION);
			return false;
		}
		return true;
	}

}
