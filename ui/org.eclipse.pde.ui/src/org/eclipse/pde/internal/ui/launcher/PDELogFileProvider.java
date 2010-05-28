/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.pde.internal.launching.launcher.LaunchListener;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.ui.internal.views.log.ILogFileProvider;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Provides list of log files for PDE Launch Configurations.
 */
public class PDELogFileProvider implements ILogFileProvider {

	/**
	 * Returns most recent log files for all PDE Launch Configurations.
	 * 
	 * @see ILogFileProvider#getLogSources()
	 * @since 3.4 
	 */
	public Map getLogSources() {
		ILaunchConfiguration[] configurations = null;
		try {
			configurations = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
		} catch (CoreException e) {
			StatusManager.getManager().handle(e.getStatus());
			return Collections.EMPTY_MAP;
		}

		Map sources = new HashMap();

		for (int i = 0; i < configurations.length; i++) {
			ILaunchConfiguration configuration = configurations[i];

			ILaunchConfigurationType type;
			try {
				type = configuration.getType();
				if (EclipseLaunchShortcut.CONFIGURATION_TYPE.equals(type.getIdentifier())) {
					String name = configuration.getName();
					File configFile = LaunchListener.getMostRecentLogFile(configuration);

					if (configFile != null) {
						sources.put(name, configFile.getCanonicalPath());
					}
				}

			} catch (CoreException e) {
				StatusManager.getManager().handle(e.getStatus());
			} catch (IOException e) { // do nothing
			}
		}

		return sources;
	}

}
