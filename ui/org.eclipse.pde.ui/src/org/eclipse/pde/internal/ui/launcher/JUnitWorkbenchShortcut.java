package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.debug.ui.*;
import org.eclipse.jdt.internal.junit.launcher.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.internal.ui.*;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
public class JUnitWorkbenchShortcut extends JUnitLaunchShortcut {	
	
	/**
	 * Returns the local java launch config type
	 */
	protected ILaunchConfigurationType getJUnitLaunchConfigType() {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType("org.eclipse.pde.ui.JunitLaunchConfig");		
	}
	
	protected ILaunchConfiguration createConfiguration(
		IJavaProject project, String name, String mainType, String container, String testName) {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType= getJUnitLaunchConfigType();
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(name));
			wc.setAttribute(ILauncherSettings.LOCATION + "0", getDefaultWorkspaceLocation());
			wc.setAttribute(ILauncherSettings.VMARGS, "");
			wc.setAttribute(ILauncherSettings.PROGARGS, LauncherUtils.getDefaultProgramArguments());
			wc.setAttribute(ILauncherSettings.USECUSTOM, true);
			wc.setAttribute(ILauncherSettings.DOCLEAR, true);
			wc.setAttribute(ILauncherSettings.ASKCLEAR, false);
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				"org.eclipse.pde.ui.workbenchClasspathProvider");
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainType);
			wc.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, false);
			wc.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, container);
			if (testName.length() > 0)
				wc.setAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, testName);	
			config= wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.log(ce);
		}
		return config;
	}
	
	protected String getDefaultWorkspaceLocation() {
		return LauncherUtils.getDefaultPath().append("junit-workbench-workspace").toOSString();				
	}
	
}
