package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class OptionalRequireBundleResolution extends
		ManifestHeaderErrorResolution {
	
	private String fBundleId;

	public OptionalRequireBundleResolution(int type, String bundleId) {
		super(type);
		fBundleId = bundleId;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle)model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader)bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null) {
			RequireBundleObject[] required = header.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (fBundleId.equals(required[i].getId())) 
					required[i].setOptional(true);
			}
		}
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.OptionalRequireBundleResolution_description, fBundleId);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.OptionalRequireBundleResolution_label, fBundleId);
	}

}
