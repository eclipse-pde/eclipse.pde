/*******************************************************************************
 * Copyright (c) 2015, 2019 Alex Blewitt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt <alex.blewitt@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddActivationPolicyResolution extends AbstractManifestMarkerResolution {

	public AddActivationPolicyResolution(int type, IMarker marker) {
		super(type, marker);
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
