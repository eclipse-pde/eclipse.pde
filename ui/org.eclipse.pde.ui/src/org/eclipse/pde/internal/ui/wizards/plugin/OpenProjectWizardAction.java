package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.ui.*;
import org.eclipse.ui.cheatsheets.*;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;

/**
 * @author melhem
 *
 */
public class OpenProjectWizardAction extends Action implements ICheatSheetAction {
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
		int result = dialog.open();
		// TODO need to notify result here
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.cheatsheets.ICheatSheetAction#run(java.lang.String[], org.eclipse.ui.cheatsheets.ICheatSheetManager)
	 */
	public void run(String[] params, ICheatSheetManager manager) {
		// TODO need to initialize the wizard with default
		// values so that it comes preset
		run();
	}
}