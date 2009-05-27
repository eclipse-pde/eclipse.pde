/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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

public class RemoveImportPackageResolution extends AbstractManifestMarkerResolution {

	private String fPkgName;

	public RemoveImportPackageResolution(int type, String packageName) {
		super(type);
		fPkgName = packageName;
	}

	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header != null)
			header.removePackage(fPkgName);
	}

	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveImportPkgResolution_description, fPkgName);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveImportPkgResolution_label, fPkgName);
	}

}
