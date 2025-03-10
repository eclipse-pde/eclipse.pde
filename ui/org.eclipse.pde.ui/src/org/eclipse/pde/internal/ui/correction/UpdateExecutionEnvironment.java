/*******************************************************************************
 *  Copyright (c) 2021 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class UpdateExecutionEnvironment extends AbstractManifestMarkerResolution {

	public UpdateExecutionEnvironment(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.UpdateExecutionEnvironment_label;
	}

	@Override
	protected void createChange(BundleModel model) {
		IBundle bundle = model.getBundle();
		if (bundle instanceof Bundle) {
			String bree = null;
			try {
				bree = (String) marker.getAttribute("BREE"); //$NON-NLS-1$
			} catch (CoreException e) {

			}
			if (bree != null) {
				bundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, bree);
			}

		}
	}

}
