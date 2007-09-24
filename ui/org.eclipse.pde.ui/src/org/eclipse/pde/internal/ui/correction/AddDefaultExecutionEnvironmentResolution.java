/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 150225
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddDefaultExecutionEnvironmentResolution extends AbstractManifestMarkerResolution {

	public AddDefaultExecutionEnvironmentResolution(int type) {
		super(type);
	}

	protected void createChange(BundleModel model) {

		IManifestHeader header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if(header == null) {
			// Initialize header with empty value
			model.getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "");
		}

		// Get header
		header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);

		if(header != null && header instanceof RequiredExecutionEnvironmentHeader) {
			// TODO consider moving to VMHelper class
			// Get available EEs & default install VM
			IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();

			for(int i = 0; i < systemEnvs.length; i++) {
				// Get strictly compatible EE for the default VM
				if(systemEnvs[i].isStrictlyCompatible(defaultVM)) {
					((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(systemEnvs[i]);
					break;
				}
			}
		}
	}

	public String getLabel() {
		return PDEUIMessages.AddDefaultExecutionEnvironment_label;
	}
}
