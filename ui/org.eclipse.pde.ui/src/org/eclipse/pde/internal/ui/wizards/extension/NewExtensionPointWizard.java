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

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;

public class NewExtensionPointWizard extends NewWizard {
	private NewExtensionPointMainPage mainPage;
	private IPluginModelBase model;
	private IProject project;
	private IPluginExtensionPoint point;
	private static final String KEY_WTITLE = "NewExtensionPointWizard.wtitle"; //$NON-NLS-1$

	public NewExtensionPointWizard(IProject project, IPluginModelBase model) {
		this(project, model, null);
	}
	
	public NewExtensionPointWizard(IProject project, IPluginModelBase model, IPluginExtensionPoint point){
		initialize();
		this.project = project;
		this.model = model;
		this.point = point;
	}

	public void initialize(){
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXP_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		setNeedsProgressMonitor(true);
	}
	
	public void addPages() {
		mainPage = new NewExtensionPointMainPage(project, model, point);
		addPage(mainPage);
	}

	public boolean performFinish() {
		return mainPage.finish();
	}
}
