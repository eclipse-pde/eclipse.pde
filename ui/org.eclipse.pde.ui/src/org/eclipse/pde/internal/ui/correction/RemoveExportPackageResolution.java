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
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveExportPackageResolution extends AbstractManifestMarkerResolution {



	public RemoveExportPackageResolution(int type, IMarker marker) {
		super(type, marker);

	}

	@Override
	protected void createChange(BundleModel model) {
		String markerPackage = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header != null) {
			// Issue 685 - If `markerPackage` can be null, it might indicate
			// that the marker is related to the removal of RequireBundle or
			// others. If it is not null, then markerPackage might represent the
			// imported package or others, and the ExportPackageHeader(header)
			// does not include it.
			if (markerPackage == null || !header.hasPackage(markerPackage)) {
				MultiFixResolution multiFixResolution = new MultiFixResolution(marker, null);
				multiFixResolution.run(marker);
			} else {
			header.removePackage(markerPackage);
			}
		}
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_label, marker.getAttribute("packageName", (String) null)); //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_description, marker.getAttribute("packageName", (String) null));//$NON-NLS-1$
	}

}
