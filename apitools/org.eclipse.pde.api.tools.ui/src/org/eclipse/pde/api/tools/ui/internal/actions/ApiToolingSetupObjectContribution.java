/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiToolingSetupRefactoring;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiToolingSetupWizard;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This class provides an object contribution for {@link IJavaProject}s to update the javadoc tags of
 * contained source files based on the component.xml file in the project.
 * 
 * If there is no component.xml file no work is done.
 * 
 * @see ApiDescriptionProcessor
 * 
 * @since 1.0.0
 */
public class ApiToolingSetupObjectContribution implements IObjectActionDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		//TODO create changes
		ApiToolingSetupWizard wizard = new ApiToolingSetupWizard(new ApiToolingSetupRefactoring());
		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
		try {
			op.run(ApiUIPlugin.getShell(), ActionMessages.ApiToolingSetupObjectContribution_0);
		}
		catch(InterruptedException ie) {
			ApiUIPlugin.log(ie);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {}

}
