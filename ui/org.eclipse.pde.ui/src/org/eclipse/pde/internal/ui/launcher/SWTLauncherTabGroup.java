package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.ui.*;
import org.eclipse.debug.ui.sourcelookup.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.*;

public class SWTLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new JavaMainTab(),
				new JavaArgumentsTab(),
				new JavaJRETab(),
				new JavaClasspathTab(),
				new SourceLookupTab(),
				new EnvironmentTab(),
				new CommonTab()
		};
		setTabs(tabs);
	}

}
