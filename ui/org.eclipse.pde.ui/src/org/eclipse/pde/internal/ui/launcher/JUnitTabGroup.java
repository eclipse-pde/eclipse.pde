package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 

import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.internal.junit.launcher.JUnitMainTab;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.launcher.ConfigurationTab;
import org.eclipse.pde.ui.launcher.PluginJUnitMainTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

public class JUnitTabGroup extends AbstractPDELaunchConfigurationTabGroup {
	
	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = null;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			tabs = new ILaunchConfigurationTab[]{new JUnitMainTab(),
					new PluginJUnitMainTab(), new JavaArgumentsTab(),
					new PluginsTab(false),	
					 new ConfigurationTab(true), new TracingTab(),
					new EnvironmentTab(), new SourceLookupTab(), 
					new CommonTab()};
		} else {
			tabs = new ILaunchConfigurationTab[]{new JUnitMainTab(),
					new PluginJUnitMainTab(), 
					new JavaArgumentsTab(),
					new PluginsTab(false),
					new TracingTab(), new EnvironmentTab(),
					new SourceLookupTab(), new CommonTab()};
		}
		setTabs(tabs);
	}

}
