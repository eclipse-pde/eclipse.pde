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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.pde.internal.core.update.configurator.PlatformConfiguration;
import org.eclipse.pde.internal.core.update.configurator.SiteEntry;

@SuppressWarnings("deprecation")
// PDE still supports searching the platform.xml for plug-in/feature listings
public class PluginPathFinder {

	private static final String URL_PROPERTY = "org.eclipse.update.resolution_url"; //$NON-NLS-1$

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
		File file = getPlatformFile(platformHome);
		if (file != null) {
			try {
				String value = new Path(platformHome).toFile().toURL().toExternalForm();
				System.setProperty(URL_PROPERTY, value);
				try {
					PlatformConfiguration config = new PlatformConfiguration(file.toURL());
					return getConfiguredSitesPaths(platformHome, config);
				} finally {
					System.setProperty(URL_PROPERTY, ""); //$NON-NLS-1$
				}
			} catch (Exception e) {
				PDECore.log(e);
			}
		}
		return scanLocations(getSites(platformHome, true));
	}

	/**
	 * Returns a File object representing the platform.xml or null if the file
	 * cannot be found.
	 *
	 * @return File representing platform.xml or <code>null</code>
	 */
	private static File getPlatformFile(String platformHome) {
		String location = System.getProperty("org.eclipse.pde.platform_location"); //$NON-NLS-1$
		File file = null;
		if (location != null) {
			try {
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				location = manager.performStringSubstitution(location);
				Path path = new Path(location);
				if (path.isAbsolute()) {
					file = path.toFile();
				} else {
					file = new File(platformHome, location);
				}
				if (file.exists()) {
					return file;
				}
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
		file = new File(platformHome, "configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		return file.exists() ? file : null;
	}

	private static URL[] getConfiguredSitesPaths(String platformHome, PlatformConfiguration configuration) {
		URL[] installPlugins = scanLocations(new File[] { new File(platformHome, "features") }); //$NON-NLS-1$
		URL[] extensionPlugins = getExtensionPluginURLs(configuration);

		URL[] all = new URL[installPlugins.length + extensionPlugins.length];
		System.arraycopy(installPlugins, 0, all, 0, installPlugins.length);
		System.arraycopy(extensionPlugins, 0, all, installPlugins.length, extensionPlugins.length);
		return all;
	}

	/**
	 *
	 * @param config
	 * @return URLs for features or plugins on the site
	 */
	private static URL[] getExtensionPluginURLs(PlatformConfiguration config) {
		ArrayList<URL> extensionPlugins = new ArrayList<>();
		SiteEntry[] sites = config.getConfiguredSites();
		for (SiteEntry site : sites) {
			URL url = site.getURL();
			if ("file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
				String[] entries = site.getFeatures();
				for (String entry : entries) {
					try {
						extensionPlugins.add(new File(url.getFile(), entry).toURL());
					} catch (MalformedURLException e) {
					}
				}
			}
		}
		return extensionPlugins.toArray(new URL[extensionPlugins.size()]);
	}

	/**
	 * Scan given plugin/feature directories or jars for existence
	 *
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
