/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;

public class SynchronizePropertiesWizard extends Wizard {
	private static final String KEY_WTITLE = "SynchronizePropertiesWizard.wtitle"; //$NON-NLS-1$

	private SynchronizePropertiesWizardPage fMainPage;

	private ISiteModel fModel;

	private ISiteFeature fSiteFeature;

	public SynchronizePropertiesWizard(ISiteFeature siteFeature,
			ISiteModel model) {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
		fSiteFeature = siteFeature;
		fModel = model;
	}

	public void addPages() {
		fMainPage = new SynchronizePropertiesWizardPage(fSiteFeature, fModel);
		addPage(fMainPage);
	}

	public boolean performFinish() {
		return fMainPage.finish();
	}
}
