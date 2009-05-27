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

import java.util.regex.Pattern;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.IOrganizeManifestsSettings;
import org.osgi.framework.Constants;

public class AddExportPackageMarkerResolution extends AbstractManifestMarkerResolution {

	private String fValues;

	public AddExportPackageMarkerResolution(int type, String values) {
		super(type);
		this.fValues = values;
	}

	public String getLabel() {
		return PDEUIMessages.AddExportPackageResolution_Label;
	}

	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			ExportPackageHeader header = (ExportPackageHeader) bun.getManifestHeader(Constants.EXPORT_PACKAGE);
			if (header == null) {
				bundle.setHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
				header = (ExportPackageHeader) bun.getManifestHeader(Constants.EXPORT_PACKAGE);
			}
			processPackages(header);
		}
	}

	private void processPackages(ExportPackageHeader header) {
		String[] packages = fValues.split(","); //$NON-NLS-1$
		String filter = PDEPlugin.getDefault().getDialogSettings().get(IOrganizeManifestsSettings.PROP_INTERAL_PACKAGE_FILTER);
		if (filter == null)
			filter = IOrganizeManifestsSettings.VALUE_DEFAULT_FILTER;
		Pattern pat = PatternConstructor.createPattern(filter, false);
		for (int i = 0; i < packages.length; i++) {
			ExportPackageObject obj = header.addPackage(packages[i]);
			if (pat.matcher(packages[i]).matches())
				obj.setInternal(true);
		}
	}

}
