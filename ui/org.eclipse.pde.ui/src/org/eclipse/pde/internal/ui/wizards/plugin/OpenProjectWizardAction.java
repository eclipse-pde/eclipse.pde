package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.ui.*;

/**
 * @author melhem
 *
 */
public class OpenProjectWizardAction extends Action {
	/**
	 * @param text
	 */
	public OpenProjectWizardAction() {
		super("OpenProject");
	}
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run() {
		NewPluginProjectWizard wizard = new NewPluginProjectWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		dialog.open();
	}

}
