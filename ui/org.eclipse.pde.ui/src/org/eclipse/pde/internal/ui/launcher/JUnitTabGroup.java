package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaSourceLookupTab;
import org.eclipse.jdt.internal.junit.launcher.JUnitMainTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class JUnitTabGroup extends AbstractLaunchConfigurationTabGroup {
	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new JUnitMainTab(),
			new JUnitArgumentsTab(),
			new AdvancedLauncherTab(false),
			new TracingLauncherTab(),
			new JavaSourceLookupTab(),
			new CommonTab()
		};
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
					if (tabs[i] instanceof AdvancedLauncherTab) {
						((AdvancedLauncherTab) tabs[i]).initialize(config);
					} else {
						tabs[i].initializeFrom(config);
					}
				}

			}
		});
	}

}
