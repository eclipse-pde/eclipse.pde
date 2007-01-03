/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.update;

import java.io.File;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IProvisionerWizard;

public class UpdateSiteProvisionerWizard extends Wizard implements
IProvisionerWizard {

	private File[] fDirs = null;
	private UpdateSitePage fPage;

	public UpdateSiteProvisionerWizard() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.UpdateSiteProvisionerWizard_title); 
	}

	public void addPages() {
		fPage = new UpdateSitePage("update site"); //$NON-NLS-1$
		addPage(fPage);
		super.addPages();
	}

	public boolean performFinish() {
		fDirs = fPage.getLocations();
		return true;
	}

	public File[] getLocations() {
		return fDirs;
	}

}
