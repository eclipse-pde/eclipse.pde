/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
import org.eclipse.core.commands.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler to find translatable strings in projects
 */
public class GetNonExternalizedStringsAction extends AbstractHandler {

	/**
	 * To indicate that only selected plug-ins are to be externalized. False by default.
	 */
	private boolean fExternalizeSelectedPluginsOnly = false;

	/**
	 * To indicate that the post-externalization message dialog should not be displayed.
	 */
	private boolean fSkipMessageDialog = false;

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		/* 
		 * Pass <code>fExternalizeSelectedPluginsOnly</code> to the operation to indicate
		 * that only the plug-ins passed in the selection are to be externalized and such that
		 * only those are displayed on the change table in the ExternalizeStringsWizard.
		 */
		runGetNonExternalizedStringsAction(HandlerUtil.getCurrentSelection(event));
		return null;
	}

	/**
	 * Executes this action, opening the externalize strings wizard and performing the proper operations.
	 * Added to allow the editors and internationalize wizard to open the externalize wizard in case some
	 * strings have not been externalized beforehand.
	 * 
	 * @param selection The selection to run the action on
	 */
	public void runGetNonExternalizedStringsAction(ISelection selection) {
		GetNonExternalizedStringsOperation runnable = new GetNonExternalizedStringsOperation(selection, fExternalizeSelectedPluginsOnly);
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
