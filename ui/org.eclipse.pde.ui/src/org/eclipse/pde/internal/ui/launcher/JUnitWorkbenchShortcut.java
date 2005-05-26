/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class JUnitWorkbenchShortcut extends JUnitLaunchShortcut {	
	
	/**
	 * Returns the local java launch config type
	 */
	protected ILaunchConfigurationType getJUnitLaunchConfigType() {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType("org.eclipse.pde.ui.JunitLaunchConfig");		 //$NON-NLS-1$
	}
	
	protected ILaunchConfiguration createConfiguration(
		IJavaProject project, String name, String mainType, String container, String testName) {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType= getJUnitLaunchConfigType();
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(name));
			wc.setAttribute(ILauncherSettings.LOCATION + "0", getDefaultWorkspaceLocation()); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.VMARGS, ""); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.PROGARGS, ""); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.USE_DEFAULT, true);
			wc.setAttribute(ILauncherSettings.DOCLEAR, true);
			wc.setAttribute(ILauncherSettings.ASKCLEAR, false);
			wc.setAttribute(ILauncherSettings.CONFIG_CLEAR_AREA, true);
			wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainType);
			wc.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, false);
			wc.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, container);
			if (testName.length() > 0)
				wc.setAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, testName);	
			if (!JUnitLaunchConfiguration.requiresUI(wc))
				wc.setAttribute(ILauncherSettings.APPLICATION, JUnitLaunchConfiguration.CORE_APPLICATION);
			config= wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.log(ce);
		}
		return config;
	}
	
	protected String getDefaultWorkspaceLocation() {
		return LauncherUtils.getDefaultPath().append("junit-workbench-workspace").toOSString();				 //$NON-NLS-1$
	}
	
}
