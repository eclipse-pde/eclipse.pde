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
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifest;

public class OrganizeExportPackageResolution extends AbstractManifestMarkerResolution {

	public OrganizeExportPackageResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeExportPackages(model.getBundle(), fResource.getProject(), true, true);
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.OrganizeExportPackageResolution_Description;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.OrganizeExportPackageResolution_Label;
	}

}
