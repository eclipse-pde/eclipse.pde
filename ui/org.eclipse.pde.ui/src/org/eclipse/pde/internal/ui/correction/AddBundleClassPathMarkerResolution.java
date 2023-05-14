/*******************************************************************************
 *  Copyright (c) 2007, 2020 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleClasspathHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddBundleClassPathMarkerResolution extends AbstractManifestMarkerResolution {



	public AddBundleClassPathMarkerResolution(int type, IMarker marker) {
		super(type, marker);

	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddBundleClassPathResolution_add, marker.getAttribute("entry", null)); //$NON-NLS-1$
	}

	@Override
	protected void createChange(BundleModel model) {
		String value = marker.getAttribute("entry", null);//$NON-NLS-1$
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			BundleClasspathHeader header = (BundleClasspathHeader) bundle.getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if (header != null)
				header.addLibrary(value);
			else
				model.getBundle().setHeader(Constants.BUNDLE_CLASSPATH, value);
		}
	}

}
