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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public abstract class AbstractPDELaunchConfigurationTabGroup extends
		AbstractLaunchConfigurationTabGroup {

	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		final ILaunchConfiguration config = configuration;
		final ILaunchConfigurationTab[] tabs = getTabs();
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					if (config instanceof ILaunchConfigurationWorkingCopy) {
						checkBackwardCompatibility(
							(ILaunchConfigurationWorkingCopy) config);
					}
				} catch (CoreException e) {
				}
				for (int i = 0; i < tabs.length; i++) {
					tabs[i].initializeFrom(config);
				}
			}
		});
	}
	
	private void checkBackwardCompatibility(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		String id = wc.getAttribute(
						IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
						(String) null);
		if (id == null) {
			wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
		}
		
		String value = wc.getAttribute("vmargs", (String)null);
		if (value != null) {
			wc.setAttribute("vmargs", (String)null);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, value);
		}
		
		value = wc.getAttribute("progargs", (String)null);
		if (value != null) {
			wc.setAttribute("progargs", (String)null);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, value);
		}
		
		value = wc.getAttribute(IPDELauncherConstants.LOCATION + "0", (String)null);
		if (value != null) {
			wc.setAttribute(IPDELauncherConstants.LOCATION + "0", (String)null);
			wc.setAttribute(IPDELauncherConstants.LOCATION, value);			
		}
		
		LaunchPluginValidator.checkBackwardCompatibility(wc, false);	
		wc.doSave();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		configuration.setAttribute(
			IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
			"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
	}

}
