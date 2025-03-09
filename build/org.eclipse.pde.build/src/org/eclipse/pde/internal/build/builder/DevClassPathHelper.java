/*******************************************************************************
 * Copyright (c) 2004, 2024 IBM Corporation and others.
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
package org.eclipse.pde.internal.build.builder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Messages;
import org.eclipse.pde.internal.build.Utils;

public class DevClassPathHelper {
	protected boolean inDevelopmentMode = false;
	protected String[] devDefaultClasspath;
	protected Properties devProperties = null;

	public DevClassPathHelper(Path osgiDev) {
		if (osgiDev != null) {
			inDevelopmentMode = true;
			devProperties = load(osgiDev);
			devDefaultClasspath = Utils.getArrayFromString(devProperties.getProperty("*")); //$NON-NLS-1$
		}
	}

	public String[] getDevClassPath(String id) {
		String[] result = null;
		if (id != null && devProperties != null) {
			String entry = devProperties.getProperty(id);
			if (entry != null) {
				result = Utils.getArrayFromString(entry);
			}
		}
		if (result == null) {
			result = devDefaultClasspath;
		}
		return result;
	}

	public boolean inDevelopmentMode() {
		return inDevelopmentMode;
	}

	/*
	 * Load the given properties file
	 */
	private static Properties load(Path path) {
		Properties props = new Properties();
		try (InputStream is = Files.newInputStream(path)) {
			props.load(is);
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_missingFile, path);
			BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, message, null));
		}
		return props;
	}
}
