/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler to run the organize manifests operation.
 *
 */
public class OrganizeManifestsAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		runOrganizeManfestsAction(HandlerUtil.getCurrentSelection(event));
		return null;
	}

	/**
	 * Runs the organize manifest operation for projects in the provided selection.
	 * Public to allow editors to call this action
	 *
	 * TODO This could be done better using the ICommandService
	 *
	 * @param selection selection to run organize manifest operation on
	 */
	public void runOrganizeManfestsAction(ISelection selection) {
		if (!PlatformUI.getWorkbench().saveAllEditors(true))
			return;

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Iterator<?> it = ssel.iterator();
			ArrayList<IProject> projects = new ArrayList<>();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile) element).getProject();
				else if (element instanceof IProject)
					proj = (IProject) element;
				else if (element instanceof IJavaProject) {
					proj = ((IJavaProject) element).getProject();
				}
				if (proj != null && PDEProject.getManifest(proj).exists())
					projects.add(proj);
			}
			if (!projects.isEmpty()) {
				OrganizeManifestsProcessor processor = new OrganizeManifestsProcessor(projects);
				PDERefactor refactor = new PDERefactor(processor);
				OrganizeManifestsWizard wizard = new OrganizeManifestsWizard(refactor);
				RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);

				try {
					op.run(PDEPlugin.getActiveWorkbenchShell(), ""); //$NON-NLS-1$
				} catch (final InterruptedException irex) {
				}
			} else
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.OrganizeManifestsWizardPage_title, PDEUIMessages.OrganizeManifestsWizardPage_errorMsg);
		}
	}
}
