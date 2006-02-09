package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsOperation;

public class OrganizeImportPackageResolution extends
		ManifestHeaderErrorResolution {
	
	private boolean fRemoveImports;

	public OrganizeImportPackageResolution(int type, boolean removeImports) {
		super(type);
		fRemoveImports = removeImports;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifestsOperation.organizeImportPackages(model.getBundle(), fRemoveImports);
	}

	public String getDescription() {
		return PDEUIMessages.OrganizeImportPackageResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeImportPackageResolution_Label;
	}

}
