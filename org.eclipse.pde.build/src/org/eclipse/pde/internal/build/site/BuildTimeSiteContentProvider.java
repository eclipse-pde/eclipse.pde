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
import java.net.URL;
import java.util.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.update.core.ISiteContentProvider;
import org.eclipse.update.core.SiteContentProvider;

public class BuildTimeSiteContentProvider extends SiteContentProvider implements ISiteContentProvider, IPDEBuildConstants {
	private String installedBaseURL;
	private String[] urls;

	public BuildTimeSiteContentProvider(String[] urls, String installedBaseURL) {
		super(null);
		this.installedBaseURL = installedBaseURL;
		this.urls = urls;
	}

	/**
	 * Returns the URL where an eclipse install can be provided. Can be null. 
	 * @return URL
	 */
	public String getInstalledBaseURL() {
		return installedBaseURL;
	}

	public Collection getPluginPaths() {
		Collection pluginsToCompile = findPluginXML(urls);
		if (installedBaseURL != null) {
			pluginsToCompile.addAll(findPluginXML(PluginPathFinder.getPluginPaths(installedBaseURL)));
			pluginsToCompile.addAll(findPluginXML(new String[] { installedBaseURL }));	
		}
		return pluginsToCompile;
	}

	public URL getURL() {
		throw new RuntimeException();
	}

	//For every entry, return all the children of this entry is it is named plugins, otherwise return the entry itself  
	private Collection findPluginXML(String[] location) {		
		Collection collectedElements = new ArrayList(10);
		for (int i = 0; i < location.length; i++) {		
			File f = new File(location[i], DEFAULT_PLUGIN_LOCATION);
			if ( f.exists() ) {
				collectedElements.addAll(Arrays.asList(f.listFiles()));
			} else {
				collectedElements.add(new File(location[i]));
			}
		}  
		return collectedElements;
	}
	
}
