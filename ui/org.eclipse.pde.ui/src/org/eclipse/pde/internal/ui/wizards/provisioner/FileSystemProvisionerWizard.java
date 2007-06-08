/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.IProvisionerWizard;

public class FileSystemProvisionerWizard extends Wizard implements IProvisionerWizard {
	
	private DirectorySelectionPage fPage = null;
	private File[] fDirs = null;
	
	public FileSystemProvisionerWizard() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.FileSystemProvisionerWizard_title); 
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_FILESYSTEM_WIZARD);
	}

	public File[] getLocations() {
		return fDirs;
	}

	public boolean performFinish() {
		fDirs = fPage.getLocations();
		return true;
	}

	public void addPages() {
		fPage = new DirectorySelectionPage("file system"); //$NON-NLS-1$
		addPage(fPage);
		super.addPages();
	}

}
