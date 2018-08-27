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
package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewProductFileWizard extends BasicNewResourceWizard {

	private ProductFileWizardPage fMainPage;

	@Override
	public void addPages() {
		fMainPage = new ProductFileWizardPage("product", getSelection()); //$NON-NLS-1$
		addPage(fMainPage);
	}

	@Override
	public boolean performFinish() {
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

	private IRunnableWithProgress getOperation() {
		IFile file = fMainPage.createNewFile();
		int option = fMainPage.getInitializationOption();
		if (option == ProductFileWizardPage.USE_LAUNCH_CONFIG)
			return new ProductFromConfigOperation(file, fMainPage.getSelectedLaunchConfiguration());
		if (option == ProductFileWizardPage.USE_PRODUCT)
			return new ProductFromExtensionOperation(file, fMainPage.getSelectedProduct());
		return new BaseProductCreationOperation(file);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(PDEUIMessages.NewProductFileWizard_windowTitle);
		setNeedsProgressMonitor(true);
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PRODUCT_WIZ);
	}

}
