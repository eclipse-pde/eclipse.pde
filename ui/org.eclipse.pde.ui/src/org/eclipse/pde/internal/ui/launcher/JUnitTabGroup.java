package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.*;
import org.eclipse.jdt.internal.junit.launcher.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;

public class JUnitTabGroup extends AbstractLaunchConfigurationTabGroup {
	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = null;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			tabs = new ILaunchConfigurationTab[]{new JUnitMainTab(),
					new JUnitArgumentsTab(), new AdvancedLauncherTab(false),
					new NewTracingLauncherTab(), new ConfigurationTab(),
					new JavaSourceLookupTab(), new EnvironmentTab(),
					new CommonTab()};
		} else {
			tabs = new ILaunchConfigurationTab[]{new JUnitMainTab(),
					new JUnitArgumentsTab(), new AdvancedLauncherTab(false),
					new NewTracingLauncherTab(), new JavaSourceLookupTab(),
					new EnvironmentTab(), new CommonTab()};
		}
		setTabs(tabs);
	}

	/**
	 * @see ILaunchConfigurationTabGroup#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config); 
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.eclipse.pde.ui.workbenchClasspathProvider");
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

}
