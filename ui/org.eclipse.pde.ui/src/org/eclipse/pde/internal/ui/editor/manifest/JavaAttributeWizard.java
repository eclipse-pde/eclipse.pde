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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.core.plugin.*;

public class JavaAttributeWizard extends Wizard {
	private String className;
	private JavaAttributeWizardPage mainPage;
	private IProject project;
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;
	private static final String KEY_WTITLE = "JavaAttributeWizard.wtitle";

public JavaAttributeWizard(IProject project, IPluginModelBase model, ISchemaAttribute attInfo, String className) {
	this.className = className;
	this.model = model;
	this.project = project;
	this.attInfo = attInfo;
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	setNeedsProgressMonitor(true);
}

public void addPages() {
	mainPage = new JavaAttributeWizardPage(project, model, attInfo, className);
	addPage(mainPage);
}
public Object getValue() {
	return new JavaAttributeValue(project, model, attInfo, className);
}
public boolean performFinish() {
	boolean result = mainPage.finish();
	if (result) {
		className = mainPage.getClassName();
	}
	return result;
}
}
