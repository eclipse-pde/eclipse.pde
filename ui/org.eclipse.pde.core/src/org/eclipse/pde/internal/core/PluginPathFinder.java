/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.runtime.Path;
import org.eclipse.update.configurator.*;

public class PluginPathFinder {
	
	private static String getPath(String platformHome, File file) {
		String prefix = new Path(platformHome).removeLastSegments(1).toString();
		Properties properties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(file);
			properties.load(fis);
			fis.close();
			String path = properties.getProperty("path"); //$NON-NLS-1$
			if (path != null) {
				if (!new Path(path).isAbsolute())
					path = prefix + Path.SEPARATOR + path;
				path += Path.SEPARATOR + "eclipse" + Path.SEPARATOR + "plugins"; //$NON-NLS-1$ //$NON-NLS-2$
				if (new File(path).exists()) {
					return path;
				}
			}
		} catch (IOException e) {
		}
		return null;
	}
	
	private static File[] getSites(String platformHome) {
		ArrayList sites = new ArrayList();
		sites.add(new File(platformHome, "plugins")); //$NON-NLS-1$
		
		File[] linkFiles = new File(platformHome + Path.SEPARATOR + "links").listFiles(); //$NON-NLS-1$	
		if (linkFiles != null) {
			for (int i = 0; i < linkFiles.length; i++) {
				String path = getPath(platformHome, linkFiles[i]);
				if (path != null) {
					sites.add(new File(path));
				}
			}
		}		
		return (File[])sites.toArray(new File[sites.size()]);
	}
	
	public static URL[] getPluginPaths(String platformHome) {
		File file = new File(platformHome, "configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		if (file.exists()) {
			try {
				IPlatformConfiguration config = ConfiguratorUtils.getPlatformConfiguration(file.toURL());
				return getConfiguredSites(platformHome, config);
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}		
		return scanLocations(getSites(platformHome));
	}
	
	private static URL[] getConfiguredSites(String platformHome, IPlatformConfiguration configuration) {
		URL[] installPlugins = scanLocations(new File[]{new File(platformHome, "plugins")});
		URL[] extensionPlugins = getExtensionURLs(configuration);
		
		URL[] all = new URL[installPlugins.length + extensionPlugins.length];
		System.arraycopy(installPlugins, 0, all, 0, installPlugins.length);
		System.arraycopy(extensionPlugins, 0, all, installPlugins.length, extensionPlugins.length);
		return all;
	}
	
	private static URL[] getExtensionURLs(IPlatformConfiguration config) {
		ArrayList extensionPlugins = new ArrayList();
		IPlatformConfiguration.ISiteEntry[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			URL url = sites[i].getURL();
			if ("file".equalsIgnoreCase(url.getProtocol())) {
				String[] plugins = sites[i].getPlugins();
				for (int j = 0; j < plugins.length; j++) {
					try {
						extensionPlugins.add(new File(url.getFile(), plugins[j]).toURL());
					} catch (MalformedURLException e) {
					}
				}
			}			
		}
		return (URL[]) extensionPlugins.toArray(new URL[extensionPlugins.size()]);		
	}
	
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
