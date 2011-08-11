/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

/**
 * Resolution to add the Bundle-ManifestVersion header to the manifest.  Will set the manifest
 * version to 2 to support OSGi R4 headers.
 */
public class AddBundleManifestVersionResolution extends AbstractManifestMarkerResolution {
	
	public AddBundleManifestVersionResolution() {
		super(AbstractPDEMarkerResolution.CREATE_TYPE);
	}

	public String getLabel() {
		return PDEUIMessages.AddBundleManifestVersionResolution_label;
	}

	public String getDescription() {
		return PDEUIMessages.AddBundleManifestVersionResolution_description;
	}

	protected void createChange(BundleModel model) {
		// Add the Bundle-ManifestVersion header.
		model.getBundle().setHeader(Constants.BUNDLE_MANIFESTVERSION, String.valueOf(2));
	}

}
