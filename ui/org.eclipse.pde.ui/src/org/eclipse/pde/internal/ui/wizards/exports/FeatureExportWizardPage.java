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
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;


public class FeatureExportWizardPage extends ExportWizardPageWithTable {
	
	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport", //$NON-NLS-1$
			PDEPlugin.getResourceString("ExportWizard.Feature.pageBlock")); //$NON-NLS-1$
		setTitle(PDEPlugin.getResourceString("ExportWizard.Feature.pageTitle")); //$NON-NLS-1$
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizardPage#findModelFor(org.eclipse.core.resources.IProject)
	 */
	protected IModel findModelFor(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject)object).getProject();
		if (object instanceof IProject)
			return PDECore.getDefault().getWorkspaceModelManager().getFeatureModel((IProject)object);
		return null;
	}
	
	protected String getJarButtonText() {
		return PDEPlugin.getResourceString("BaseExportWizardPage.fPackageJARs");
	}
}
