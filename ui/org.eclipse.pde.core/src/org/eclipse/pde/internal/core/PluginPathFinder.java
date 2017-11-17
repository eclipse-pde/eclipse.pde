/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 525701
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.core.plugin.TargetPlatform;

@SuppressWarnings("deprecation")
public class PluginPathFinder {

	/**
	 *
	 * @param platformHome
	 * @param linkFile
	 * @param features false for plugins, true for features
	 * @return path of plugins or features directory of an extension site
	 */
	private static String getSitePath(String platformHome, File linkFile, boolean features) {
		String prefix = new Path(platformHome).removeLastSegments(1).toString();
		Properties properties = new Properties();
		try (FileInputStream fis = new FileInputStream(linkFile)) {
			properties.load(fis);
			String path = properties.getProperty("path"); //$NON-NLS-1$
			if (path != null) {
				if (!new Path(path).isAbsolute())
					path = prefix + IPath.SEPARATOR + path;
				path += IPath.SEPARATOR + "eclipse" + IPath.SEPARATOR; //$NON-NLS-1$
				if (features)
					path += "features"; //$NON-NLS-1$
				else
					path += "plugins"; //$NON-NLS-1$
				if (new File(path).exists()) {
					return path;
				}
			}
		} catch (IOException e) {
		}
		return null;
	}

	/**
	 *
	 * @param platformHome
	 * @param features false for plugin sites, true for feature sites
	 * @return array of ".../plugins" or ".../features" Files
	 */
	private static File[] getSites(String platformHome, boolean features) {
		HashSet<File> sites = new HashSet<>();
		File file = new File(platformHome, features ? "features" : "plugins"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!features && !file.exists())
			file = new File(platformHome);
		if (file.exists())
			sites.add(file);

		File[] linkFiles = new File(platformHome + IPath.SEPARATOR + "links").listFiles(); //$NON-NLS-1$
		if (linkFiles != null) {
			for (File linkFile : linkFiles) {
				String path = getSitePath(platformHome, linkFile, features);
				if (path != null) {
					sites.add(new File(path));
				}
			}
		}

		// If there is no features/plugins folder and no linked files, try the home location
		if (sites.isEmpty()) {
			file = new File(platformHome);
			if (file.exists()) {
				sites.add(file);
			}
		}

		return sites.toArray(new File[sites.size()]);
	}

	/**
	 * Attempts to find all plugin paths if the target platform was at the given string location.
	 * <p>
	 * Should not be called in PDE. It should only be used to confirm test results match the
	 * old way of doing things (before ITargetPlatformService).
	 * </p>
	 *
	 * @param platformHome the target platform location
	 * @param installedOnly whether to check for a bundles.info or another configuration file to
	 * 		determine what bundles are installed rather than what bundles simply exist in the plugins folder
	 * @return list of URL plug-in locations
	 */
	public static URL[] getPluginPaths(String platformHome, boolean installedOnly) {
		// If we don't care about installed bundles, simply scan the location
		if (!installedOnly)
			return scanLocations(getSites(platformHome, false));

		// See if we can find a bundles.info to get installed bundles from
		URL[] urls = null;
		if (new Path(platformHome).equals(new Path(TargetPlatform.getDefaultLocation()))) {
			// Pointing at default install, so use the actual configuration area
			Location configArea = Platform.getConfigurationLocation();

			if (configArea != null) {
				urls = P2Utils.readBundlesTxt(platformHome, configArea.getURL());

				// try the shared location (parent)
				if (urls == null && configArea.getParentLocation() != null) {
					urls = P2Utils.readBundlesTxt(platformHome, configArea.getParentLocation().getURL());
				}
			}

		} else {
			// Pointing at a folder, so try to guess the configuration area
			File configurationArea = new File(platformHome, "configuration"); //$NON-NLS-1$
			if (configurationArea.exists()) {
				try {
					urls = P2Utils.readBundlesTxt(platformHome, configurationArea.toURL());
				} catch (MalformedURLException e) {
					PDECore.log(e);
				}
			}
		}
		if (urls != null) {
			return urls;
		}

		return getPlatformXMLPaths(platformHome, false);
	}

	public static URL[] getFeaturePaths(String platformHome) {
		return getPlatformXMLPaths(platformHome, true);
	}

	/**
	 * Returns a list of file URLs for plug-ins or features found in the default
	 * directory ("plugins"/"features").
	 *
	 * @param platformHome
	 *            base location for the installation, used to search for
	 *            plugins/features
	 * @param findFeatures
	 *            if <code>true</code> will return paths to features, otherwise will
	 *            return paths to plug-ins.
	 * @return a list of URL paths to plug-ins or features. Possibly empty if the
	 *         default directory had no valid files
	 */
	public static URL[] getPlatformXMLPaths(String platformHome, boolean findFeatures) {
		return scanLocations(getSites(platformHome, findFeatures));
	}

	/**
	 * Scan given plugin/feature directories or jars for existence
	 * @param sites
	 * @return URLs to plugins/features
	 */
	public static URL[] scanLocations(File[] sites) {
		HashSet<URL> result = new HashSet<>();
		for (int i = 0; i < sites.length; i++) {
			if (!sites[i].exists())
				continue;
			File[] children = sites[i].listFiles();
			if (children != null) {
				for (File element : children) {
					try {
						result.add(element.toURL());
					} catch (MalformedURLException e) {
					}
				}
			}
		}
		return result.toArray(new URL[result.size()]);
	}

	public static boolean isDevLaunchMode() {
		if (Boolean.getBoolean("eclipse.pde.launch")) //$NON-NLS-1$
			return true;
		String[] args = Platform.getApplicationArgs();
		for (String arg : args) {
			if (arg.equals("-pdelaunch")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

}
