package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
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
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceFeatureModels();
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
	}
}
