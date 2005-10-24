package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class OptionalImportPackageResolution extends ManifestHeaderErrorResolution {
	
	private String fPackageName;
	
	public OptionalImportPackageResolution(int type, String packageName) {
		super(type);
		fPackageName = packageName;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle)model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader)bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null) {
			ImportPackageObject pkg = header.getPackage(fPackageName);
			if (pkg != null)
				pkg.setOptional(true);
		}
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.OptionalImportPkgResolution_description, fPackageName);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.OptionalImportPkgResolution_label, fPackageName);
	}

}
