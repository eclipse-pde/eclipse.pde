/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.ISiteContentProvider;
import org.eclipse.update.core.SiteContentProvider;

public class BuildTimeSiteContentProvider extends SiteContentProvider implements ISiteContentProvider, IPDEBuildConstants {
	private URL installedBaseURL;
	private URL[] urls;

	public BuildTimeSiteContentProvider(URL[] urls, URL installedBaseURL) {
		super(urls[0]);
		this.installedBaseURL = installedBaseURL;
		this.urls = urls;
	}

	/**
	 * Returns the URL where an eclipse install can be provided. Can be null. 
	 * @return URL
	 */
	public URL getInstalledBaseURL() {
		return installedBaseURL;
	}

	public URL[] getPluginPaths() {
		URL[] pluginsToCompile = findPluginXML(urls);

		if (installedBaseURL != null) {
			URL[] installedPlugins = findPluginXML(new URL[] { installedBaseURL });
			URL[] pluginsAndBase = new URL[pluginsToCompile.length + installedPlugins.length];
			System.arraycopy(pluginsToCompile, 0, pluginsAndBase, 0, pluginsToCompile.length);
			System.arraycopy(installedPlugins, 0, pluginsAndBase, pluginsToCompile.length, installedPlugins.length);
			return pluginsAndBase;
		} else {
			return pluginsToCompile;
		}
	}

	public URL getURL() {
		throw new RuntimeException(); //TO CHECK
	}

	private URL[] findPluginXML(URL[] location) {
		Collection collectedElements = new ArrayList(10);
		for (int i = 0; i < location.length; i++) {
			Collection foundPlugins = Utils.findFiles(location[i].getFile(), DEFAULT_PLUGIN_LOCATION, DEFAULT_PLUGIN_FILENAME_DESCRIPTOR);
			if (foundPlugins != null)
				collectedElements.addAll(foundPlugins);
		}
		for (int i = 0; i < location.length; i++) {
			Collection foundFragments = Utils.findFiles(location[i].getFile(), DEFAULT_PLUGIN_LOCATION, DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR);
			if (foundFragments != null)
				collectedElements.addAll(foundFragments);
		}
		URL[] pluginURLs = new URL[collectedElements.size()];
		int i = 0;
		for (Iterator iter = collectedElements.iterator(); iter.hasNext();) {
			File element = (File) iter.next();
			try {
				pluginURLs[i] = new URL("file:" + element.getAbsolutePath()); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				Platform.getPlugin(PI_PDEBUILD).getLog().log(new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_MISSING_SOURCE, Policy.bind("warning.cannotLocateSource", element.getAbsolutePath()), e)); //$NON-NLS-1$
			}
			i++;
		}
		return pluginURLs;
	}
}
