/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveUnknownExecEnvironments extends AbstractManifestMarkerResolution {

	public RemoveUnknownExecEnvironments(int type) {
		super(type);
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header instanceof RequiredExecutionEnvironmentHeader) {
			RequiredExecutionEnvironmentHeader reqHeader = (RequiredExecutionEnvironmentHeader) header;
			ExecutionEnvironment[] bundleEnvs = reqHeader.getEnvironments();
			IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			for (int i = 0; i < bundleEnvs.length; i++) {
				boolean found = false;
				for (int j = 0; j < systemEnvs.length; j++) {
					if (bundleEnvs[i].getName().equals(systemEnvs[j].getId())) {
						found = true;
						break;
					}
				}
				if (!found)
					reqHeader.removeExecutionEnvironment(bundleEnvs[i]);
			}
		}
	}

	public String getLabel() {
		return PDEUIMessages.RemoveUnknownExecEnvironments_label;
	}

}
