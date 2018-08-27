/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.pde.launching.IPDELauncherConstants;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut capable of launching an OSGi frameowrk
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class OSGiLaunchShortcut extends AbstractLaunchShortcut {

	@Override
	public void launch(ISelection selection, String mode) {
		launch(mode);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		launch(mode);
	}

	@Override
	protected String getLaunchConfigurationTypeName() {
		return IPDELauncherConstants.OSGI_CONFIGURATION_TYPE;
	}

	/**
	 * Delegates to the initializer associated with the selected OSGI framework
	 * to initialize the launch configuration
	 * <p>
	 * Refer to the <code>org.eclipse.pde.ui.osgiFrameworks</code> extension point.
	 * </p>
	 * @see org.eclipse.pde.ui.launcher.AbstractLaunchShortcut#initializeConfiguration(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	protected void initializeConfiguration(ILaunchConfigurationWorkingCopy configuration) {
		OSGiFrameworkManager manager = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager();
		manager.getDefaultInitializer().initialize(configuration);
	}

	@Override
	protected boolean isGoodMatch(ILaunchConfiguration configuration) {
		return true;
	}

}
