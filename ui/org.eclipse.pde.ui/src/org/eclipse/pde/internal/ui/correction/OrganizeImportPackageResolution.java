package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifest;

public class OrganizeImportPackageResolution extends
		AbstractManifestMarkerResolution {
	
	private boolean fRemoveImports;

	public OrganizeImportPackageResolution(int type, boolean removeImports) {
		super(type);
		fRemoveImports = removeImports;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeImportPackages(model.getBundle(), fRemoveImports);
	}

	public String getDescription() {
		return PDEUIMessages.OrganizeImportPackageResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeImportPackageResolution_Label;
	}

}
