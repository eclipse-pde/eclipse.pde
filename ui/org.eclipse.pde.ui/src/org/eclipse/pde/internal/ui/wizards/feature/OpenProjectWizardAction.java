package org.eclipse.pde.internal.ui.wizards.feature;

import java.util.Hashtable;
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
		super("OpenProject"); //$NON-NLS-1$
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
			defValues.put(NewFeatureProjectWizard.DEF_PROJECT_NAME, params[0]);
		if (params.length>1)
			defValues.put(NewFeatureProjectWizard.DEF_ID, params[1]);
		if (params.length>2)
			defValues.put(NewFeatureProjectWizard.DEF_NAME, params[2]);
		NewFeatureProjectWizard wizard = new NewFeatureProjectWizard();
		wizard.init(defValues);
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		int result = dialog.open();
		notifyResult(result==WizardDialog.OK);		
	}
}