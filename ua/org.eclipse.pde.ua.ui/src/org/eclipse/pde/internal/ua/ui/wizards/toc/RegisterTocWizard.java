/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.wizards.toc;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class RegisterTocWizard extends Wizard implements INewWizard {

	private RegisterTocWizardPage fMainPage;

	private IModel fModel;

	public RegisterTocWizard(IModel model) {
		fModel = model;
	}

	@Override
	public void addPages() {
		fMainPage = new RegisterTocWizardPage(fModel);
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
		return new RegisterTocOperation(fMainPage, getShell());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(TocWizardMessages.RegisterTocWizard_link);
		// TODO: MP: LOW: TOC: New register table of contents wizard image
		setDefaultPageImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_CHEATSHEET_WIZ);
		setNeedsProgressMonitor(true);
	}

}
