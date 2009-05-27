/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveInternalDirectiveEntryResolution extends AbstractManifestMarkerResolution {

	private String fPackageName;

	public RemoveInternalDirectiveEntryResolution(int type, String packageName) {
		super(type);
		fPackageName = packageName;
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header instanceof ExportPackageHeader) {
			ExportPackageObject exportedPackage = ((ExportPackageHeader) header).getPackage(fPackageName);
			if (exportedPackage != null)
				exportedPackage.removeInternalDirective();
		}
	}

	public String getLabel() {
		return PDEUIMessages.RemoveInternalDirective_label;
	}

	public String getDescription() {
		return PDEUIMessages.RemoveInternalDirective_desc;
	}

}
