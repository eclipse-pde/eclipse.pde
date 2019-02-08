/*******************************************************************************
 *  Copyright (c) 2006, 2019 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveImportExportServicesResolution extends AbstractManifestMarkerResolution {

	String fServiceHeader;

	public RemoveImportExportServicesResolution(int type, String serviceHeader, IMarker marker) {
		super(type, marker);
		fServiceHeader = serviceHeader;
	}

	@Override
	protected void createChange(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		IManifestHeader header = bundle.getManifestHeader(fServiceHeader);
		if (header != null)
			bundle.setHeader(fServiceHeader, null);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveImportExportServices_label, fServiceHeader);
	}

	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.RemoveImportExportServices_description, fServiceHeader);
	}

}
