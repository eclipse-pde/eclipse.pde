package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;


public class PluginExportWizardPage extends BaseExportWizardPage {
	public PluginExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"pluginExport",
			PDEPlugin.getResourceString("ExportWizard.Plugin.pageBlock"),
			false);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Plugin.pageTitle"));
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getAllModels();
	}
}
