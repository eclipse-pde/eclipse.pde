/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;

public class OSGiLaunchConfigurationInitializer {
	
	protected static final String DEFAULT = "default"; //$NON-NLS-1$
	
	public void initialize(ILaunchConfigurationWorkingCopy configuration) {
		initializeFrameworkDefaults(configuration);
		initializeBundleState(configuration);
		initializeSourcePathProvider(configuration);
	}
	
	protected void initializeSourcePathProvider(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, 
									PDESourcePathProvider.ID);
	}

	protected void initializeFrameworkDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		configuration.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
	}
	
	protected void initializeBundleState(ILaunchConfigurationWorkingCopy configuration) {
		StringBuffer explugins = new StringBuffer();
		StringBuffer wsplugins = new StringBuffer();
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			boolean inWorkspace = models[i].getUnderlyingResource() != null;
			appendBundle(inWorkspace ? wsplugins : explugins, id);			
		}
		configuration.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, wsplugins.toString());
		configuration.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, explugins.toString());
		configuration.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
	}
	
	private void appendBundle(StringBuffer buffer, String bundleID) {
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$
		buffer.append(bundleID);
		buffer.append("@"); //$NON-NLS-1$
		buffer.append(getStartLevel(bundleID));
		buffer.append(":"); //$NON-NLS-1$
		buffer.append(getAutoStart(bundleID));	
	}
	
	protected String getStartLevel(String bundleID) {
		return DEFAULT;
	}
	
	protected String getAutoStart(String bundleID) {
		return DEFAULT;
	}

}
