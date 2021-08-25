/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveRedundantAutomaticModuleHeader extends AbstractManifestMarkerResolution {



	public RemoveRedundantAutomaticModuleHeader(int type, IMarker marker) {
		super(type, marker);

	}

	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			bundle.setHeader(ICoreConstants.AUTOMATIC_MODULE_NAME, ""); //$NON-NLS-1$
		}
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RemoveAutomaticModuleResolution_remove;
	}

}
