package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginExportWizardPage extends BaseExportWizardPage {
	public PluginExportWizardPage(IStructuredSelection selection) {
		super(selection, "pluginExport", PDEPlugin.getResourceString("ExportWizard.Plugin.pageBlock"), false);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Plugin.pageTitle"));
		setDescription(PDEPlugin.getResourceString("ExportWizard.Plugin.description"));
	}
	
	public Object [] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getAllModels();
	}
}
