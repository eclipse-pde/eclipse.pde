/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.pde.internal.ui.editor.*;

public class BuildEditorContributor extends PDEEditorContributor {

	public BuildEditorContributor() {
		super("Plug-in Jars");
	}
	public BuildEditorContributor(String menuName) {
		super(menuName);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEEditorContributor#contextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	public void contextMenuAboutToShow(IMenuManager mng) {
	}
}
