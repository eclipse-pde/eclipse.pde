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
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;

public class GenericExtensionWizard extends Wizard implements IExtensionWizard {
	private IPluginModelBase model;
	private PointSelectionPage pointSelectionPage;
	private static final String KEY_WTITLE = "GenericExtensionWizard.wtitle";

public GenericExtensionWizard() {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
}
public void addPages() {
	pointSelectionPage = new PointSelectionPage(model.getPluginBase());
	addPage(pointSelectionPage);
}
public IPluginExtension getNewExtension() {
	return pointSelectionPage.getNewExtension();
}
public void init(IProject project, IPluginModelBase pluginModelBase) {
	this.model = pluginModelBase;
}
public boolean performFinish() {
	return pointSelectionPage.finish();
}

public boolean canFinish() {
	if (pointSelectionPage.canFinish()==false) return false;
	return super.canFinish();
}
}
