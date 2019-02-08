/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveExportPackageResolution extends AbstractManifestMarkerResolution {

	String fPackage;

	public RemoveExportPackageResolution(int type, String pkgName, IMarker marker) {
		super(type, marker);
		fPackage = pkgName;
	}

	@Override
	protected void createChange(BundleModel model) {
		fPackage = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header != null)
			header.removePackage(fPackage);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_label, fPackage);
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveExportPkgs_description, fPackage);
	}

}
