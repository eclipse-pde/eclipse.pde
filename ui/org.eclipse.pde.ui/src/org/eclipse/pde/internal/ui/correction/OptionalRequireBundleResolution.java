/*******************************************************************************
 *  Copyright (c) 2005, 2020 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class OptionalRequireBundleResolution extends AbstractManifestMarkerResolution {


	public OptionalRequireBundleResolution(int type, IMarker marker) {
		super(type, marker);

	}

	@Override
	protected void createChange(BundleModel model) {
		String bundleId = marker.getAttribute("bundleId", (String) null); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader) bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null) {
			RequireBundleObject[] requiredBundles = header.getRequiredBundles();
			for (RequireBundleObject requiredBundle : requiredBundles) {
				if (bundleId.equals(requiredBundle.getId()))
					requiredBundle.setOptional(true);
			}
		}
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.OptionalRequireBundleResolution_description,  marker.getAttribute("bundleId", (String) null)); //$NON-NLS-1$
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.OptionalRequireBundleResolution_label,  marker.getAttribute("bundleId", (String) null)); //$NON-NLS-1$
	}

}
