/*******************************************************************************
 *  Copyright (c) 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrew Obuchowicz <aobuchow@redhat.com> - Initial Implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.osgi.framework.Constants;

public class ReplaceExecEnvironment extends AbstractManifestMarkerResolution {

	private final String fRequiredEE;

	public ReplaceExecEnvironment(int type, IMarker marker) {
		super(type, marker);
		this.fRequiredEE = super.marker.getAttribute(PDEMarkerFactory.REQUIRED_EXEC_ENV, "JavaSE-1.8"); //$NON-NLS-1$
	}

	@Override
	public String getLabel() {
		return NLS.bind(Messages.ReplaceExecEnvironment_Marker_Label, this.fRequiredEE);
	}

	@Override
	protected void createChange(BundleModel model) {
		@SuppressWarnings("deprecation")
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header instanceof RequiredExecutionEnvironmentHeader reqHeader) {
			Set<String> systemEnvs = TargetPlatformHelper.getPDEState().getfProvidedExecutionEnvironments();
			if (systemEnvs.contains(fRequiredEE)) {
				for (String ee : reqHeader.getEnvironments()) {
					reqHeader.removeExecutionEnvironment(ee);
				}
				reqHeader.addExecutionEnvironment(fRequiredEE);
			}
		}
	}

}
