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
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;

public class PluginPathFinder {
	private static final String URL_PROPERTY = "org.eclipse.update.resolution_url"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String DROPINS = "dropins"; //$NON-NLS-1$
	private static final String LINK = ".link"; //$NON-NLS-1$
	private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$

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
				path += IPath.SEPARATOR + ECLIPSE + IPath.SEPARATOR;
				if (features)
					path += IPDEBuildConstants.DEFAULT_FEATURE_LOCATION;
				else
					path += IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION;
				if (new File(path).exists()) {
					return path;
				}
			}
		} catch (IOException e) {
			//ignore
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
		ArrayList sites = new ArrayList();

		File file = new File(platformHome, features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
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
		return (File[]) sites.toArray(new File[sites.size()]);
	}

	private static List getDropins(String platformHome, boolean features) {
		File dropins = new File(platformHome, DROPINS);
		if (!dropins.exists())
			return Collections.EMPTY_LIST;

		ArrayList sites = new ArrayList();
		ArrayList results = new ArrayList();

		File[] contents = dropins.listFiles();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isFile()) {
				if (contents[i].getName().endsWith(LINK)) {
					String path = getSitePath(platformHome, contents[i], features);
					if (path != null)
						sites.add(new File(path));
				} else {
					//bundle
					results.add(contents[i]);
				}
			} else { //folder
				//dropins/features or dropins/plugins
				if (contents[i].isDirectory() && contents[i].getName().equals(features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION)) {
					results.addAll(Arrays.asList(contents[i].listFiles()));
					continue;
				}

				//dropins/*/features or dropins/*/plugins
				File temp = new File(contents[i], features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
				if (temp.isDirectory()) {
					sites.add(temp);
					continue;
				}

				//dropins/*/eclipse/features or dropins/*/eclipse/plugins
				temp = new File(contents[i], ECLIPSE + File.separator + (features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION));
				if (temp.isDirectory()) {
					sites.add(temp);
					continue;
				}

				//else treat as a bundle/feature
				results.add(contents[i]);
			}
		}

		results.addAll(scanLocations((File[]) sites.toArray(new File[sites.size()])));
		return results;
	}

	public static File[] getFeaturePaths(String platformHome) {
		return getPaths(platformHome, true, false);
	}

	public static File[] getPluginPaths(String platformHome, boolean filterP2Base) {
		return getPaths(platformHome, false, filterP2Base);
	}

	public static File[] getPluginPaths(String platformHome) {
		return getPaths(platformHome, false, false);
	}

	public static File[] getPaths(String platformHome, boolean features, boolean filterP2Base) {

		if (filterP2Base) {
			URL[] urls = P2Utils.readBundlesTxt(platformHome);
			if (urls != null && urls.length > 0) {
				return Utils.asFile(urls);
			}
		}

		File file = new File(platformHome, "configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		if (file.exists()) {
			try {
				String value = new Path(platformHome).toFile().toURL().toExternalForm();
				System.setProperty(URL_PROPERTY, value);
				try {
					IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
					return getConfiguredSitesPaths(platformHome, config, features);
				} finally {
					System.setProperty(URL_PROPERTY, EMPTY_STRING);
				}
			} catch (MalformedURLException e) {
				//ignore
			} catch (IOException e) {
				//ignore
			}
		}

		List list = scanLocations(getSites(platformHome, features));
		list.addAll(getDropins(platformHome, features));
		return Utils.asFile(list);
	}

	private static File[] getConfiguredSitesPaths(String platformHome, IPlatformConfiguration configuration, boolean features) {
		List installPlugins = scanLocations(new File[] {new File(platformHome, features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION)});
		List extensionPlugins = getExtensionPlugins(configuration, features);
		List dropinsPlugins = getDropins(platformHome, features);

		Set all = new LinkedHashSet();
		all.addAll(installPlugins);
		all.addAll(extensionPlugins);
		all.addAll(dropinsPlugins);

		return (File[]) all.toArray(new File[all.size()]);
	}

	/**
	 * 
	 * @param config
	 * @param features true for features false for plugins
	 * @return List of Files for features or plugins on the site
	 */
	private static List getExtensionPlugins(IPlatformConfiguration config, boolean features) {
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
					extensionPlugins.add(new File(url.getFile(), entries[j]));
				}
			}
		}
		return extensionPlugins;
	}

	/**
	 * Scan given plugin/feature directories or jars for existence
	 * @param sites
	 * @return URLs to plugins/features
	 */
	private static List scanLocations(File[] sites) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < sites.length; i++) {
			if (sites[i] == null || !sites[i].exists())
				continue;
			File[] children = sites[i].listFiles();
			if (children != null)
				result.addAll(Arrays.asList(children));
		}
		return result;
	}
}
