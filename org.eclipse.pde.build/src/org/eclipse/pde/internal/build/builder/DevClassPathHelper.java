/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

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
				devDefaultClasspath = Utils.getArrayFromString(devProperties.getProperty("*"));
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
				is.close();
			}
		} catch (IOException e) {
			String message = Policy.bind("exception.missingFile", url.toExternalForm()); //$NON-NLS-1$
			BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, message, null));
		}
		return props;
	}
}