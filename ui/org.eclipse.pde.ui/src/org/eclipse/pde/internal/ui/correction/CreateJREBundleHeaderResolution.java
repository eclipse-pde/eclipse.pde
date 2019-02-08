/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class CreateJREBundleHeaderResolution extends AbstractManifestMarkerResolution {

	public CreateJREBundleHeaderResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.CreateJREBundleHeaderResolution_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.CreateJREBundleHeaderResolution_label;
	}

	@Override
	protected void createChange(BundleModel model) {
		model.getBundle().setHeader(ICoreConstants.ECLIPSE_JREBUNDLE, Boolean.toString(true));
	}
}
