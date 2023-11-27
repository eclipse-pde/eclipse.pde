/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
 *     James D Miles (IBM Corp.) - bug 176250, Configurator needs to handle more platform urls
 *******************************************************************************/
package org.eclipse.pde.internal.core.update.configurator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

class Utils {
	// os
	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$

	public static IStatus newStatus(String message, Throwable e) {
		return Status.error(message, e);
	}

	public static void log(String message) {
		log(newStatus(message, null));
	}

	public static void log(IStatus status) {
		ILog.of(Utils.class).log(status);
	}

	public static boolean isValidEnvironment(String os, String ws, String arch, String nl) {
		if (os != null && !isMatching(os, getOS())) {
			return false;
		}
		if (ws != null && !isMatching(ws, getWS())) {
			return false;
		}
		if (arch != null && !isMatching(arch, getArch())) {
			return false;
		}
		if (nl != null && !isMatchingLocale(nl, getNL())) {
			return false;
		}
		return true;
	}

	/**
	 * Return the current operating system value.
	 *
	 * @see EnvironmentInfo#getOS()
	 */
	public static String getOS() {
		return Platform.getOS();
	}

	/**
	 * Return the current windowing system value.
	 *
	 * @see EnvironmentInfo#getWS()
	 */
	public static String getWS() {
		return Platform.getWS();
	}

	/**
	 * Return the current system architecture value.
	 *
	 * @see EnvironmentInfo#getOSArch()
	 */
	public static String getArch() {
		return Platform.getOSArch();
	}

	/**
	 * Return the current NL value.
	 *
	 * @see EnvironmentInfo#getNL()
	 */
	public static String getNL() {
		return Platform.getNL();
	}

	/**
	 * Return the configuration location.
	 *
	 * @see Location
	 */
	public static Location getConfigurationLocation() {
		return Platform.getConfigurationLocation();
	}

	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues == null) {
			return false;
		}
		if ("*".equalsIgnoreCase(candidateValues)) { //$NON-NLS-1$
			return true;
		}
		siteValues = siteValues.toUpperCase();
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (siteValues.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isMatchingLocale(String candidateValues, String locale) {
		if (locale == null) {
			return false;
		}
		if ("*".equalsIgnoreCase(candidateValues)) { //$NON-NLS-1$
			return true;
		}

		locale = locale.toUpperCase();
		candidateValues = candidateValues.toUpperCase();
		StringTokenizer stok = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String candidate = stok.nextToken();
			if (locale.indexOf(candidate) == 0) {
				return true;
			}
			if (candidate.indexOf(locale) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an absolute URL by combining a base absolute URL and another URL
	 * relative to the first one. If the relative URL protocol does not match
	 * the base URL protocol, or if the relative URL path is not relative,
	 * return it as is.
	 */
	public static URL makeAbsolute(URL base, URL relativeLocation) {
		if (!"file".equals(base.getProtocol())) { //$NON-NLS-1$
			// we only deal with file: URLs
			return relativeLocation;
		}
		if (relativeLocation.getProtocol() != null && !relativeLocation.getProtocol().equals(base.getProtocol())) {
			// it is not relative, return as is (avoid creating garbage)
			return relativeLocation;
		}
		IPath relativePath = IPath.fromOSString(relativeLocation.getPath());
		if (relativePath.isAbsolute()) {
			return relativeLocation;
		}
		try {
			IPath absolutePath = IPath.fromOSString(base.getPath()).append(relativeLocation.getPath());
			// File.toURL() is the best way to create a file: URL
			return absolutePath.toFile().toURL();
		} catch (MalformedURLException e) {
			// cannot happen since we are building from two existing valid URLs
			Utils.log(e.getLocalizedMessage());
			return relativeLocation;
		}
	}

	/**
	 * Ensures file: URLs on Windows have the right form (i.e. '/' as segment
	 * separator, drive letter in lower case, etc)
	 */
	public static String canonicalizeURL(String url) {
		if (!(isWindows && url.startsWith("file:"))) { //$NON-NLS-1$
			return url;
		}
		try {
			String path = new URL(url).getPath();
			// normalize to not have leading / so we can check the form
			File file = new File(path);
			path = file.toString().replace('\\', '/');
			if (Character.isUpperCase(path.charAt(0))) {
				char[] chars = path.toCharArray();
				chars[0] = Character.toLowerCase(chars[0]);
				path = new String(chars);
				return new File(path).toURL().toExternalForm();
			}
		} catch (MalformedURLException e) {
			// default to original url
		}
		return url;
	}

	/**
	 * Return the install location.
	 *
	 * @see Location
	 */
	public static URL getInstallURL() {
		Location location = Platform.getInstallLocation();

		// it is pretty much impossible for the install location to be null. If
		// it is, the
		// system is in a bad way so throw and exception and get the heck outta
		// here.
		if (location == null)
		 {
			throw new IllegalStateException("The installation location must not be null"); //$NON-NLS-1$
		}

		return location.getURL();
	}

}
