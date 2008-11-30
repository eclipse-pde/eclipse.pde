/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject;

/**
 * Abstract action that allows new nodes to be added to the context help
 * model.
 * @since 3.4
 */
public abstract class CtxHelpAbstractAddAction extends Action {

	protected CtxHelpObject fParentObject;

	//The target object to insert after
	protected CtxHelpObject fTargetObject;

	/**
	 * Set the parent object that this action will add
	 * objects to.
	 * 
	 * @param parent The new parent object for this action
	 */
	public void setParentObject(CtxHelpObject parent) {
		fParentObject = parent;
	}

	/**
	 * Set the target object that this action will add
	 * objects after.
	 * 
	 * @param target The new target object for this action
	 */
	public void setTargetObject(CtxHelpObject target) {
		fTargetObject = target;
	}

	/**
	 * Returns the names of the children of the parent object.  Used
	 * to find a new name that doesn't conflict.  Will return an empty
	 * array if the parent is not set.
	 * @return children names or an empty array
	 */
	public String[] getChildNames() {
		if (fParentObject != null) {
			int numChildren = fParentObject.getChildren().size();
			CtxHelpObject[] ctxHelpObjects = (CtxHelpObject[]) fParentObject.getChildren().toArray(new CtxHelpObject[numChildren]);

			String[] ctxHelpObjectNames = new String[ctxHelpObjects.length];

			for (int i = 0; i < numChildren; ++i) {
				ctxHelpObjectNames[i] = ctxHelpObjects[i].getName();
			}
			return ctxHelpObjectNames;
		}
		return new String[0];
	}

	/**
	 * Add the child to the parent object. If a target object is specified,
	 * add the child as a sibling after that object.
	 * 
	 * @param child The object to add to the parent
	 */
	protected void addChild(CtxHelpObject child) {
		if (fTargetObject == null) {
			fParentObject.addChild(child);
		} else {
			fParentObject.addChild(child, fTargetObject, false);
		}
	}
}
