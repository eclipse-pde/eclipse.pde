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
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.help.WorkbenchHelp;


public class FeatureExportWizardPage extends BaseExportWizardPage {
	
	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport",
			PDEPlugin.getResourceString("ExportWizard.Feature.pageBlock"),
			true);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Feature.pageTitle"));
	}

	public Object[] getListElements() {
		NewWorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getFeatureModels();
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizardPage#isValidModel(org.eclipse.pde.core.IModel)
	 */
	protected boolean isValidModel(IModel model) {
		return model instanceof IFeatureModel;
	}

}
