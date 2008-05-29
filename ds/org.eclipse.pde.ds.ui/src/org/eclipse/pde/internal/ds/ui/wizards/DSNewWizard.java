/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

public class DSNewWizard extends BasicNewFileResourceWizard {
	protected DSFileWizardPage fMainPage;

	/**
	 * 
	 */
	public DSNewWizard() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#addPages()
	 */
	public void addPages() {
		 fMainPage = new DSFileWizardPage(getSelection());
		 addPage(fMainPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard#performFinish()
	 */
	public boolean performFinish() {
		try {

			IRunnableWithProgress op = new DSCreationOperation(fMainPage
					.createNewFile());

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
