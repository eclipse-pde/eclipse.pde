package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FeatureExportWizardPage extends BaseExportWizardPage {
	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(selection, "featureExport", "Available &Features", true);
		setTitle("Deployable features");
		setDescription("Export the selected projects into a form suitable for deploying in an Eclipse product");
	}
	
	public Object [] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceFeatureModels();
	}
}
