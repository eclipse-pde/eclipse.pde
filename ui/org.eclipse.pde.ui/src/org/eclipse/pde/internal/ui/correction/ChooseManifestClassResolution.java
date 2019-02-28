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
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;

public class ChooseManifestClassResolution extends AbstractManifestMarkerResolution {

	private String fHeader;

	public ChooseManifestClassResolution(int type, String headerName, IMarker marker) {
		super(type, marker);
		fHeader = headerName;
	}

	@Override
	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(fHeader);
		String type = PDEJavaHelperUI.selectType(fResource, IJavaElementSearchConstants.CONSIDER_CLASSES);
		if (type != null)
			header.setValue(type);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.ChooseManifestClassResolution_label, fHeader);
	}

}
