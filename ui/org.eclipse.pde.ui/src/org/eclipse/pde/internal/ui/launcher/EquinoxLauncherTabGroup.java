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

import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.launcher.EquinoxPluginsTab;
import org.eclipse.pde.ui.launcher.EquinoxSettingsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

public class EquinoxLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs =
			tabs = new ILaunchConfigurationTab[]{
					new EquinoxPluginsTab(),
					new JavaArgumentsTab(),
					new EquinoxSettingsTab(),
					new TracingTab(), 
					new EnvironmentTab(),
					new CommonTab()};
		setTabs(tabs);
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "-console"); //$NON-NLS-1$
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String progArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (progArgs.indexOf("-console") == -1) //$NON-NLS-1$
			progArgs = "-console " + progArgs; //$NON-NLS-1$
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, progArgs); //$NON-NLS-1$
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (vmArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}

}
