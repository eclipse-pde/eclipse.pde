/*******************************************************************************
 *  Copyright (c) 2005, 2024 IBM Corporation and others.
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
 *	   Latha Patil (ETAS GmbH) - Issue #685
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveImportPackageResolution extends AbstractManifestMarkerResolution {


	public RemoveImportPackageResolution(int type, IMarker marker) {
		super(type, marker);

	}

	@Override
	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null) {
			String packageName = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
			//Issue 685 - If `packageName` can be null, it might indicate that the marker is related to the removal of RequireBundle or others. If it is not null, then `packageName` might represent the exported package or others, and the ImportPackageHeader(header) does not include it.

			if (packageName == null || !header.hasPackage(packageName)) {
				MultiFixResolution multiFixResolution = new MultiFixResolution(marker, null);
				multiFixResolution.run(marker);
			} else {
				header.removePackage(packageName);
			}
		}
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveImportPkgResolution_description, marker.getAttribute("packageName", (String) null)); //$NON-NLS-1$
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveImportPkgResolution_label, marker.getAttribute("packageName", (String) null)); //$NON-NLS-1$
	}

}
