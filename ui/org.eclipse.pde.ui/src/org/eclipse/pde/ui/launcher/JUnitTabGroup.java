/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.ui.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;

/**
 * Creates and initializes the tabs for the Plug-in JUnit test launch configuration.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class JUnitTabGroup extends AbstractPDELaunchConfigurationTabGroup {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = null;
		tabs = new ILaunchConfigurationTab[] {new TestTab(), new PluginJUnitMainTab(), new JavaArgumentsTab(), new PluginsTab(), new ConfigurationTab(true), new TracingTab(), new EnvironmentTab(), new CommonTab()};
		setTabs(tabs);
	}

}
