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
package org.eclipse.pde.internal.ui.wizards.project;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;

public class OpenProjectWizardAction extends Action {
	/**
	 * Constructor for OpenProjectWizardAction.
	 */
	public OpenProjectWizardAction() {
		super("OpenProject");
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run() {
		NewProjectWizard wizard = new NewProjectWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		dialog.open();
	}
}
