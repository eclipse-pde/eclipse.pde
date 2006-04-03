/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class SynchronizePropertiesAction extends Action {
	private ISiteModel fModel;

	private ISiteFeature fSiteFeature;

	public SynchronizePropertiesAction(ISiteFeature siteFeature,
			ISiteModel model) {
		setText(PDEUIMessages.SynchronizePropertiesAction_label);
		fSiteFeature = siteFeature;
		fModel = model;
	}

	public void run() {
		SynchronizePropertiesWizard wizard = new SynchronizePropertiesWizard(
				fSiteFeature, fModel);
		WizardDialog dialog = new WizardDialog(PDEPlugin
				.getActiveWorkbenchShell(), wizard);
		dialog.open();
	}
}
