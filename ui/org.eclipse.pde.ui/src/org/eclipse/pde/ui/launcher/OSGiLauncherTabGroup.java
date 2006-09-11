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

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.OSGiFrameworkManager;

/**
 * Creates and initializes the tabs on the OSGi Framework launch configuration.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 */
public class OSGiLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = 
			new ILaunchConfigurationTab[]{
				new BundlesTab(),
				new JavaArgumentsTab(),
				new OSGiSettingsTab(),
				new TracingTab(), 
				new EnvironmentTab(),
				new CommonTab()};
		setTabs(tabs);
	}
	
	/**
	 * Configures defaults on newly created launch configurations.
	 * This function also passes the launch configuration copy to the default
	 * registered OSGi framework, giving it an opportunity to initialize and override
	 * more defaults on the launch configuration.
	 * Refer to the <code>org.eclipse.pde.core.osgiFrameworks</code> extension point for more details
	 * on OSGi frameworks.
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		OSGiFrameworkManager manager = PDEPlugin.getDefault().getOSGiFrameworkManager();
		manager.getDefaultInitializer().initialize(configuration);
	}

}
