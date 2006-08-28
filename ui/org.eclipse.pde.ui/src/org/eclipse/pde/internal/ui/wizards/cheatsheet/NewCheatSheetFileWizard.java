/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.cheatsheet;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

/**
 * NewCheatSheetWizard
 *
 */
public class NewCheatSheetFileWizard extends BasicNewFileResourceWizard implements INewWizard {

	private CheatSheetFileWizardPage fMainPage;
	
	/**
	 * 
	 */
	public NewCheatSheetFileWizard() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#addPages()
	 */
	public void addPages() {
		fMainPage = new CheatSheetFileWizardPage("cheatsheet", getSelection()); //$NON-NLS-1$
		addPage(fMainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        super.init(workbench, currentSelection);
        setWindowTitle(PDEUIMessages.NewCheatSheetFileWizard_0);
        setNeedsProgressMonitor(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#initializeDefaultPageImageDescriptor()
	 */
	protected void initializeDefaultPageImageDescriptor() {
		// TODO: MP: Update with proper image
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PRODUCT_WIZ);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#performFinish()
	 */
	public boolean performFinish() {
		// Post-process page data
		fMainPage.finalizePage();
		try {
			getContainer().run(false, true, getOperation());
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * @return
	 */
	private IRunnableWithProgress getOperation() {
        IFile file = fMainPage.createNewFile();
		int option = fMainPage.getCheatSheetType();
		if (option == CheatSheetFileWizardPage.F_SIMPLE_CHEAT_SHEET) {
			return new SimpleCheatSheetCreationOperation(file);
		} else if (option == CheatSheetFileWizardPage.F_COMPOSITE_CHEAT_SHEET) {
			// TODO: MP: Do specific operation for composite cheat sheet
			return null;
		}
		// This should never happen
		// TODO: MP: Externalize
		PDEPlugin.logErrorMessage("Unknown cheat sheet type encountered"); //$NON-NLS-1$
		return null;
		//return new BaseCheatSheetCreationOperation(file);
	}	
}
