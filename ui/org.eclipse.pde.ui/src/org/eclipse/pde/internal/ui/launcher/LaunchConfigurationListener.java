/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import org.eclipse.debug.core.*;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class LaunchConfigurationListener implements ILaunchConfigurationListener {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
		File configDir = LaunchConfigurationHelper.getConfigurationLocation(configuration);
		if (configDir.exists()) {
			// rename the config area if it was auto-set by PDE when the launch configuration is renamed
			ILaunchConfiguration destination = DebugPlugin.getDefault().getLaunchManager().getMovedTo(configuration);
			boolean delete = true;
			if (destination != null) {
				delete = !configDir.renameTo(LaunchConfigurationHelper.getConfigurationLocation(destination));
			}
			if (delete)
				CoreUtility.deleteContent(configDir);
		}
	}

}
