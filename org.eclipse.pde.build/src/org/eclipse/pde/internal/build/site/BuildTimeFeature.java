/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.*;
import java.util.ArrayList;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.compatibility.SiteManager;

public class BuildTimeFeature extends Feature {
	/**
	 * Simple file name of the default feature manifest file
	 * @since 3.4.0
	 */
	public static final String FEATURE_FILE = "feature"; //$NON-NLS-1$

	/**
	 * File extension of the default feature manifest file
	 * @since 3.4.0
	 */
	public static final String FEATURE_XML = FEATURE_FILE + ".xml"; //$NON-NLS-1$

	public BuildTimeFeature(String id, String version) {
		super(id, version);
	}

	public BuildTimeFeature() {
		super("", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Boolean binary = null;
	private int contextQualifierLength = -1;
	private BuildTimeSiteContentProvider contentProvider = null;
	private BuildTimeSite site = null;
	private URL url = null;
	private String rootLocation = null;

	public FeatureEntry[] getRawIncludedFeatureReferences() {
		ArrayList included = new ArrayList();
		FeatureEntry[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || entries[i].isPlugin())
				continue;
			included.add(entries[i]);
		}
		return (FeatureEntry[]) included.toArray(new FeatureEntry[included.size()]);
	}

	public FeatureEntry[] getIncludedFeatureReferences() {
		ArrayList included = new ArrayList();
		FeatureEntry[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || entries[i].isPlugin())
				continue;

			if (SiteManager.isValidEnvironment(entries[i])) {
				included.add(entries[i]);
			}
		}

		return (FeatureEntry[]) included.toArray(new FeatureEntry[included.size()]);
	}

	public FeatureEntry[] getPluginEntries() {
		ArrayList plugins = new ArrayList();
		FeatureEntry[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || !entries[i].isPlugin())
				continue;
			if (SiteManager.isValidEnvironment(entries[i])) {
				plugins.add(entries[i]);
			}
		}
		return (FeatureEntry[]) plugins.toArray(new FeatureEntry[plugins.size()]);
	}

	public FeatureEntry[] getRawPluginEntries() {
		ArrayList plugins = new ArrayList();
		FeatureEntry[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isRequires() || !entries[i].isPlugin())
				continue;
			plugins.add(entries[i]);
		}
		return (FeatureEntry[]) plugins.toArray(new FeatureEntry[plugins.size()]);
	}

	public FeatureEntry[] getImports() {
		ArrayList imports = new ArrayList();
		FeatureEntry[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (!entries[i].isRequires())
				continue;
			imports.add(entries[i]);
		}
		return (FeatureEntry[]) imports.toArray(new FeatureEntry[imports.size()]);
	}

	public boolean isBinary() {
		if (binary == null) {
			String root = getRootLocation();
			File properties = new File(root, IPDEBuildConstants.PROPERTIES_FILE);
			if (!properties.exists())
				binary = Boolean.TRUE;
			else
				binary = Boolean.FALSE;
		}
		return binary.booleanValue();
	}

	public void setBinary(boolean isCompiled) {
		this.binary = isCompiled ? Boolean.TRUE : Boolean.FALSE;
	}

	public void setContextQualifierLength(int l) {
		contextQualifierLength = l;
	}

	public int getContextQualifierLength() {
		return contextQualifierLength;
	}

	public void setSite(BuildTimeSite site) {
		this.site = site;
	}

	public BuildTimeSite getSite() {
		return site;
	}

	public void setFeatureContentProvider(BuildTimeSiteContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public BuildTimeSiteContentProvider getFeatureContentProvider() {
		return contentProvider;
	}

	public URL getURL() {
		return url;
	}

	public void setURL(URL url) {
		this.url = url;
	}

	/**
	 * @return the local filesystem location of the directory containing the feature.xml file. 
	 */
	public String getRootLocation() {
		if (rootLocation == null) {
			URL location = getURL();
			if (location == null)
				return null;
			try {
				URI locationURI = URIUtil.toURI(location);
				rootLocation = URIUtil.toFile(locationURI).getAbsolutePath();
			} catch (URISyntaxException e) {
				rootLocation = location.getPath();
			}
			int i = rootLocation.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (i != -1)
				rootLocation = rootLocation.substring(0, i);
		}
		return rootLocation;
	}

	public FeatureEntry findPluginEntry(String id, String version) {
		FeatureEntry[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isPlugin() && entries[i].getId().equals(id))
				if (Utils.matchVersions(version, entries[i].getVersion()))
					return entries[i];
		}
		return null;
	}
}
