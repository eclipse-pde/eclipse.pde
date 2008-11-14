/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.ui.*;

public class GetNonExternalizedStringsAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	//Azure: To indicate that only selected plug-ins are to be externalized. False by default.
	private boolean fExternalizeSelectedPluginsOnly = false;

	//Azure: To indicate that the post-externalization message dialog should not be displayed.
	private boolean fSkipMessageDialog = false;

	public GetNonExternalizedStringsAction() {
	}

	public void run(IAction action) {
		/* 
		 * Azure: Pass <code>fExternalizeSelectedPluginsOnly</code> to the operation to indicate
		 * that only the plug-ins passed in the selection are to be externalized and such that
		 * only those are displayed on the change table in the ExternalizeStringsWizard.
		 */
		GetNonExternalizedStringsOperation runnable = new GetNonExternalizedStringsOperation(fSelection, fExternalizeSelectedPluginsOnly);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			if (runnable.wasCanceled())
				return;
			ModelChangeTable changeTable = runnable.getChangeTable();
			if (!changeTable.isEmpty()) {
				ExternalizeStringsProcessor processor = new ExternalizeStringsProcessor();
				PDERefactor refactor = new PDERefactor(processor);
				ExternalizeStringsWizard wizard = new ExternalizeStringsWizard(changeTable, refactor);
				RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);

				try {
					op.run(PDEPlugin.getActiveWorkbenchShell(), ""); //$NON-NLS-1$
				} catch (final InterruptedException irex) {
				}
			} else {
				/* 
				 * Azure: When the InternationalizeAction invokes the ExternalizeStringsAction,
				 * <code>fSkipMessageDialog</code> is set to true in order for no intermediate
				 * message to appear if all selected plug-ins were already externalized.
				 */
				if (!fSkipMessageDialog)
					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), PDEUIMessages.GetNonExternalizedStringsAction_allExternalizedTitle, PDEUIMessages.GetNonExternalizedStringsAction_allExternalizedMessage);
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	/**
	 * TODO: Azure Documentation
	 * @param externalizeSelectedPluginsOnly
	 */
	public void setExternalizeSelectedPluginsOnly(boolean externalizeSelectedPluginsOnly) {
		fExternalizeSelectedPluginsOnly = externalizeSelectedPluginsOnly;
	}

	public boolean isExternalizeSelectedPluginsOnly() {
		return fExternalizeSelectedPluginsOnly;
	}

	public void setSkipMessageDialog(boolean skipMessageDialog) {
		this.fSkipMessageDialog = skipMessageDialog;
	}

	public boolean isSkipMessageDialog() {
		return fSkipMessageDialog;
	}

}
