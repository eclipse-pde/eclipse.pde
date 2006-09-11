/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;

public class JUnitWorkbenchLaunchShortcut extends JUnitLaunchShortcut {	
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.JUnitLaunchShortcut#getLaunchConfigurationTypeId()
	 */
	protected String getLaunchConfigurationTypeId() {
		return "org.eclipse.pde.ui.JunitLaunchConfig"; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.JUnitLaunchShortcut#createLaunchConfiguration(org.eclipse.jdt.core.IJavaElement)
	 */
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);
		if (TargetPlatform.getTargetVersion() >= 3.2)
			wc.setAttribute("pde.version", "3.2a"); //$NON-NLS-1$ //$NON-NLS-2$
		setWorkspaceDataOptions(wc);
		setApplicationOptions(wc);
		setProgramArguments(wc);
		setVMArguments(wc);
		setConfigurationOptions(wc);
		setTracingOptions(wc);
		setSourcePathProvider(wc);
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		return wc;
	}
	
	protected void setVMArguments(ILaunchConfigurationWorkingCopy configuration) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (vmArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}
	
	protected void setProgramArguments(ILaunchConfigurationWorkingCopy configuration) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String programArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (programArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
	}
	
	protected void setWorkspaceDataOptions(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultJUnitWorkspaceLocation());
		configuration.setAttribute(IPDELauncherConstants.DOCLEAR, true);
		configuration.setAttribute(IPDELauncherConstants.ASKCLEAR, false);		
	}
	
	protected void setConfigurationOptions(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, false);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, LaunchArgumentsHelper.getDefaultJUnitConfigurationLocation());
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, true);		
	}
	
	protected void setApplicationOptions(ILaunchConfigurationWorkingCopy configuration) {
		if (LauncherUtils.requiresUI(configuration)) {
			String product = TargetPlatform.getDefaultProduct();
			if (product != null) {
				configuration.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
				configuration.setAttribute(IPDELauncherConstants.PRODUCT, product);
			}
		} else {
			configuration.setAttribute(IPDELauncherConstants.APPLICATION, JUnitLaunchConfigurationDelegate.CORE_APPLICATION);				
		}		
	}
	
	protected void setTracingOptions(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);		
	}
	
	protected void setSourcePathProvider(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
	            PDESourcePathProvider.ID); 		
	}
	
}
