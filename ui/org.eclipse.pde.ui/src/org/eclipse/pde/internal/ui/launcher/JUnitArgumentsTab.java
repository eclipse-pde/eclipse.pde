package org.eclipse.pde.internal.ui.launcher;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

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
		config.setAttribute(BOOTSTRAP_ENTRIES, "");
		if (!JUnitLaunchConfiguration.requiresUI(config))
			config.setAttribute(APPLICATION, JUnitLaunchConfiguration.CORE_APPLICATION);
	}
	
	protected String[] getApplicationNames() {
		TreeSet result = new TreeSet();
		result.add("[No Application] - Headless Mode");
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.applications")) {
					String id = extensions[j].getPluginBase().getId() + "." + extensions[j].getId();
					if (id != null && !id.startsWith("org.eclipse.pde.junit.runtime")){
						result.add(id);
					}
				}
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#initializeApplicationSection(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	protected void initializeApplicationSection(ILaunchConfiguration config)
			throws CoreException {
		String application = config.getAttribute(APPLICATION, (String)null);
		if (JUnitLaunchConfiguration.CORE_APPLICATION.equals(application))
			fApplicationCombo.setText(fApplicationCombo.getItem(0));
		else
			super.initializeApplicationSection(config);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#saveApplicationSection(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		if (fApplicationCombo.getSelectionIndex() == 0)
			config.setAttribute(APPLICATION, JUnitLaunchConfiguration.CORE_APPLICATION);
		else {
			config.setAttribute(APPLICATION, (String)null);
			super.saveApplicationSection(config);
		}
	}
	

	
}
