/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

/**
 * Resolution to add the Bundle-ManifestVersion header to the manifest.  Will set the manifest
 * version to 2 to support OSGi R4 headers.
 */
public class AddBundleManifestVersionResolution extends AbstractManifestMarkerResolution {

	public AddBundleManifestVersionResolution(IMarker marker) {
		super(AbstractPDEMarkerResolution.CREATE_TYPE, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddBundleManifestVersionResolution_label;
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.AddBundleManifestVersionResolution_description;
	}

	@Override
	protected void createChange(BundleModel model) {
		// Add the Bundle-ManifestVersion header.
		model.getBundle().setHeader(Constants.BUNDLE_MANIFESTVERSION, String.valueOf(2));
	}

}
