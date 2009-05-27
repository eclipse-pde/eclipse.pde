/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 150225
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class AddDefaultExecutionEnvironmentResolution extends AbstractManifestMarkerResolution {

	private String id;

	public AddDefaultExecutionEnvironmentResolution(int type, String id) {
		super(type);
		this.id = id;
	}

	protected void createChange(BundleModel model) {

		IManifestHeader header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header == null) {
			// Initialize header with empty value
			model.getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, ""); //$NON-NLS-1$
		}

		// Get header
		header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);

		if (header != null && header instanceof RequiredExecutionEnvironmentHeader) {
			IExecutionEnvironment ee = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(id);
			((RequiredExecutionEnvironmentHeader) header).addExecutionEnvironment(ee);
		}
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddDefaultExecutionEnvironment_label, id);
	}
}
