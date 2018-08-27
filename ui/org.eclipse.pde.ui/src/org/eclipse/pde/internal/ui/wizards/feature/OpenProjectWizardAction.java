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
package org.eclipse.pde.internal.ui.wizards.feature;

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
		super(PDEUIMessages.Actions_Feature_OpenProjectWizardAction);
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
			defValues.put(AbstractNewFeatureWizard.DEF_PROJECT_NAME, params[0]);
		if (params.length > 1)
			defValues.put(AbstractNewFeatureWizard.DEF_FEATURE_ID, params[1]);
		if (params.length > 2)
			defValues.put(AbstractNewFeatureWizard.DEF_FEATURE_NAME, params[2]);
		NewFeatureProjectWizard wizard = new NewFeatureProjectWizard();
		wizard.init(defValues);
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 500, 500);
		dialog.getShell().setText(wizard.getWindowTitle());
		int result = dialog.open();
		notifyResult(result == Window.OK);
	}
}
