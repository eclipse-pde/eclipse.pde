/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Team Azure - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.commands.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * This action class is responsible for creating and initializing the
 * InternationalizeWizard.
 */
public class InternationalizeAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//Create an InternationalizeOperation on the workbench selection.
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}
		InternationalizeOperation runnable = new InternationalizeOperation(selection);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			if (runnable.wasCanceled()) {
				return null;
			}

			/*	Get the plugin model table containing the list of workspace and
			 * 	external plug-ins
			 */
			InternationalizeModelTable pluginTable = runnable.getPluginTable();

			if (!pluginTable.isEmpty()) {

				InternationalizeWizard wizard = new InternationalizeWizard(pluginTable);
				wizard.init(PlatformUI.getWorkbench(), (IStructuredSelection) selection);

				//Create an operation to start and run the wizard
				InternationalizeWizardOpenOperation op = new InternationalizeWizardOpenOperation(wizard);
				try {
					op.run(PDEPlugin.getActiveWorkbenchShell(), ""); //$NON-NLS-1$
				} catch (final InterruptedException irex) {
				}
			} else {
				MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), PDEUIMessages.InternationalizeAction_internationalizeTitle, PDEUIMessages.InternationalizeAction_internationalizeMessage);
			}
		}
		return null;
	}
}
