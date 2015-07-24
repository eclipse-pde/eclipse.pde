/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveExportPackageResolution extends AbstractManifestMarkerResolution {

	String fPackage;

	public RemoveExportPackageResolution(int type, String pkgName) {
		super(type);
		fPackage = pkgName;
	}

	@Override
	protected void createChange(BundleModel model) {
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
