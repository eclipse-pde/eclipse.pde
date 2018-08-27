/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.ui.wizards.ctxhelp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

/**
 * Wizard to create a new context help file.
 * @since 3.4
 */
public class NewCtxHelpWizard extends BasicNewFileResourceWizard {

	protected NewCtxHelpWizardPage fMainPage;

	@Override
	public void addPages() {
		fMainPage = new NewCtxHelpWizardPage("new context help", getSelection()); //$NON-NLS-1$
		addPage(fMainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(CtxWizardMessages.NewCtxHelpWizard_title);
		setNeedsProgressMonitor(true);
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		// setDefaultPageImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_TARGET_WIZ);
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

	/**
	 * @return the operation to execute on finish
	 */
	private IRunnableWithProgress getOperation() {
		IFile file = fMainPage.createNewFile();
		return new NewCtxHelpOperation(file);
	}

}
