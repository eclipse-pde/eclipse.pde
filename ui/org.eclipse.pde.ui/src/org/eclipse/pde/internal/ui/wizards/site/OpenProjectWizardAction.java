/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.site;

import java.util.Hashtable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

public class OpenProjectWizardAction extends Action implements ICheatSheetAction {
	public OpenProjectWizardAction() {
		super(PDEUIMessages.Actions_Site_OpenProjectWizardAction);
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run() {
		run(new String[] {}, null);
	}

	@Override
	public void run(String[] params, ICheatSheetManager manager) {
		Hashtable<String, String> defValues = new Hashtable<>();
		if (params.length > 0)
			defValues.put(NewSiteProjectWizard.DEF_PROJECT_NAME, params[0]);
		NewSiteProjectWizard wizard = new NewSiteProjectWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		wizard.init(defValues);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		int result = dialog.open();
		notifyResult(result == Window.OK);
	}
}
