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
package org.eclipse.pde.internal.ui.osgi.wizards.project;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.project.NewProjectWizard;

public class NewBundleFragmentProjectWizard extends NewProjectWizard {
private static final String KEY_TITLE = "NewBundleFragmentProjectWizard.title";

public NewBundleFragmentProjectWizard() {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
	setWindowTitle(PDEPlugin.getResourceString(KEY_TITLE));
}
public boolean isFragmentWizard() {
	return true;
}
}
