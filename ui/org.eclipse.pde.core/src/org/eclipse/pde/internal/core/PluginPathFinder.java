/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.update.configurator.*;

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
		ArrayList sites = new ArrayList();
		sites.add(new File(platformHome, features ? "features" : "plugins"));  //$NON-NLS-1$//$NON-NLS-2$
		
		File[] linkFiles = new File(platformHome + IPath.SEPARATOR + "links").listFiles(); //$NON-NLS-1$	
		if (linkFiles != null) {
			for (int i = 0; i < linkFiles.length; i++) {
				String path = getSitePath(platformHome, linkFiles[i], features);
				if (path != null) {
					sites.add(new File(path));
				}
			}
		}		
		return (File[])sites.toArray(new File[sites.size()]);
	}
	
	public static URL[] getPluginPaths(String platformHome) {
		if (ExternalModelManager.isTargetEqualToHost(platformHome) && !PDECore.isDevLaunchMode())
			return ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();
		
		File file = new File(platformHome, "configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		if (file.exists()) {
			try {
				IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
				return getConfiguredSitesPaths(platformHome, config, false);
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}		
		return scanLocations(getSites(platformHome, false));
	}
	
	public static URL[] getFeaturePaths(String platformHome) {
		File file = new File(platformHome, "configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		if (file.exists()) {
			try {
				IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
				return getConfiguredSitesPaths(platformHome, config, true);
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}		
		return scanLocations(getSites(platformHome, false));
	}
	
	private static URL[] getConfiguredSitesPaths(String platformHome, IPlatformConfiguration configuration, boolean features) {
		URL[] installPlugins = scanLocations(new File[] { new File(
				platformHome, features ? "features" : "plugins") }); //$NON-NLS-1$ //$NON-NLS-2$
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
				if(features)
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
		ArrayList result = new ArrayList();
		for (int i = 0; i < sites.length; i++){
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
	
}
