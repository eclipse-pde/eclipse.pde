/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class WorkbenchLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog,
	 *      String)
	 */
public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = null;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			tabs = new ILaunchConfigurationTab[]{new BasicLauncherTab(),
					new AdvancedLauncherTab(), new ConfigurationTab(),
					new TracingLauncherTab(), new EnvironmentTab(),
					new SourceLookupTab(), new CommonTab()};
		} else {
			tabs = new ILaunchConfigurationTab[]{new BasicLauncherTab(),
					new AdvancedLauncherTab(), new TracingLauncherTab(),
					new EnvironmentTab(), new SourceLookupTab(), 
					new CommonTab()};
		}
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
							"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
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
			"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
	}

}
