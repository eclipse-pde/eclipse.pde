/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;

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
		try {
			FileInputStream fis = new FileInputStream(linkFile);
			properties.load(fis);
			fis.close();
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
		HashSet sites = new HashSet();
		File file = new File(platformHome, features ? "features" : "plugins"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!features && !file.exists())
			file = new File(platformHome);
		if (file.exists())
			sites.add(file);

		File[] linkFiles = new File(platformHome + IPath.SEPARATOR + "links").listFiles(); //$NON-NLS-1$	
		if (linkFiles != null) {
			for (int i = 0; i < linkFiles.length; i++) {
				String path = getSitePath(platformHome, linkFiles[i], features);
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

		return (File[]) sites.toArray(new File[sites.size()]);
	}

	public static URL[] getPluginPaths(String platformHome) {
		// If we don't care about installed bundles, simply scan the location
		PDEPreferencesManager store = PDECore.getDefault().getPreferencesManager();
		if (!store.getBoolean(ICoreConstants.TARGET_PLATFORM_REALIZATION))
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

		if (new Path(platformHome).equals(new Path(TargetPlatform.getDefaultLocation())) && !isDevLaunchMode())
			return ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();

		File file = getPlatformFile(platformHome);
		if (file != null) {
			try {
				String value = new Path(platformHome).toFile().toURL().toExternalForm();
				System.setProperty(URL_PROPERTY, value);
				try {
					IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
					return getConfiguredSitesPaths(platformHome, config, false);
				} finally {
					System.setProperty(URL_PROPERTY, EMPTY_STRING);
				}
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}
		return scanLocations(getSites(platformHome, false));
	}

	/*
	 * Returns a File object representing the platform.xml or null if the file cannot be found.
	 */
	private static File getPlatformFile(String platformHome) {
		String location = System.getProperty("org.eclipse.pde.platform_location"); //$NON-NLS-1$
		File file = null;
		if (location != null) {
			try {
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				location = manager.performStringSubstitution(location);
				Path path = new Path(location);
				if (path.isAbsolute())
					file = path.toFile();
				else
					file = new File(platformHome, location);
				if (file.exists())
					return file;
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
		file = new File(platformHome, "configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		return file.exists() ? file : null;
	}

	public static URL[] getFeaturePaths(String platformHome) {
		File file = getPlatformFile(platformHome);
		if (file != null) {
			try {
				String value = new Path(platformHome).toFile().toURL().toExternalForm();
				System.setProperty(URL_PROPERTY, value);
				try {
					IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
					return getConfiguredSitesPaths(platformHome, config, true);
				} finally {
					System.setProperty(URL_PROPERTY, EMPTY_STRING);
				}
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}
		return scanLocations(getSites(platformHome, true));
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
		ArrayList extensionPlugins = new ArrayList();
		IPlatformConfiguration.ISiteEntry[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			URL url = sites[i].getURL();
			if ("file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
				String[] entries;
				if (features)
					entries = sites[i].getFeatures();
				else
					entries = sites[i].getPlugins();
				for (int j = 0; j < entries.length; j++) {
					try {
						extensionPlugins.add(new File(url.getFile(), entries[j]).toURL());
					} catch (MalformedURLException e) {
					}
				}
			}
		}
		return (URL[]) extensionPlugins.toArray(new URL[extensionPlugins.size()]);
	}

	/**
	 * Scan given plugin/feature directores or jars for existance
	 * @param sites
	 * @return URLs to plugins/features
	 */
	public static URL[] scanLocations(File[] sites) {
		HashSet result = new HashSet();
		for (int i = 0; i < sites.length; i++) {
			if (!sites[i].exists())
				continue;
			File[] children = sites[i].listFiles();
			if (children != null) {
				for (int j = 0; j < children.length; j++) {
					try {
						result.add(children[j].toURL());
					} catch (MalformedURLException e) {
					}
				}
			}
		}
		return (URL[]) result.toArray(new URL[result.size()]);
	}

	public static boolean isDevLaunchMode() {
		if (Boolean.getBoolean("eclipse.pde.launch")) //$NON-NLS-1$
			return true;
		String[] args = Platform.getApplicationArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-pdelaunch")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

}
