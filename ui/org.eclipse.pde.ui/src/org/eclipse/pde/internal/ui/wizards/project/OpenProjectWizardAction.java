/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.project;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;

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
		dialog.getShell().setSize(500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		dialog.open();
	}
}
