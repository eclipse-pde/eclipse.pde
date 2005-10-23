package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IMarkerResolution;

public class RemoveExportPkgResolution extends ManifestHeaderErrorResolution
		implements IMarkerResolution {
	
	String fPackage;

	public RemoveExportPkgResolution(int type, String pkgName) {
		super(type);
		fPackage = pkgName;
	}

	protected void createChange(BundleModel model) {
		ExportPackageUtil.removeExportPackage(model.getBundle(), fPackage);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_label, fPackage);
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_description, fPackage);
	}

}
