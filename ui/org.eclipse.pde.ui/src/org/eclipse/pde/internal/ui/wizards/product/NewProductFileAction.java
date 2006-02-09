package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

public class NewProductFileAction extends Action implements ICheatSheetAction {
	
	public NewProductFileAction() {
		super("NewProductFile");  //$NON-NLS-1$
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
		NewProductFileWizard wizard = new NewProductFileWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		int result = dialog.open();
		notifyResult(result==WizardDialog.OK);
	}

}
