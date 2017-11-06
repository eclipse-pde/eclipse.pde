/*******************************************************************************
 *  Copyright (c) 2017 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddAutomaticModuleResolution extends AbstractManifestMarkerResolution {

	public AddAutomaticModuleResolution(int type) {
		super(type);
	}

	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			IManifestHeader header = bun.getManifestHeader(ICoreConstants.AUTOMATIC_MODULE_NAME);
			if (header == null) {
				IManifestHeader headerName = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
				String val = headerName.getValue();
				try {
					val = val.substring(0, val.indexOf(';'));
				}
				catch (Exception e) {
					// for cases where ; not present
				}
				bundle.setHeader(ICoreConstants.AUTOMATIC_MODULE_NAME, val);
			}
		}
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.AddAutomaticModuleResolution_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddAutomaticModuleResolution_label;
	}

}
