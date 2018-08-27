/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

public class NewCSFileWizard extends BasicNewFileResourceWizard {

	protected CSFileWizardPage fMainPage;

	public NewCSFileWizard() {
		super();
	}

	@Override
	public void addPages() {
		fMainPage = new CSFileWizardPage("cheatsheet", getSelection()); //$NON-NLS-1$
		addPage(fMainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(CSWizardMessages.NewCSFileWizard_title);
		setNeedsProgressMonitor(true);
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_CHEATSHEET_WIZ);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(false, true, getOperation());
		} catch (InvocationTargetException e) {
			PDEUserAssistanceUIPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	private IRunnableWithProgress getOperation() {

		IFile file = fMainPage.createNewFile();
		int option = fMainPage.getCheatSheetType();
		if (option == CSFileWizardPage.F_SIMPLE_CHEAT_SHEET) {
			return new SimpleCSCreationOperation(file);
		} else if (option == CSFileWizardPage.F_COMPOSITE_CHEAT_SHEET) {
			return new CompCSCreationOperation(file);
		}
		return null;
	}
}
