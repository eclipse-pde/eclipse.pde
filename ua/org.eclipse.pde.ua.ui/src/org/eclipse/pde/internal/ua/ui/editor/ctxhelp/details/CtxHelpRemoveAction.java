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
 * The action used to remove one or more elements from the context help xml.
 * @since 3.4
 */
public class CtxHelpRemoveAction extends Action {

	private CtxHelpObject[] fObjects;

	//The next object to be selected after the
	//selected object is removed
	private CtxHelpObject fObjectToSelect;

	public CtxHelpRemoveAction() {
		// Adds the 'Delete' keybinding to the action when displayed
		// in a context menu
		setActionDefinitionId("org.eclipse.ui.edit.delete"); //$NON-NLS-1$
		setText(CtxHelpDetailsMessages.CtxHelpRemoveAction_remove);
	}

	/**
	 * Sets the objects to be removed when this action is run.
	 * @param objects objects to remove
	 */
	public void setToRemove(CtxHelpObject[] objects) {
		fObjects = objects;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fObjects == null)
			return;

		for (int i = 0; i < fObjects.length; ++i) {
			if (fObjects[i] != null && fObjects[i].canBeRemoved()) {
				CtxHelpObject parent = fObjects[i].getParent();
				if (parent != null) {
					determineNextSelection(parent, i);
					parent.removeChild(fObjects[i]);
				}
			}
		}
	}

	/**
	 * Determine the next object that should be selected
	 * after the designated object has been removed
	 * 
	 * @param parent The parent of the deleted object
	 */
	private void determineNextSelection(CtxHelpObject parent, int index) {
		// Select the next sibling
		fObjectToSelect = parent.getNextSibling(fObjects[index]);
		if (fObjectToSelect == null) {
			// No next sibling
			// Select the previous sibling
			fObjectToSelect = parent.getPreviousSibling(fObjects[index]);
			if (fObjectToSelect == null) {
				// No previous sibling
				// Select the parent
				fObjectToSelect = parent;
			}
		}
	}

	/**
	 * Returns the object that should be selected after the action is run.
	 * @return the object to select or <code>null</code>
	 */
	public CtxHelpObject getNextSelection() {
		return fObjectToSelect;
	}

	/**
	 * Clears the next selection returned by {@link #getNextSelection()}.
	 */
	public void clearNextSelection() {
		fObjectToSelect = null;
	}

}
