/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

public class WorkbenchLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog,
	 *      String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs =
			new ILaunchConfigurationTab[] {
				new RuntimeWorkbenchArgumentsTab(),
				new AdvancedLauncherTab(),
				new TracingLauncherTab(),
				new JavaSourceLookupTab(),
				new EnvironmentTab(),
				new CommonTab()};
		setTabs(tabs);
	}
	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		final ILaunchConfiguration config = configuration;
		final ILaunchConfigurationTab[] tabs = getTabs();
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					String id =
						config.getAttribute(
							IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
							(String) null);
					if (id == null
						&& config instanceof ILaunchConfigurationWorkingCopy) {
						ILaunchConfigurationWorkingCopy wc =
							(ILaunchConfigurationWorkingCopy) config;
						wc.setAttribute(
							IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
							"org.eclipse.pde.ui.workbenchClasspathProvider");
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
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		configuration.setAttribute(
			IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
			"org.eclipse.pde.ui.workbenchClasspathProvider");
	}

}
