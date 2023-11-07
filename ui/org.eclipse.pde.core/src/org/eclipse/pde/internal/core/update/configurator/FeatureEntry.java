/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.update.configurator;

import java.net.URL;

/**
 *
 * Feature information
 */
@SuppressWarnings("deprecation")
class FeatureEntry implements IConfigurationConstants {
	private final String id;
	private final String version;
	private final String pluginVersion;
	private final String application;
	private final URL[] root;
	private final boolean primary;
	private final String pluginIdentifier;
	private String url;
	private SiteEntry site;

	public FeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary,
			String application, URL[] root) {
		if (id == null) {
			throw new IllegalArgumentException();
		}
		this.id = id;
		this.version = version;
		this.pluginVersion = pluginVersion;
		this.pluginIdentifier = pluginIdentifier;
		this.primary = primary;
		this.application = application;
		this.root = (root == null ? new URL[0] : root);
	}

	public FeatureEntry(String id, String version, String pluginVersion, boolean primary, String application,
			URL[] root) {
		this(id, version, id, pluginVersion, primary, application, root);
	}

	public void setSite(SiteEntry site) {
		this.site = site;
	}

	public SiteEntry getSite() {
		return this.site;
	}

	/**
	 * Sets the url string (relative to the site url)
	 */
	public void setURL(String url) {
		this.url = url;
	}

	/**
	 * @return the feature url (relative to the site):
	 *         features/org.eclipse.platform/
	 */
	public String getURL() {
		// if (url == null)
		// url = FEATURES + "/" + id + "_" + version + "/";
		return url;
	}

	public String getFeatureIdentifier() {
		return id;
	}

	public String getFeatureVersion() {
		return version;
	}

	public String getFeaturePluginVersion() {
		return pluginVersion != null && pluginVersion.length() > 0 ? pluginVersion : null;
	}

	public String getFeaturePluginIdentifier() {
		// if no plugin is specified, use the feature id
		return pluginIdentifier != null && pluginIdentifier.length() > 0 ? pluginIdentifier : id;
	}

	public String getFeatureApplication() {
		return application;
	}

	public URL[] getFeatureRootURLs() {
		return root;
	}

	public boolean canBePrimary() {
		return primary;
	}

	public String getApplication() {
		return application;
	}

	public String getId() {
		return id;
	}

}
