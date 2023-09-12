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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class SynchronizePropertiesAction extends Action {
	private final ISiteModel fModel;

	private final ISiteFeature[] fSiteFeatures;

	public SynchronizePropertiesAction(ISiteFeature[] siteFeatures, ISiteModel model) {
		setText(PDEUIMessages.SynchronizePropertiesAction_label);
		fSiteFeatures = siteFeatures;
		fModel = model;
	}

	@Override
	public void run() {
		SynchronizePropertiesWizard wizard = new SynchronizePropertiesWizard(fSiteFeatures, fModel);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.open();
	}
}
