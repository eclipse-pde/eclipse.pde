/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.pde.internal.ui.*;

public class JUnitArgumentsTab extends BasicLauncherTab {
	
	protected String getApplicationAttribute() {
		return APP_TO_TEST;
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(
			LOCATION + "0", //$NON-NLS-1$
			LauncherUtils.getDefaultPath().append("runtime-test-workspace").toOSString()); //$NON-NLS-1$
		config.setAttribute(DOCLEAR, true);
		config.setAttribute(ASKCLEAR, false);
		config.setAttribute(PROGARGS, ""); //$NON-NLS-1$
		config.setAttribute(VMARGS, ""); //$NON-NLS-1$
		config.setAttribute(BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$
		if (!JUnitLaunchConfiguration.requiresUI(config))
			config.setAttribute(APPLICATION, JUnitLaunchConfiguration.CORE_APPLICATION);
	}
	
	protected String[] getApplicationNames() {
		TreeSet result = new TreeSet();
		result.add(PDEPlugin.getResourceString("JUnitArgumentsTab.headless")); //$NON-NLS-1$
		String[] appNames = super.getApplicationNames();
		for (int i = 0; i < appNames.length; i++) {
			result.add(appNames[i]);
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
			fApplicationCombo.setText(PDEPlugin.getResourceString("JUnitArgumentsTab.headless")); //$NON-NLS-1$
		else
			super.initializeApplicationSection(config);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#saveApplicationSection(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		if (fApplicationCombo.getText().equals(PDEPlugin.getResourceString("JUnitArgumentsTab.headless"))) { //$NON-NLS-1$
			config.setAttribute(APPLICATION, JUnitLaunchConfiguration.CORE_APPLICATION);
			config.setAttribute(APP_TO_TEST, (String)null);
		} else {
			config.setAttribute(APPLICATION, (String)null);
			super.saveApplicationSection(config);
		}
	}
	

	
}
