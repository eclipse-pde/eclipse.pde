/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class OptionalRequireBundleResolution extends AbstractManifestMarkerResolution {

	private String fBundleId;

	public OptionalRequireBundleResolution(int type, String bundleId) {
		super(type);
		fBundleId = bundleId;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		RequireBundleHeader header = (RequireBundleHeader) bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
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
