/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 * Creates and initializes the tabs on the OSGi Framework launch configuration.
 * This class in not intended to be instantiated by clients - it is an extension
 * that is instantiated by the platform.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 3.3
 */
public class OSGiLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * The tab group delegates to all tabs in the group. Prior to the
	 * delegation, it migrates all launch configurations.
	 *
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		final ILaunchConfiguration config = configuration;
		BusyIndicator.showWhile(Display.getCurrent(), () -> {
			try {
				if (config instanceof ILaunchConfigurationWorkingCopy) {
					BundleLauncherHelper.migrateOsgiLaunchConfiguration((ILaunchConfigurationWorkingCopy) config);
				}
			} catch (CoreException e) {
			}
			super.initializeFrom(configuration);
		});
	}

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {new BundlesTab(), new JavaArgumentsTab(), new OSGiSettingsTab(), new TracingTab(), new EnvironmentTab(), new CommonTab()};
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
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		OSGiFrameworkManager manager = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager();
		manager.getDefaultInitializer().initialize(configuration);
	}

}
