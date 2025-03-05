package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class UpdateSingletonToSymbolicName extends AbstractManifestMarkerResolution {

	private final boolean fisDirective;

	public UpdateSingletonToSymbolicName(int type, boolean directive, IMarker marker) {
		super(type, marker);
		fisDirective = directive;
	}

	@Override
	public String getLabel() {
		String userDirective = marker.getAttribute("userDirective", null); //$NON-NLS-1$
		return NLS.bind(PDEUIMessages.UpdateSingleton_dir_label, userDirective);
	}

	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle bun) {
			IManifestHeader header = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			if (header instanceof BundleSymbolicNameHeader) {
				if (fisDirective && TargetPlatformHelper.getTargetVersion() >= 3.1)
					bundle.setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
				else if (!fisDirective && TargetPlatformHelper.getTargetVersion() < 3.1)
					bundle.setHeader(Constants.BUNDLE_MANIFESTVERSION, null);
				String entry = ((BundleSymbolicNameHeader) header).getValue();
				int ind1 = entry.indexOf(';');
				int ind2 = entry.indexOf(':');
				String invalidDir = entry.substring(ind1 + 1, ind2);
				((BundleSymbolicNameHeader) header).setDirective(invalidDir, null);
				((BundleSymbolicNameHeader) header).setDirective(Constants.SINGLETON_DIRECTIVE, String.valueOf(true));
			}
		}
	}

}
