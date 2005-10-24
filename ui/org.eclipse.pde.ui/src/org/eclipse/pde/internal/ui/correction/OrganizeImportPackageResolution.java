package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class OrganizeImportPackageResolution extends
		ManifestHeaderErrorResolution {
	
	private boolean fRemoveImports;

	public OrganizeImportPackageResolution(int type, boolean removeImports) {
		super(type);
		fRemoveImports = removeImports;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifestJob.organizeImportPackages(model.getBundle(), fRemoveImports);
	}

	public String getDescription() {
		return PDEUIMessages.OrganizeImportPackageResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeImportPackageResolution_Label;
	}

}
