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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class OrganizeManifestsWizard extends Wizard {

	private OrganizeManifestsWizardPage fMainPage;
	private ArrayList fProjects;
	
	public OrganizeManifestsWizard(ArrayList projects) {
		fProjects = projects;
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.OrganizeManifestsWizard_title);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_ORGANIZE_MANIFESTS);
	}

	public boolean performFinish() {
		fMainPage.preformOk();
		try {
			OrganizeManifestsOperation op = new OrganizeManifestsOperation(fProjects);
			op.setOperations(fMainPage.getSettings());
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.log(e);
			return false;
		}
		return true;
	}
	
	public void addPages() {
		fMainPage = new OrganizeManifestsWizardPage();
		addPage(fMainPage);
	}
}
