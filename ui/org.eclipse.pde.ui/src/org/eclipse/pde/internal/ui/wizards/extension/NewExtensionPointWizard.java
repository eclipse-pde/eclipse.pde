/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.NewWizard;

public class NewExtensionPointWizard extends NewWizard {
	private NewExtensionPointMainPage mainPage;
	private IPluginModelBase model;
	private IProject project;
	private static final String KEY_WTITLE = "NewExtensionPointWizard.wtitle";

	public NewExtensionPointWizard(IProject project, IPluginModelBase model) {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXP_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		this.model = model;
		this.project = project;
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		mainPage = new NewExtensionPointMainPage(project, model);
		addPage(mainPage);
	}

	public boolean performFinish() {
		return mainPage.finish();
	}
}
