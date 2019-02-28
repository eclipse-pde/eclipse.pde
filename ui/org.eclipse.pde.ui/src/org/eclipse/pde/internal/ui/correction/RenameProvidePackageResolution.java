/*******************************************************************************
 *  Copyright (c) 2007, 2019 IBM Corporation and others.
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
 *     Brock Janiczak <brockj@tpg.com.au> - bug 169373
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RenameProvidePackageResolution extends AbstractManifestMarkerResolution {

	public RenameProvidePackageResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.RenameProvidePackageResolution_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RenameProvidePackageResolution_label;
	}

	@Override
	protected void createChange(BundleModel model) {
		model.getBundle().renameHeader(ICoreConstants.PROVIDE_PACKAGE, Constants.EXPORT_PACKAGE);
	}

}
