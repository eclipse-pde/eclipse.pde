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
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * New target definition file wizard used to create a new target definition file from
 * the new target platform preference page.
 */
public class NewTargetDefinitionFileWizard extends BasicNewResourceWizard {

	NewTargetDefnitionFileWizardPage fPage;
	IPath fPath;

	public NewTargetDefinitionFileWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
		setWindowTitle(PDEUIMessages.NewTargetProfileWizard_title);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		fPage = new NewTargetDefnitionFileWizardPage("New Target Definition", StructuredSelection.EMPTY); //$NON-NLS-1$
		addPage(fPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		fPath = fPage.getContainerFullPath().append(fPage.getFileName());
		return true;
	}

	/**
	 * @return Path of the new file
	 */
	public IPath getTargetFileLocation() {
		return fPath;
	}
}
