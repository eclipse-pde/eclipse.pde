/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Utils;

public class PluginPathFinder {
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
		try (FileInputStream fis = new FileInputStream(linkFile)) {
			properties.load(fis);
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
		ArrayList<File> sites = new ArrayList<>();

		File file = new File(platformHome, features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
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
		return sites.toArray(new File[sites.size()]);
	}

	private static List<File> getDropins(String platformHome, boolean features) {
		File dropins = new File(platformHome, DROPINS);
		if (!dropins.exists())
			return Collections.emptyList();

		ArrayList<File> sites = new ArrayList<>();
		ArrayList<File> results = new ArrayList<>();

		File[] contents = dropins.listFiles();
		for (File content : contents) {
			if (content.isFile()) {
				if (content.getName().endsWith(LINK)) {
					String path = getSitePath(platformHome, content, features);
					if (path != null)
						sites.add(new File(path));
				} else {
					//bundle
					results.add(content);
				}
			} else { //folder
				//dropins/features or dropins/plugins
				if (content.isDirectory() && content.getName().equals(features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION)) {
					results.addAll(Arrays.asList(content.listFiles()));
					continue;
				}

				//dropins/*/features or dropins/*/plugins
				File temp = new File(content, features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
				if (temp.isDirectory()) {
					sites.add(temp);
					continue;
				}

				//dropins/*/eclipse/features or dropins/*/eclipse/plugins
				temp = new File(content, ECLIPSE + File.separator + (features ? IPDEBuildConstants.DEFAULT_FEATURE_LOCATION : IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION));
				if (temp.isDirectory()) {
					sites.add(temp);
					continue;
				}

				//else treat as a bundle/feature
				results.add(content);
			}
		}

		results.addAll(scanLocations(sites.toArray(new File[sites.size()])));
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

		List<File> list = scanLocations(getSites(platformHome, features));
		list.addAll(getDropins(platformHome, features));
		return Utils.asFile(list);
	}

	/**
	 * Scan given plugin/feature directories or jars for existence
	 * @param sites
	 * @return URLs to plugins/features
	 */
	private static List<File> scanLocations(File[] sites) {
		ArrayList<File> result = new ArrayList<>();
		for (File site : sites) {
			if (site == null || !site.exists())
				continue;
			File[] children = site.listFiles();
			if (children != null)
				result.addAll(Arrays.asList(children));
		}
		return result;
	}
}
