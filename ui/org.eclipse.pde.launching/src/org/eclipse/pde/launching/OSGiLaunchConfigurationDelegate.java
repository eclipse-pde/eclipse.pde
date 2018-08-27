/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.launching.*;
import org.eclipse.pde.internal.launching.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;

/**
 * A launch delegate for launching OSGi frameworks
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * <p>
 * This class originally existed in 3.3 as
 * <code>org.eclipse.pde.ui.launcher.OSGiLaunchConfigurationDelegate</code>.
 * </p>
 * @since 3.6
 */
public class OSGiLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	/**
	 * Delegates to the launcher delegate associated with the OSGi framework
	 * selected in the launch configuration.
	 *
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		OSGiFrameworkManager manager = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager();
		String id = configuration.getAttribute(IPDELauncherConstants.OSGI_FRAMEWORK_ID, manager.getDefaultFramework());
		LaunchConfigurationDelegate launcher = manager.getFrameworkLauncher(id);
		if (launcher != null) {
			launcher.launch(configuration, mode, launch, monitor);
		} else {
			String name = manager.getFrameworkName(id);
			if (name == null)
				name = PDEMessages.OSGiLaunchConfiguration_selected;
			String message = NLS.bind(PDEMessages.OSGiLaunchConfiguration_cannotFindLaunchConfiguration, name);
			IStatus status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, IStatus.OK, message, null);
			throw new CoreException(status);
		}
	}

	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		return computeBuildOrder(LaunchPluginValidator.getAffectedProjects(configuration));
	}

}
