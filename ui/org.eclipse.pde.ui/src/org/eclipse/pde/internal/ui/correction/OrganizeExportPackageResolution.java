package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class OrganizeExportPackageResolution extends
		ManifestHeaderErrorResolution {
	
	private IProject fProject;

	public OrganizeExportPackageResolution(int type, IProject project) {
		super(type);
		fProject = project;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifestJob.organizeExportPackages(model.getBundle(), fProject);

	}

	public String getDescription() {
		return PDEUIMessages.OrganizeExportPackageResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeExportPackageResolution_Label;
	}

}
