/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 214156
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveLazyLoadingDirectiveResolution extends AbstractManifestMarkerResolution {

	private String fHeader = null;

	public RemoveLazyLoadingDirectiveResolution(int type, String currentHeader, IMarker marker) {
		super(type, marker);
		fHeader = currentHeader;
	}

	@Override
	protected void createChange(BundleModel model) {
		fHeader = marker.getAttribute("header", ICoreConstants.ECLIPSE_LAZYSTART) ;//$NON-NLS-1$
		model.getBundle().setHeader(fHeader, null);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RemoveLazyLoadingDirectiveResolution_remove;
	}

}
