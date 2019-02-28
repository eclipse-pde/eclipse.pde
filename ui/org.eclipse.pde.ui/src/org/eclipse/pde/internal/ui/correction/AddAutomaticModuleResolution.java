/*******************************************************************************
 *  Copyright (c) 2017, 2019 IBM Corporation and others.
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
import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddAutomaticModuleResolution extends AbstractManifestMarkerResolution {
	private IMarker mark;

	public AddAutomaticModuleResolution(int type, IMarker marker) {
		super(type, marker);
		mark = marker;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet<IMarker> mset = new HashSet<>(markers.length);
		for (IMarker iMarker : markers) {
			if (iMarker.equals(mark))
				continue;
			if (getProblemId(iMarker) == PDEMarkerFactory.M_NO_AUTOMATIC_MODULE)
				mset.add(iMarker);
		}
		int size = mset.size();
		return mset.toArray(new IMarker[size]);
	}
	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			Bundle bun = (Bundle) bundle;
			IManifestHeader header = bun.getManifestHeader(ICoreConstants.AUTOMATIC_MODULE_NAME);
			if (header == null) {
				IManifestHeader headerName = bun.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
				String val = headerName.getValue();
				try {
					val = val.substring(0, val.indexOf(';'));
				}
				catch (Exception e) {
					// for cases where ; not present
				}
				bundle.setHeader(ICoreConstants.AUTOMATIC_MODULE_NAME, val);
			}
		}
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.AddAutomaticModuleResolution_desc;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddAutomaticModuleResolution_label;
	}

	int getProblemId(IMarker marker) {
		int problemID = marker.getAttribute(PDEMarkerFactory.PROBLEM_ID, PDEMarkerFactory.NO_RESOLUTION);
		if (problemID != PDEMarkerFactory.NO_RESOLUTION) {
			return problemID;
		}
		return marker.getAttribute("id", PDEMarkerFactory.NO_RESOLUTION); //$NON-NLS-1$
	}


}
