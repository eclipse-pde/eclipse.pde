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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;

public class UpdateSitePage extends WizardPage {

	protected UpdateSitePage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.UpdateSiteWizardPage_title);
		setDescription(PDEUIMessages.UpdateSiteWizardPage_description);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		// TODO Auto-generated method stub

	}
	
	public File[] getLocations() {
//		Preferences pref = PDECore.getDefault().getPluginPreferences();
//		pref.setValue(LAST_LOCATION, fLastLocation);
//		return (File[]) fElements.toArray(new File[fElements.size()]);
		return null;
	}

}
