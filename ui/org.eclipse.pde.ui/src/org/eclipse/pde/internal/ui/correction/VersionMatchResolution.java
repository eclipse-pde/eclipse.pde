package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class VersionMatchResolution extends AbstractManifestMarkerResolution {
	public VersionMatchResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	protected void createChange(BundleModel model) {
		String bundleId = marker.getAttribute("bundleId", (String) null); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader) bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null) {
			RequireBundleObject[] requiredBundles = header.getRequiredBundles();
			for (int i = 0; i < requiredBundles.length; i++) {
				if (bundleId.equals(requiredBundles[i].getId())) {
					IPluginModelBase modelBase = PluginRegistry.findModel(bundleId);
					String version = null;
					if (modelBase != null) {
						version = VersionUtil
								.computeInitialPluginVersion(modelBase.getBundleDescription().getVersion().toString());
						requiredBundles[i].setVersion(version);
					}
				}
			}
		}
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddMatchingVersion_RequireBundle;
	}

}
