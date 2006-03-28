package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveRequireBundleResolution extends AbstractManifestMarkerResolution {
	
	private String fBundleId;

	public RemoveRequireBundleResolution(int type, String bundleID) {
		super(type);
		fBundleId = bundleID;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle)model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader)bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null)
			header.removeBundle(fBundleId);
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveRequireBundleResolution_description, fBundleId);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveRequireBundleResolution_label, fBundleId);
	}

}
