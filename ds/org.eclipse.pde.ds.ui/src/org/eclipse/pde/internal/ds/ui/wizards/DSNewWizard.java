/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class DSNewWizard extends Wizard implements INewWizard {
	protected DSFileWizardPage fMainPage;

	public DSNewWizard() {
		super();
	}

	@Override
	public void addPages() {
		 addPage(fMainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		setWindowTitle(Messages.DSNewWizard_title);
		setDialogSettings(Activator.getDefault().getDialogSettings());
		fMainPage = new DSFileWizardPage(currentSelection);
	}

	@Override
	public boolean performFinish() {
		try {
			IDialogSettings settings = getDialogSettings();
			if (settings != null) {
				fMainPage.saveSettings(settings);
			}
			IRunnableWithProgress op = new DSCreationOperation(fMainPage
					.createNewFile(), fMainPage.getDSComponentNameValue(),
					fMainPage.getDSImplementationClassValue());

			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			Activator.logException(e, null, null);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
}
