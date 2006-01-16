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
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;

public class OpenTargetProfileAction extends Action {

	private ITargetModel fTargetModel;
	private Shell fShell;

	public OpenTargetProfileAction(Shell shell, ITargetModel model) {
		fShell = shell;
		fTargetModel = model;
	}
	
	public void run() {
		if (fTargetModel == null) {
			MessageDialog.openError(fShell, PDEUIMessages.OpenTargetProfileAction_title, PDEUIMessages.OpenTargetProfileAction_missingProfile);
			return;
		}
		
		if (!fTargetModel.isLoaded()) {
			MessageDialog.openError(fShell, PDEUIMessages.OpenTargetProfileAction_title, PDEUIMessages.OpenTargetProfileAction_invalidProfile);
			return;
		}
		
		ApplicationWindow appWindow = new TargetProfileWindow(fShell, fTargetModel);
		appWindow.create();
		appWindow.open();		
	}

}
