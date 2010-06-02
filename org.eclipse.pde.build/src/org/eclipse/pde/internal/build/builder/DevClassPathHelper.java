/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;

public class DevClassPathHelper {
	protected boolean inDevelopmentMode = false;
	protected String[] devDefaultClasspath;
	protected Properties devProperties = null;

	public DevClassPathHelper(String devInfo) {
		// Check the osgi.dev property to see if dev classpath entries have been defined.
		String osgiDev = devInfo;
		if (osgiDev != null) {
			try {
				inDevelopmentMode = true;
				URL location = new URL(osgiDev);
				devProperties = load(location);
				devDefaultClasspath = Utils.getArrayFromString(devProperties.getProperty("*")); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				devDefaultClasspath = Utils.getArrayFromString(osgiDev);
			}
		}
	}

	public String[] getDevClassPath(String id) {
		String[] result = null;
		if (id != null && devProperties != null) {
			String entry = devProperties.getProperty(id);
			if (entry != null)
				result = Utils.getArrayFromString(entry);
		}
		if (result == null)
			result = devDefaultClasspath;
		return result;
	}

	public boolean inDevelopmentMode() {
		return inDevelopmentMode;
	}

	/*
	 * Load the given properties file
	 */
	private static Properties load(URL url) {
		Properties props = new Properties();
		try {
			InputStream is = null;
			try {
				is = url.openStream();
				props.load(is);
			} finally {
				if (is != null)
					is.close();
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_missingFile, url.toExternalForm());
			BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, message, null));
		}
		return props;
	}
}
