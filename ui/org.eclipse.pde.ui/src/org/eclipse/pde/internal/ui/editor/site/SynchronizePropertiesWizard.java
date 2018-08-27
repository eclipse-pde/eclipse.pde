/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 181878
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.*;

public class SynchronizePropertiesWizard extends Wizard {
	private SynchronizePropertiesWizardPage fMainPage;

	private ISiteModel fModel;

	private ISiteFeature[] fSiteFeatures;

	public SynchronizePropertiesWizard(ISiteFeature[] siteFeatures, ISiteModel model) {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.SynchronizePropertiesWizard_wtitle);
		fSiteFeatures = siteFeatures;
		fModel = model;
	}

	@Override
	public void addPages() {
		fMainPage = new SynchronizePropertiesWizardPage(fSiteFeatures, fModel);
		addPage(fMainPage);
	}

	@Override
	public boolean performFinish() {
		return fMainPage.finish();
	}
}
