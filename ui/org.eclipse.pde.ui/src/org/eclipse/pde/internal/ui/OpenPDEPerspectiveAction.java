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
package org.eclipse.pde.internal.ui;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.*;

public class OpenPDEPerspectiveAction extends Action {
	public OpenPDEPerspectiveAction() {
	}

	public void run() {
		IWorkbenchWindow window = PDEPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IAdaptable input;
		if (page != null)
			input = page.getInput();
		else
			input = ResourcesPlugin.getWorkspace().getRoot();
		try {
			PlatformUI.getWorkbench().showPerspective(
				"org.eclipse.pde.ui.PDEPerspective",
				window,
				input);
			notifyResult(true);
			
		} catch (WorkbenchException e) {
			PDEPlugin.logException(e);
			notifyResult(false);
		}
	}
}
