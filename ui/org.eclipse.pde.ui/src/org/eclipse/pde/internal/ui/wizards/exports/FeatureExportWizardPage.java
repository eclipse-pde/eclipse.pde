package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;


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
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceFeatureModels();
	}
}
