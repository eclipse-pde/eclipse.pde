/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 525701
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("deprecation")
// PDE still supports searching the platform.xml for plug-in/feature listings
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
				if (!new Path(path).isAbsolute()) {
					path = prefix + IPath.SEPARATOR + path;
				}
				path += IPath.SEPARATOR + "eclipse" + IPath.SEPARATOR; //$NON-NLS-1$
				if (features) {
					path += "features"; //$NON-NLS-1$
				} else {
					path += "plugins"; //$NON-NLS-1$
				}
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
		if (!features && !file.exists()) {
			file = new File(platformHome);
		}
		if (file.exists()) {
			sites.add(file);
		}

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

	public static URL[] getFeaturePaths(String platformHome) {
		return scanLocations(getSites(platformHome, true));
	}

	/**
	 * Scan given plugin/feature directories or jars for existence
	 * @param sites
	 * @return URLs to plugins/features
	 */
	public static URL[] scanLocations(File[] sites) {
		HashSet<URL> result = new HashSet<>();
		for (int i = 0; i < sites.length; i++) {
			if (!sites[i].exists()) {
				continue;
			}
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
}
