/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     James D Miles (IBM Corp.) - bug 176250, Configurator needs to handle more platform urls
 *******************************************************************************/
package org.eclipse.pde.internal.core.update.configurator;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@SuppressWarnings("deprecation")
class Configuration implements IConfigurationConstants {

	private final HashMap<String, SiteEntry> sites = new HashMap<>();
	private final HashMap<String, URL> platformURLs = new HashMap<>();
	private Date date;
	private long lastModified; // needed to account for file system limitations
	private URL url;
	private boolean transientConfig;
	private boolean isDirty;
	private Configuration linkedConfig; // shared configuration
	private URL associatedInstallURL = Utils.getInstallURL();

	public Configuration() {
		this(new Date());
		// the config is created now or out of a platform.xml without a date
		isDirty = true;
	}

	public Configuration(Date date) {
		this.date = date;
	}

	public void setURL(URL url) {
		this.url = url;
	}

	public URL getURL() {
		return url;
	}

	public void setLinkedConfig(Configuration linkedConfig) {
		this.linkedConfig = linkedConfig;
		// make all the sites read-only
		for (SiteEntry linkedSite : linkedConfig.getSites()) {
			linkedSite.setUpdateable(false);
		}
	}

	public Configuration getLinkedConfig() {
		return linkedConfig;
	}

	/**
	 * @return true if the config needs to be saved
	 */
	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean dirty) {
		isDirty = dirty;
	}

	public void addSiteEntry(String url, SiteEntry site) {
		url = Utils.canonicalizeURL(url);
		// only add the same site once
		if (sites.get(url) == null && (linkedConfig == null || linkedConfig.sites.get(url) == null)) {
			site.setConfig(this);
			sites.put(url, site);
			if (url.startsWith("platform:")) {//$NON-NLS-1$
				URL pURL;
				try {
					URL relSite = null;
					if (url.startsWith("platform:/config")) { //$NON-NLS-1$
						// url for location of configuration is relative to
						// platform.xml
						URL config_loc = getURL();
						relSite = new URL(config_loc, ".."); //$NON-NLS-1$
					} else {
						relSite = getInstallURL();
					}

					pURL = new URL(url);
					URL rURL = PlatformConfiguration.resolvePlatformURL(pURL, relSite);
					String resolvedURL = rURL.toExternalForm();
					platformURLs.put(resolvedURL, pURL);
				} catch (IOException e) {
					// can't resolve so can't have look up.
				}
			}
		}
	}

	public void removeSiteEntry(String url) {
		url = Utils.canonicalizeURL(url);
		sites.remove(url);
		if (url.startsWith("platform:")) { //$NON-NLS-1$
			URL pURL;
			try {
				URL relSite = null;
				if (url.startsWith("platform:/config")) { //$NON-NLS-1$
					// url for location of configuration is relative to
					// platform.xml
					URL config_loc = getURL();
					relSite = new URL(config_loc, ".."); //$NON-NLS-1$
				} else {
					relSite = getInstallURL();
				}

				pURL = new URL(url);
				URL rURL = PlatformConfiguration.resolvePlatformURL(pURL, relSite);
				String resolvedURL = rURL.toExternalForm();
				platformURLs.remove(resolvedURL);
			} catch (IOException e) {
				// can't resolve so can't have look up.
			}
		}
	}

	public SiteEntry getSiteEntry(String url) {
		url = Utils.canonicalizeURL(url);
		SiteEntry site = sites.get(url);
		if (site == null && linkedConfig != null) {
			site = linkedConfig.getSiteEntry(url);
		}
		return site;
	}

	public SiteEntry[] getSites() {
		if (linkedConfig == null) {
			return sites.values().toArray(new SiteEntry[sites.size()]);
		}
		ArrayList<SiteEntry> combinedSites = new ArrayList<>(sites.values());
		combinedSites.addAll(linkedConfig.sites.values());
		return combinedSites.toArray(new SiteEntry[combinedSites.size()]);
	}

	public boolean isTransient() {
		return transientConfig;
	}

	public void setTransient(boolean isTransient) {
		this.transientConfig = isTransient;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public boolean unconfigureFeatureEntry(FeatureEntry feature) {
		for (SiteEntry site : getSites()) {
			if (site.unconfigureFeatureEntry(feature)) {
				return true;
			}
		}
		return false;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public long lastModified() {
		return (lastModified != 0) ? lastModified : date.getTime();
	}

	/**
	 * Returns the url as a platform:/ url, if possible, else leaves it
	 * unchanged
	 */
	public URL asPlatformURL(URL url) {
		try {
			if (url.getProtocol().equals("file")) {//$NON-NLS-1$
				String rUrl = url.toExternalForm();
				URL pUrl = platformURLs.get(rUrl);
				if (pUrl == null) {
					return url;
				}
				return pUrl;
			}
			return url;
		} catch (Exception e) {
			return url;
		}
	}

	public URL getInstallURL() {
		return associatedInstallURL;
	}

	public void setInstallLocation(URL installURL) {
		associatedInstallURL = installURL;
	}
}
