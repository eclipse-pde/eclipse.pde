/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.ui.internal.wizards.CompareToBaselineWizard;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

public class CompareWithAction implements IObjectActionDelegate {

	private IWorkbenchPartSite workbenchPartSite;
	private ISelection selection = null;
	
	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		workbenchPartSite = targetPart.getSite();
	}
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (this.selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection=(IStructuredSelection) this.selection;
			CompareToBaselineWizard wizard = new CompareToBaselineWizard(structuredSelection, ActionMessages.CompareDialogTitle);
			WizardDialog wdialog = new WizardDialog(workbenchPartSite.getShell(), wizard);
			wdialog.open();
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}
