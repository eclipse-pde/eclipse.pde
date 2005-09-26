/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.launcher.ConfigurationTab;
import org.eclipse.pde.ui.launcher.MainTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

public class EclipseApplicationLauncherTabGroup extends AbstractPDELaunchConfigurationTabGroup {

	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog,
	 *      String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = null;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			tabs = new ILaunchConfigurationTab[]{new MainTab(),
					new JavaArgumentsTab(),
					new PluginsTab(), new ConfigurationTab(),
					new TracingTab(), new EnvironmentTab(),
					new SourceLookupTab(), new CommonTab()};
		} else {
			tabs = new ILaunchConfigurationTab[]{new MainTab(),
					new JavaArgumentsTab(),
					new PluginsTab(), new TracingTab(),
					new EnvironmentTab(), new SourceLookupTab(), 
					new CommonTab()};
		}
		setTabs(tabs);
	}

}
