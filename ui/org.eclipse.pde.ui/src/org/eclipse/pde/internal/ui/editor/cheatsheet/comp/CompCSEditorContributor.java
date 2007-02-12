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

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp;

import org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;

/**
 * CompCSEditorContributor
 *
 */
public class CompCSEditorContributor extends PDEFormEditorContributor {

	/**
	 * @param menuName
	 */
	public CompCSEditorContributor() {
		super("compCSEditor"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditorContributor#setActivePage(org.eclipse.ui.IEditorPart)
	 */
	public void setActivePage(IEditorPart newEditor) {
		super.setActivePage(newEditor);

		registerGlobalActionHandlers();
	}
	
	/**
	 * 
	 */
	private void registerGlobalActionHandlers() {
		// Register the revert action
		getActionBars().setGlobalActionHandler(ActionFactory.REVERT.getId(), 
				getRevertAction());		
	}
		
	
}
