/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.ui.launcher.EquinoxSettingsTab;
import org.eclipse.pde.ui.launcher.EquinoxPluginsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

public class EquinoxLauncherTabGroup extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs =
			tabs = new ILaunchConfigurationTab[]{
					new EquinoxPluginsTab(),
					new JavaArgumentsTab(),
					new EquinoxSettingsTab(),
					new TracingTab(), 
					new EnvironmentTab(),
					new SourceLookupTab(), 
					new CommonTab()};

		setTabs(tabs);
	}

}
