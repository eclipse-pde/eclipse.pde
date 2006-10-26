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

import java.util.HashSet;

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractAddAction;

/**
 * SimpleCSAddStepAction
 *
 */
public abstract class CompCSAbstractAddAction extends CSAbstractAddAction {

	protected ICompCSObject fParentObject;
	
	/**
	 * 
	 */
	public CompCSAbstractAddAction() {
		// NO-OP
	}

	/**
	 * @param cheatsheet
	 */
	public void setParentObject(ICompCSObject object) {
		fParentObject = object;
	}
	
	/**
	 * @return
	 */
	protected String generateTaskObjectName(ICompCSTaskGroup parent, String base) {
		StringBuffer result = new StringBuffer(base);
		ICompCSTaskObject[] taskObjects = parent.getFieldTaskObjects();
		// Used to track auto-generated numbers used
		HashSet set = new HashSet();

		// Linear search O(n).  
		// Performance hit unnoticeable because number of items per cheatsheet
		// should be minimal.
		for (int i = 0; i < taskObjects.length; i++) {
			compareTitleWithBase(base, set, taskObjects[i].getFieldName());
		}
		// Add an auto-generated number
		addNumberToBase(result, set);
		
		return result.toString();
	}	
}
