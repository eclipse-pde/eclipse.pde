/*******************************************************************************
 * Copyright (c) 2015 Alex Blewitt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alex Blewitt <alex.blewitt@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddActivationPolicyResolution extends AbstractManifestMarkerResolution {

	public AddActivationPolicyResolution(int type) {
		super(type);
	}

	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			IManifestHeader header = bun.getManifestHeader(Constants.BUNDLE_ACTIVATIONPOLICY);
			if (header == null) {
				bundle.setHeader(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
			}
		}
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.UpdateActivationResolution_bundleActivationPolicyAdd_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.UpdateActivationResolution_bundleActivationPolicyAdd_label;
	}
}
