/*******************************************************************************
 *  Copyright (c) 2006, 2018 IBM Corporation and others.
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

import java.util.HashSet;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.IOrganizeManifestsSettings;
import org.osgi.framework.Constants;

public class AddExportPackageMarkerResolution extends AbstractManifestMarkerResolution {

	private String fValues;
	IMarker marker = null;

	public AddExportPackageMarkerResolution(IMarker mark, int type, String values) {
		super(type);
		this.fValues = values;
		this.marker = mark;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddExportPackageResolution_Label;
	}

	@Override
	public void run(IMarker marker) {
		this.marker = marker;
		super.run(marker);
	}

	@Override
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
		fValues = marker.getAttribute("packages", null); //$NON-NLS-1$
		if (fValues == null) {
			return;
		}
		String[] packages = fValues.split(","); //$NON-NLS-1$
		String filter = PDEPlugin.getDefault().getDialogSettings().get(IOrganizeManifestsSettings.PROP_INTERAL_PACKAGE_FILTER);
		if (filter == null)
			filter = IOrganizeManifestsSettings.VALUE_DEFAULT_FILTER;
		Pattern pat = PatternConstructor.createPattern(filter, false);
		for (String packageId : packages) {
			ExportPackageObject obj = header.addPackage(packageId);
			if (pat.matcher(packageId).matches())
				obj.setInternal(true);
		}
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet<IMarker> mset = new HashSet<>(markers.length);
		for (IMarker iMarker : markers) {
			if (iMarker.equals(marker))
				continue;
			String str = iMarker.getAttribute(PDEMarkerFactory.compilerKey, ""); //$NON-NLS-1$
			if (str.equals(marker.getAttribute(PDEMarkerFactory.compilerKey, ""))) //$NON-NLS-1$
				mset.add(iMarker);
		}
		int size = mset.size();
		return mset.toArray(new IMarker[size]);
	}

}
