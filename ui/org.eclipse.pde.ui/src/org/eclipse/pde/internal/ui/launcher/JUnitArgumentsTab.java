package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.core.*;

public class JUnitArgumentsTab extends BasicLauncherTab {
	
	protected String getApplicationAttribute() {
		return APP_TO_TEST;
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(
			LOCATION + "0",
			LauncherUtils.getDefaultPath().append("runtime-test-workspace").toOSString());
		config.setAttribute(DOCLEAR, true);
		config.setAttribute(ASKCLEAR, false);
		config.setAttribute(PROGARGS, LauncherUtils.getDefaultProgramArguments());
		config.setAttribute(VMARGS, "");
	}
	
}
