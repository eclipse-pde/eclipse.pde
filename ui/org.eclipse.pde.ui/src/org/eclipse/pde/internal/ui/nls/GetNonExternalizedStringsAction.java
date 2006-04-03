/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class GetNonExternalizedStringsAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	
	public GetNonExternalizedStringsAction() {
	}

	public void run(IAction action) {
		GetNonExternalizedStringsOperation runnable = new GetNonExternalizedStringsOperation(fSelection);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			if (runnable.wasCanceled()) 
				return;
			ModelChangeTable changeTable = runnable.getChangeTable();
			if (!changeTable.isEmpty()) {
				ExternalizeStringsWizard wizard = new ExternalizeStringsWizard(changeTable);
				final WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
					public void run() {
						dialog.open();
					}
				});
			} else
				MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
						PDEUIMessages.GetNonExternalizedStringsAction_allExternalizedTitle, 
						PDEUIMessages.GetNonExternalizedStringsAction_allExternalizedMessage);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}
}
