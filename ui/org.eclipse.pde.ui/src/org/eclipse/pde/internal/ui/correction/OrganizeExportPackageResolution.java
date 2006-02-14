package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifest;

public class OrganizeExportPackageResolution extends
		ManifestHeaderErrorResolution {
	
	private IProject fProject;

	public OrganizeExportPackageResolution(int type, IProject project) {
		super(type);
		fProject = project;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeExportPackages(model.getBundle(), fProject, true, true);
	}

	public String getDescription() {
		return PDEUIMessages.OrganizeExportPackageResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeExportPackageResolution_Label;
	}

}
