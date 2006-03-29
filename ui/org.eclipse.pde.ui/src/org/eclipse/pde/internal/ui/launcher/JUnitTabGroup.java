package org.eclipse.pde.internal.ui.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;
import org.eclipse.jdt.internal.junit.launcher.JUnitMainTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
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
		tabs = new ILaunchConfigurationTab[]{
				new JUnitMainTab(),
				new PluginJUnitMainTab(), 
				new JavaArgumentsTab(),
				new PluginsTab(false),	
				new ConfigurationTab(true), 
				new TracingTab(),
				new EnvironmentTab(), 
				new CommonTab()};
		setTabs(tabs);
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		
		String vmArgs;
		try {
			vmArgs= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); //$NON-NLS-1$
		} catch (CoreException e) {
			vmArgs= ""; //$NON-NLS-1$
		}
		vmArgs = AssertionVMArg.enableAssertInArgString(vmArgs);
		if (vmArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}

}
