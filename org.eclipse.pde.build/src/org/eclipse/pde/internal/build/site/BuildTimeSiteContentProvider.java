/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.*;

public class BuildTimeSiteContentProvider implements IPDEBuildConstants {
	private String installedBaseURL;
	private String[] urls;
	private PDEUIStateWrapper pdeUIState;
	private BuildTimeSite site;

	public BuildTimeSiteContentProvider(String[] urls, String installedBaseURL, PDEUIStateWrapper initialState) {
		//super(null);
		this.installedBaseURL = installedBaseURL;
		this.urls = urls;
		this.pdeUIState = initialState;
	}

	/**
	 * Returns the URL where an eclipse install can be provided. Can be null. 
	 * @return URL
	 */
	public String getInstalledBaseURL() {
		return installedBaseURL;
	}

	public Collection getPluginPaths() {
		Collection pluginsToCompile = findPluginXML(Utils.asFile(urls));
		if (installedBaseURL != null) {
			pluginsToCompile.addAll(Arrays.asList(PluginPathFinder.getPluginPaths(installedBaseURL)));
		}
		return pluginsToCompile;
	}

	public URL getURL() {
		throw new RuntimeException();
	}

	//For every entry, return all the children of this entry is it is named plugins, otherwise return the entry itself  
	private Collection findPluginXML(File[] location) {
		Collection collectedElements = new ArrayList(10);
		for (int i = 0; i < location.length; i++) {
			File f = new File(location[i], DEFAULT_PLUGIN_LOCATION);
			if (f.exists()) {
				//location was the root of an eclipse install, list everything from the plugins directory
				collectedElements.addAll(Arrays.asList(f.listFiles()));
			} else if (new File(location[i], JarFile.MANIFEST_NAME).exists() || new File(location[i], Constants.PLUGIN_FILENAME_DESCRIPTOR).exists()) {
				collectedElements.add(location[i]);
			} else if (location[i].isDirectory()) {
				collectedElements.addAll(Arrays.asList(location[i].listFiles()));
			}
		}
		return collectedElements;
	}

	public PDEUIStateWrapper getInitialState() {
		return pdeUIState;
	}

	public URL getArchiveReference(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public BuildTimeSite getSite() {
		// TODO Auto-generated method stub
		return site;
	}

	public void setSite(BuildTimeSite site) {
		this.site = site;
	}
}
