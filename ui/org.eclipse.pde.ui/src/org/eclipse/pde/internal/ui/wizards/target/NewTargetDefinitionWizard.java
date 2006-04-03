/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewTargetDefinitionWizard extends BasicNewResourceWizard {
	
	TargetDefinitionWizardPage fPage;
	
	public void addPages() {
		fPage = new TargetDefinitionWizardPage("profile", getSelection()); //$NON-NLS-1$
		addPage(fPage);
	}

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
	
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle(PDEUIMessages.NewTargetProfileWizard_title); 
		setNeedsProgressMonitor(true);
	}
	
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}
	
	private BaseTargetDefinitionOperation getOperation() {
		int option = fPage.getInitializationOption();
		if (option == TargetDefinitionWizardPage.USE_DEFAULT)
			return new BaseTargetDefinitionOperation(fPage.createNewFile());
		else if (option == TargetDefinitionWizardPage.USE_CURRENT_TP)
			return new TargetDefinitionFromPlatformOperation(fPage.createNewFile());
		return new TargetDefinitionFromTargetOperation(fPage.createNewFile(), fPage.getTargetId());
	}

}
