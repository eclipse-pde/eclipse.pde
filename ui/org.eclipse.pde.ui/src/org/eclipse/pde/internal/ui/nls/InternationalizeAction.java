/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.*;

/**
 * This action class is responsible for creating and initializing the
 * InternationalizeWizard.
 * 
 * @author Team Azure
 *
 */
public class InternationalizeAction implements IWorkbenchWindowActionDelegate {

	private IStructuredSelection fSelection;

	public InternationalizeAction() {
	}

	public void run(IAction action) {
		//Create an InternationalizeOperation on the workbench selection.
		InternationalizeOperation runnable = new InternationalizeOperation(fSelection);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			if (runnable.wasCanceled()) {
				return;
			}

			/*	Get the plugin model table containing the list of workspace and 
			 * 	external plug-ins
			 */
			InternationalizeModelTable pluginTable = runnable.getPluginTable();

			if (!pluginTable.isEmpty()) {

				InternationalizeWizard wizard = new InternationalizeWizard(action, pluginTable);
				wizard.init(PlatformUI.getWorkbench(), fSelection);

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
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = (IStructuredSelection) selection;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}
}
