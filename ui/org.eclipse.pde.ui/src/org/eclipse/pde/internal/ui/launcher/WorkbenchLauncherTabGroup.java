package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.ui.*;

public class WorkbenchLauncherTabGroup
	extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[4];
		tabs[0]= new BasicLauncherTab();
		tabs[0].setLaunchConfigurationDialog(dialog);
		tabs[1]= new AdvancedLauncherTab(); 
		tabs[1].setLaunchConfigurationDialog(dialog);
		tabs[2]= new TracingLauncherTab();
		tabs[2].setLaunchConfigurationDialog(dialog);
		tabs[3]= new CommonTab();
		tabs[3].setLaunchConfigurationDialog(dialog);
		setTabs(tabs);
	}

}
