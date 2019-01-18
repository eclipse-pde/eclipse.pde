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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.configurator.IPlatformConfiguration.ISiteEntry;

@SuppressWarnings("deprecation")
// PDE still supports searching the platform.xml for plug-in/feature listings
public class PluginPathFinder {

	private static final String URL_PROPERTY = "org.eclipse.update.resolution_url"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

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
		if (!installedOnly) {
			return scanLocations(getSites(platformHome, false));
		}

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

		if (new Path(platformHome).equals(new Path(TargetPlatform.getDefaultLocation())) && !isDevLaunchMode()) {
			return ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();
		}

		return getPlatformXMLPaths(platformHome, false);
	}

	public static URL[] getFeaturePaths(String platformHome) {
		return getPlatformXMLPaths(platformHome, true);
	}

	/**
	 * Returns a list of file URLs for plug-ins or features found in a platform.xml file or in the default
	 * directory ("plugins"/"features") if no platform.xml is available.
	 *
	 * @param platformHome base location for the installation, used to search for platform.xml
	 * @param findFeatures if <code>true</code> will return paths to features, otherwise will return paths to plug-ins.
	 * @return a list of URL paths to plug-ins or features.  Possibly empty if the platform.xml had no entries or the default directory had no valid files
	 */
	public static URL[] getPlatformXMLPaths(String platformHome, boolean findFeatures) {
		File file = getPlatformFile(platformHome);
		if (file != null) {
			try {
				String value = new Path(platformHome).toFile().toURL().toExternalForm();
				System.setProperty(URL_PROPERTY, value);
				try {
					IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
					return getConfiguredSitesPaths(platformHome, config, findFeatures);
				} finally {
					System.setProperty(URL_PROPERTY, EMPTY_STRING);
				}
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}
		return scanLocations(getSites(platformHome, findFeatures));
	}

	/**
	 * Returns a File object representing the platform.xml or null if the file cannot be found.
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

	private static URL[] getConfiguredSitesPaths(String platformHome, IPlatformConfiguration configuration, boolean features) {
		URL[] installPlugins = scanLocations(new File[] {new File(platformHome, features ? "features" : "plugins")}); //$NON-NLS-1$ //$NON-NLS-2$
		URL[] extensionPlugins = getExtensionPluginURLs(configuration, features);

		URL[] all = new URL[installPlugins.length + extensionPlugins.length];
		System.arraycopy(installPlugins, 0, all, 0, installPlugins.length);
		System.arraycopy(extensionPlugins, 0, all, installPlugins.length, extensionPlugins.length);
		return all;
	}

	/**
	 *
	 * @param config
	 * @param features true for features false for plugins
	 * @return URLs for features or plugins on the site
	 */
	private static URL[] getExtensionPluginURLs(IPlatformConfiguration config, boolean features) {
		ArrayList<URL> extensionPlugins = new ArrayList<>();
		IPlatformConfiguration.ISiteEntry[] sites = config.getConfiguredSites();
		for (ISiteEntry site : sites) {
			URL url = site.getURL();
			if ("file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
				String[] entries;
				if (features) {
					entries = site.getFeatures();
				} else {
					entries = site.getPlugins();
				}
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

	public static boolean isDevLaunchMode() {
		if (Boolean.getBoolean("eclipse.pde.launch")) { //$NON-NLS-1$
			return true;
		}
		String[] args = Platform.getApplicationArgs();
		for (String arg : args) {
			if (arg.equals("-pdelaunch")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

}
