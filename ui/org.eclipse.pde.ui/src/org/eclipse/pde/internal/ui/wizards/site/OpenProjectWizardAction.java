package org.eclipse.pde.internal.ui.wizards.site;

import java.util.Hashtable;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.ui.*;
import org.eclipse.ui.cheatsheets.*;

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
		run(new String [] {}, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.cheatsheets.ICheatSheetAction#run(java.lang.String[], org.eclipse.ui.cheatsheets.ICheatSheetManager)
	 */
	public void run(String[] params, ICheatSheetManager manager) {
		Hashtable defValues = new Hashtable();
		if (params.length>0)
			defValues.put(NewSiteProjectWizard.DEF_PROJECT_NAME, params[0]);
		NewSiteProjectWizard wizard = new NewSiteProjectWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		wizard.init(defValues);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		int result = dialog.open();
		notifyResult(result==WizardDialog.OK);
	}
}