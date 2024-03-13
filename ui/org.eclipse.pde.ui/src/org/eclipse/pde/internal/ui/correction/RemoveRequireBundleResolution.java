/*******************************************************************************
 *  Copyright (c) 2005, 2024 IBM Corporation and others.
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
 *	   Latha Patil (ETAS GmbH) - Issue #685
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveRequireBundleResolution extends AbstractManifestMarkerResolution {

	private String fBundleId;

	public RemoveRequireBundleResolution(int type, String bundleID, IMarker marker) {
		super(type, marker);
		fBundleId = bundleID;
	}

	@Override
	protected void createChange(BundleModel model) {
		fBundleId = marker.getAttribute("bundleId", null); //$NON-NLS-1$
		// Issue 685 - If `fBundleId` can be null, it might indicate that the marker is related to the removal of import/export packages or others.
		if (fBundleId == null) {
			MultiFixResolution multiFixResolution = new MultiFixResolution(marker, null);
			multiFixResolution.run(marker);
		}
		Bundle bundle = (Bundle) model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader) bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null)
			header.removeBundle(fBundleId);
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveRequireBundleResolution_description, fBundleId);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveRequireBundleResolution_label, fBundleId);
	}

}
