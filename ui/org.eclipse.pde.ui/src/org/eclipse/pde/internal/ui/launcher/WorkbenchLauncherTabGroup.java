package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class WorkbenchLauncherTabGroup
	extends AbstractLaunchConfigurationTabGroup {

	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[4];
		tabs[0] = new BasicLauncherTab();
		tabs[0].setLaunchConfigurationDialog(dialog);
		tabs[1] = new AdvancedLauncherTab();
		tabs[1].setLaunchConfigurationDialog(dialog);
		tabs[2] = new TracingLauncherTab();
		tabs[2].setLaunchConfigurationDialog(dialog);
		tabs[3] = new CommonTab();
		tabs[3].setLaunchConfigurationDialog(dialog);
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
				for (int i = 0; i < tabs.length; i++) {
					if (tabs[i] instanceof AdvancedLauncherTab) {
						((AdvancedLauncherTab) tabs[i]).initialize(
							config);
					} else {
						tabs[i].initializeFrom(config);
					}
				}
			}
		});
	}
	
	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		ILaunchConfigurationTab[] tabs = getTabs();
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] instanceof AdvancedLauncherTab) {
				((AdvancedLauncherTab)tabs[i]).doPerformApply(configuration);
			} else {
				tabs[i].performApply(configuration);
			}
		}
	}


}
