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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;


public class FeatureExportWizardPage extends ExportWizardPageWithTable {
	
	private AdvancedFeatureExportPage featurePage;
	
	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport", //$NON-NLS-1$
			PDEUIMessages.ExportWizard_Feature_pageBlock); //$NON-NLS-1$
		setTitle(PDEUIMessages.ExportWizard_Feature_pageTitle); //$NON-NLS-1$
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getFeatureModels();
	}
	
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
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
		return PDEUIMessages.BaseExportWizardPage_fPackageJARs; //$NON-NLS-1$
	}
	
	protected void setFeaturePage(AdvancedFeatureExportPage fPage) {
		featurePage = fPage;
	}
	
	protected void pageUpdate(boolean hideJNLP) {
		featurePage.hideJNLP(hideJNLP);
		if (isPageComplete() || getErrorMessage() == null) {
			featurePage.forceValidatePage(true);
		}
	}
	
	public IWizardPage getNextPage() {
		String exportType = ((BaseExportWizard)getWizard()).getExportOperation();
		featurePage.hideJNLP(exportType.equals("zip")); //$NON-NLS-1$
		return super.getNextPage();
	}
}
