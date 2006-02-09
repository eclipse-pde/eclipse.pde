package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifestsOperation;

public class OrganizeRequireBundleResolution extends ManifestHeaderErrorResolution {
	
	private boolean fRemoveImports;

	public OrganizeRequireBundleResolution(int type, boolean removeImports) {
		super(type);
		fRemoveImports = removeImports;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifestsOperation.organizeRequireBundles(model.getBundle(), fRemoveImports);
	}

	public String getDescription() {
		return PDEUIMessages.OrganizeRequireBundleResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeRequireBundleResolution_Label;
	}

}
