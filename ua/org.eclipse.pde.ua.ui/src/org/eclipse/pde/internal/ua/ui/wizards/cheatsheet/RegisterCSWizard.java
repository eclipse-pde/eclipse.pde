/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class RegisterCSWizard extends Wizard implements INewWizard {

	private RegisterCSWizardPage fMainPage;

	private IModel fModel;

	public RegisterCSWizard(IModel model) {
		fModel = model;
	}

	@Override
	public void addPages() {
		if (fModel instanceof ICompCSModel) {
			fMainPage = new RegisterCompCSWizardPage((ICompCSModel) fModel);
		} else if (fModel instanceof ISimpleCSModel) {
			fMainPage = new RegisterSimpleCSWizardPage((ISimpleCSModel) fModel);
		}

		addPage(fMainPage);
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
		return new RegisterCSOperation(fMainPage, getShell());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(CSWizardMessages.RegisterCSWizard_title);
		// TODO: MP: LOW: CompCS: New register cheat sheet wizard image
		setDefaultPageImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_CHEATSHEET_WIZ);
		setNeedsProgressMonitor(true);
	}

}
