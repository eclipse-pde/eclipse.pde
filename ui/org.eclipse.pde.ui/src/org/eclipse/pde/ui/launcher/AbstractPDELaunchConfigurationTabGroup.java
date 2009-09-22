/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.launching.PDESourcePathProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 * An abstract class subclassed by the Eclipse application and JUnit Plug-in launch
 * configuration tab groups.
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.3
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class AbstractPDELaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * The tab group delegates to all tabs in the group.
	 * Prior to the delegation, it migrates all pre-3.2 launch configurations
	 * to make them 3.2-compliant.
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		final ILaunchConfiguration config = configuration;
		final ILaunchConfigurationTab[] tabs = getTabs();
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					if (config instanceof ILaunchConfigurationWorkingCopy) {
						checkBackwardCompatibility((ILaunchConfigurationWorkingCopy) config);
					}
				} catch (CoreException e) {
				}
				for (int i = 0; i < tabs.length; i++) {
					tabs[i].initializeFrom(config);
				}
			}
		});
	}

	/**
	 * Checks if the launch configuration is 3.2-compliant and migrates it if it's not.
	 * 
	 * @param wc 
	 * 			the launch configuration to be migrated if it's not 3.2-compliant
	 * @throws CoreException
	 * 			a CoreException is thrown if there was an error retrieving launch 
	 * 			configuration attributes
	 */
	private void checkBackwardCompatibility(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		String id = wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
		if (id == null) {
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
		}

		String value = wc.getAttribute("vmargs", (String) null); //$NON-NLS-1$
		if (value != null) {
			wc.setAttribute("vmargs", (String) null); //$NON-NLS-1$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, value);
		}

		value = wc.getAttribute("progargs", (String) null); //$NON-NLS-1$
		if (value != null) {
			wc.setAttribute("progargs", (String) null); //$NON-NLS-1$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, value);
		}

		value = wc.getAttribute(IPDELauncherConstants.LOCATION + "0", (String) null); //$NON-NLS-1$
		if (value != null) {
			wc.setAttribute(IPDELauncherConstants.LOCATION + "0", (String) null); //$NON-NLS-1$
			wc.setAttribute(IPDELauncherConstants.LOCATION, value);
		}

		BundleLauncherHelper.checkBackwardCompatibility(wc, false);
		if (wc.isDirty()) {
			wc.doSave();
		}
	}

	/**
	 * Delegates to all tabs to set defaults.
	 * It then sets program and VM arguments based on values on the 
	 * <b>Plug-in Development > Target Platform > Launching Arguments</b> preference page.
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		if (TargetPlatformHelper.usesNewApplicationModel())
			configuration.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$ 
		else if (TargetPlatformHelper.getTargetVersion() >= 3.2)
			configuration.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.2a"); //$NON-NLS-1$ 

		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);

		// Set Program/VM arguments with preference values
		String programArgs = LaunchArgumentsHelper.getInitialProgramArguments().trim();
		if (programArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);

		String vmArgs = LaunchArgumentsHelper.getInitialVMArguments().trim();
		if (vmArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

		configuration.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);
	}

}
