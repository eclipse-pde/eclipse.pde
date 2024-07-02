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

import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveUnknownExecEnvironments extends AbstractManifestMarkerResolution {

	public RemoveUnknownExecEnvironments(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header instanceof RequiredExecutionEnvironmentHeader reqHeader) {
			Set<String> systemEnvs = TargetPlatformHelper.getPDEState().getfProvidedExecutionEnvironments();
			for (String ee : reqHeader.getEnvironments()) {
				if (!systemEnvs.contains(ee)) {
					reqHeader.removeExecutionEnvironment(ee);
				}
			}
		}
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RemoveUnknownExecEnvironments_label;
	}

}
