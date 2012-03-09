/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
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
		final File configDir = LaunchConfigurationHelper.getConfigurationLocation(configuration);
		if (configDir.exists()) {
			// rename the config area if it was auto-set by PDE when the launch configuration is renamed
			ILaunchConfiguration destination = DebugPlugin.getDefault().getLaunchManager().getMovedTo(configuration);
			boolean delete = true;
			if (destination != null) {
				delete = !configDir.renameTo(LaunchConfigurationHelper.getConfigurationLocation(destination));
			}
			// delete asynchronously in a job to avoid blocking calling thread
			if (delete) {
				Job job = new Job("Clean Configuration Data") { //$NON-NLS-1$
					protected IStatus run(IProgressMonitor monitor) {
						CoreUtility.deleteContent(configDir, monitor);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}
		}
	}

}
