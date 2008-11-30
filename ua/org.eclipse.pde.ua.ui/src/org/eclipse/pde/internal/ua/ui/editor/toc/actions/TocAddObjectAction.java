/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.core.toc.text.TocTopic;

/**
 * TocAddObjectAction - the abstract implementation for
 * adding objects to a TOC object.
 */
public abstract class TocAddObjectAction extends Action {
	//The parent TOC object, which the new object will be
	//a child of.
	TocObject fParentObject;

	//The target object to insert after
	TocObject fTargetObject;

	/**
	 * Set the parent object that this action will add
	 * objects to.
	 * 
	 * @param parent The new parent object for this action
	 */
	public void setParentObject(TocObject parent) {
		fParentObject = parent;
	}

	/**
	 * Set the target object that this action will add
	 * objects after.
	 * 
	 * @param target The new target object for this action
	 */
	public void setTargetObject(TocObject target) {
		fTargetObject = target;
	}

	/**
	 * @return The names of the children of this TOC object
	 */
	public String[] getChildNames() {
		int numChildren = fParentObject.getChildren().size();
		TocObject[] tocObjects = (TocObject[]) fParentObject.getChildren().toArray(new TocObject[numChildren]);

		String[] tocObjectNames = new String[tocObjects.length];

		for (int i = 0; i < numChildren; ++i) {
			tocObjectNames[i] = tocObjects[i].getName();
		}

		return tocObjectNames;
	}

	/**
	 * Add the child to the parent object. If a target object is specified,
	 * add the child as a direct sibling after that object.
	 * 
	 * @param child The object to add to the parent
	 */
	protected void addChild(TocObject child) {
		if (fTargetObject == null) {
			((TocTopic) fParentObject).addChild(child);
		} else {
			((TocTopic) fParentObject).addChild(child, fTargetObject, false);
			fTargetObject = null;
		}
	}
}
