/*******************************************************************************
 *  Copyright (c) 2006, 2020 IBM Corporation and others.
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

import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.IOrganizeManifestsSettings;
import org.osgi.framework.Constants;

public class AddExportPackageMarkerResolution extends AbstractManifestMarkerResolution {


	public AddExportPackageMarkerResolution(IMarker mark, int type) {
		super(type, mark);
		this.marker = mark;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddExportPackageResolution_Label;
	}

	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle bun) {
			ExportPackageHeader header = (ExportPackageHeader) bun.getManifestHeader(Constants.EXPORT_PACKAGE);
			if (header == null) {
				bundle.setHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
				header = (ExportPackageHeader) bun.getManifestHeader(Constants.EXPORT_PACKAGE);
			}
			processPackages(header, false);
		}
	}

	protected void processPackages(ExportPackageHeader header, boolean setInternal) {
		String values = marker.getAttribute("packages", null); //$NON-NLS-1$
		if (values == null) {
			return;
		}
		String[] packages = values.split(","); //$NON-NLS-1$
		String filter = PDEPlugin.getDefault().getDialogSettings().get(IOrganizeManifestsSettings.PROP_INTERAL_PACKAGE_FILTER);
		if (filter == null) {
			filter = IOrganizeManifestsSettings.VALUE_DEFAULT_FILTER;
		}
		Pattern pat = PatternConstructor.createPattern(filter, false);
		for (String packageId : packages) {
			ExportPackageObject obj = header.addPackage(packageId);
			if (setInternal){
				obj.setInternal(setInternal);
				continue;
			}
			if (pat.matcher(packageId).matches()) {
				obj.setInternal(true);
			}
		}
	}

}
