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
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveInternalDirectiveEntryResolution extends AbstractManifestMarkerResolution {

	private String fPackageName;

	public RemoveInternalDirectiveEntryResolution(int type, String packageName, IMarker marker) {
		super(type, marker);
		fPackageName = packageName;
	}

	@Override
	protected void createChange(BundleModel model) {
		fPackageName= marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header instanceof ExportPackageHeader) {
			ExportPackageObject exportedPackage = ((ExportPackageHeader) header).getPackage(fPackageName);
			if (exportedPackage != null)
				exportedPackage.removeInternalDirective();
		}
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RemoveInternalDirective_label;
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.RemoveInternalDirective_desc;
	}

}
