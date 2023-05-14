/*******************************************************************************
 *  Copyright (c) 2005, 2020 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class OptionalImportPackageResolution extends AbstractManifestMarkerResolution {



	public OptionalImportPackageResolution(int type, String packageName, IMarker marker) {
		super(type, marker);

	}

	@Override
	protected void createChange(BundleModel model) {
		String packageName = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		Bundle bundle = (Bundle) model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null) {
			ImportPackageObject pkg = header.getPackage(packageName);
			if (pkg != null)
				pkg.setOptional(true);
		}
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.OptionalImportPkgResolution_description,marker.getAttribute("packageName", (String) null)); //$NON-NLS-1$
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.OptionalImportPkgResolution_label,marker.getAttribute("packageName", (String) null)); //$NON-NLS-1$
	}

}
