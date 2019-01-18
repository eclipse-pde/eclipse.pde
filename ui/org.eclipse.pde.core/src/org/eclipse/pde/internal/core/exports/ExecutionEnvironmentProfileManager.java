/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.osgi.framework.Bundle;

/**
 * Generates profiles for custom execution environments. Custom profiles are
 * generated on demand, once per workspace invocation (since EE's are extensible,
 * the set of EE's can change from time to time).
 */
public class ExecutionEnvironmentProfileManager {

	/**
	 * Location in the local file system where custom profiles are stored
	 */
	private static final IPath PROFILE_PATH = PDECore.getDefault().getStateLocation().append(".profiles"); //$NON-NLS-1$

	/**
	 * Number of custom profiles or -1 if not yet initialized.
	 */
	private static int fgCustomCount = -1;

	/**
	 * Array of locations where custom profiles are stored.
	 */
	private static final String[] LOCATIONS = new String[] {PROFILE_PATH.toOSString()};

	/**
	 * Returns absolute paths in the local file systems of directories and jars containing
	 * OSGi profile property files for custom execution environments, or <code>null</code>
	 * if none. This is in addition to the standard profiles known by <code>org.eclipse.osgi</code>.
	 *
	 * @return locations (directories and jars) containing custom execution environment
	 * profile files, or <code>null</code> if none
	 */
	public static String[] getCustomProfileLocations() {
		initialize();
		if (fgCustomCount > 0) {
			return LOCATIONS;
		}
		return null;
	}

	/**
	 * Generates profile files for custom execution environments (i.e. those not provided
	 * by OSGi framework).
	 */
	private static synchronized void initialize() {
		if (fgCustomCount == -1) {
			fgCustomCount = 0;
			// clean any existing profiles
			File dir = PROFILE_PATH.toFile();
			if (!dir.exists()) {
				dir.mkdir();
			}
			File[] files = dir.listFiles();
			for (File file : files) {
				file.delete();
			}
			// create current profiles
			Bundle bundle = Platform.getBundle(IPDEBuildConstants.BUNDLE_OSGI);
			IExecutionEnvironment[] environments = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			for (IExecutionEnvironment env : environments) {
				String path = env.getId().replace('/', '_') + ".profile"; //$NON-NLS-1$
				URL entry = bundle.getEntry(path);
				if (entry == null) {
					// custom entry if not contained in OSGi bundle
					Properties properties = env.getProfileProperties();
					if (properties != null) {
						File profile = new File(dir, path);
						try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(profile));) {
							fgCustomCount++;
							properties.store(stream, null);
						} catch (IOException e) {
							PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(PDECoreMessages.ExecutionEnvironmentProfileManager_0, env.getId()), e));
						}
					}
				}
			}

		}
	}
}
