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
 *******************************************************************************/
package org.eclipse.pde.internal.core.update.configurator;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;

@Deprecated
public class SiteEntry implements IConfigurationConstants {
	private static final String MAC_OS_MARKER = ".DS_Store"; //$NON-NLS-1$

	private URL url; // this is the external URL for the site
	private URL resolvedURL; // this is the resolved URL used internally
	private SitePolicy policy;
	private boolean updateable = true;
	private Map<String, FeatureEntry> featureEntries;
	private long changeStamp;
	private long featuresChangeStamp;
	private long pluginsChangeStamp;
	private String linkFileName;
	private boolean enabled = true;
	private Configuration config;

	private static FeatureParser featureParser = new FeatureParser();
	private static boolean isMacOS = Utils.getOS().equals(Constants.OS_MACOSX);

	public SiteEntry(URL url) {
		this(url, null);
	}

	public SiteEntry(URL url, SitePolicy policy) {
		if (url == null) {
			try {
				url = new URL("platform:/base/"); //$NON-NLS-1$ try using
													// platform-relative URL
			} catch (MalformedURLException e) {
				url = PlatformConfiguration.getInstallURL(); // ensure we come
																// up ... use
																// absolute file
																// URL
			}
		}

		if (policy == null) {
			policy = new SitePolicy(PlatformConfiguration.getDefaultPolicy(), DEFAULT_POLICY_LIST);
		}

		if (url.getProtocol().equals("file")) { //$NON-NLS-1$
			try {
				// TODO remove this when platform fixes local file url's
				this.url = new File(url.getFile()).toURL();
			} catch (MalformedURLException e1) {
				this.url = url;
			}
		} else {
			this.url = url;
		}

		this.policy = policy;
		this.resolvedURL = this.url;
	}

	public void setConfig(Configuration config) {
		this.config = config;
		if (url.getProtocol().equals("platform")) { //$NON-NLS-1$
			try {
				// resolve the config location relative to the configURL
				if (url.getPath().startsWith("/config")) { //$NON-NLS-1$
					URL configURL = config.getURL();
					URL config_loc = new URL(configURL, ".."); //$NON-NLS-1$
					resolvedURL = PlatformConfiguration.resolvePlatformURL(url, config_loc); // 19536
				}
				else {
					resolvedURL = PlatformConfiguration.resolvePlatformURL(url, config.getInstallURL()); // 19536
				}
			} catch (IOException e) {
				// will use the baseline URL ...
			}
		}
	}

	public Configuration getConfig() {
		return config;
	}

	public URL getURL() {
		return url;
	}

	public SitePolicy getSitePolicy() {
		return policy;
	}

	public synchronized void setSitePolicy(SitePolicy policy) {
		if (policy == null) {
			throw new IllegalArgumentException();
		}
		this.policy = policy;
	}

	public String[] getFeatures() {
		return getDetectedFeatures();
	}

	public long getChangeStamp() {
		if (changeStamp == 0) {
			computeChangeStamp();
		}
		return changeStamp;
	}

	public long getFeaturesChangeStamp() {
		if (featuresChangeStamp == 0) {
			computeFeaturesChangeStamp();
		}
		return featuresChangeStamp;
	}

	public long getPluginsChangeStamp() {
		if (pluginsChangeStamp == 0) {
			computePluginsChangeStamp();
		}
		return pluginsChangeStamp;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public boolean isNativelyLinked() {
		return isExternallyLinkedSite();
	}

	public URL getResolvedURL() {
		return resolvedURL;
	}

	/**
	 * Detect new features (timestamp &gt; current site timestamp) and validates
	 * existing features (they might have been removed)
	 */
	private void detectFeatures() {

		if (featureEntries != null) {
			validateFeatureEntries();
		} else {
			featureEntries = new HashMap<>();
		}

		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			return;
		}

		// locate feature entries on site
		File siteRoot = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		File featuresDir = new File(siteRoot, FEATURES);
		if (featuresDir.exists()) {
			// handle the installed features under the features directory
			File[] dirs = featuresDir.listFiles((FileFilter) f -> {
				// mac os folders contain a file .DS_Store in each folder, and
				// we need to skip it (bug 76869)
				if (isMacOS && f.getName().equals(MAC_OS_MARKER)) {
					return false;
				}
				boolean valid = f.isDirectory() && (new File(f, FEATURE_XML).exists());
				if (!valid) {
					Utils.log(NLS.bind(Messages.SiteEntry_cannotFindFeatureInDir,
							(new String[] { f.getAbsolutePath() })));
				}
				return valid;
			});

			for (File dir : dirs) {
				try {
					File featureXML = new File(dir, FEATURE_XML);
					if (featureXML.lastModified() <= featuresChangeStamp && dir.lastModified() <= featuresChangeStamp) {
						continue;
					}
					URL featureURL = featureXML.toURL();
					FeatureEntry featureEntry = featureParser.parse(featureURL);
					if (featureEntry != null) {
						addFeatureEntry(featureEntry);
					}
				} catch (MalformedURLException e) {
					Utils.log(NLS.bind(Messages.InstalledSiteParser_UnableToCreateURLForFile,
							(new String[] { featuresDir.getAbsolutePath() })));
				}
			}
		}
	}

	/**
	 * @return list of feature url's (relative to site)
	 */
	private synchronized String[] getDetectedFeatures() {
		if (featureEntries == null) {
			detectFeatures();
		}
		String[] features = new String[featureEntries.size()];
		Iterator<FeatureEntry> iterator = featureEntries.values().iterator();
		for (int i = 0; i < features.length; i++) {
			features[i] = iterator.next().getURL();
		}
		return features;
	}

	private void computeChangeStamp() {
		changeStamp = Math.max(computeFeaturesChangeStamp(), computePluginsChangeStamp());
		// changeStampIsValid = true;
	}

	private synchronized long computeFeaturesChangeStamp() {
		if (featuresChangeStamp > 0) {
			return featuresChangeStamp;
		}

		String[] features = getFeatures();

		// compute stamp for the features directory
		long dirStamp = 0;
		if (PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			File featuresDir = new File(root, FEATURES);
			dirStamp = featuresDir.lastModified();
		}
		featuresChangeStamp = Math.max(dirStamp, computeStamp(features));
		return featuresChangeStamp;
	}

	private synchronized long computePluginsChangeStamp() {
		if (pluginsChangeStamp > 0) {
			return pluginsChangeStamp;
		}

		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			Utils.log(NLS.bind(Messages.SiteEntry_computePluginStamp, (new String[] { resolvedURL.toExternalForm() })));
			return 0;
		}

		// compute stamp for the plugins directory
		File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		File pluginsDir = new File(root, PLUGINS);
		if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
			return 0;
		}

		pluginsChangeStamp = pluginsDir.lastModified();
		return pluginsChangeStamp;
	}

	private long computeStamp(String[] targets) {

		long result = 0;
		if (!PlatformConfiguration.supportsDetection(resolvedURL, config.getInstallURL())) {
			// NOTE: this path should not be executed until we support running
			// from an arbitrary URL (in particular from http server). For
			// now just compute stamp across the list of names. Eventually
			// when general URLs are supported we need to do better (factor
			// in at least the existence of the target). However, given this
			// code executes early on the startup sequence we need to be
			// extremely mindful of performance issues.
			// In fact, we should get the last modified from the connection
			for (String target : targets) {
				result ^= target.hashCode();
			}
		} else {
			// compute stamp across local targets
			File rootFile = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			if (rootFile.exists()) {
				File f = null;
				for (String target : targets) {
					f = new File(rootFile, target);
					if (f.exists()) {
						result = Math.max(result, f.lastModified());
					}
				}
			}
		}

		return result;
	}

	public void setLinkFileName(String linkFileName) {
		this.linkFileName = linkFileName;
	}

	public String getLinkFileName() {
		return linkFileName;
	}

	public boolean isExternallyLinkedSite() {
		return (linkFileName != null && !linkFileName.trim().isEmpty());
	}

	public void addFeatureEntry(FeatureEntry feature) {
		if (featureEntries == null) {
			featureEntries = new HashMap<>();
		}
		// Make sure we keep the larger version of same feature
		FeatureEntry existing = featureEntries.get(feature.getFeatureIdentifier());
		if (existing != null) {
			VersionedIdentifier existingVersion = new VersionedIdentifier(existing.getFeatureIdentifier(),
					existing.getFeatureVersion());
			VersionedIdentifier newVersion = new VersionedIdentifier(feature.getFeatureIdentifier(),
					feature.getFeatureVersion());
			if (existingVersion.getVersion().compareTo(newVersion.getVersion()) < 0) {
				featureEntries.put(feature.getFeatureIdentifier(), feature);
				pluginsChangeStamp = 0;
			} else if (existingVersion.equals(newVersion)) {
				// log error if same feature version/id but a different url
				if (!feature.getURL().equals(existing.getURL())) {
					Utils.log(NLS.bind(Messages.SiteEntry_duplicateFeature,
							(new String[] { getURL().toExternalForm(), existing.getFeatureIdentifier() })));
				}
			}
		} else {
			featureEntries.put(feature.getFeatureIdentifier(), feature);
			pluginsChangeStamp = 0;
		}
		feature.setSite(this);
	}

	public FeatureEntry[] getFeatureEntries() {
		if (featureEntries == null) {
			detectFeatures();
		}

		if (featureEntries == null) {
			return new FeatureEntry[0];
		}
		return featureEntries.values().toArray(new FeatureEntry[featureEntries.size()]);
	}

	private void validateFeatureEntries() {
		File root = new File(resolvedURL.getFile().replace('/', File.separatorChar));
		Iterator<FeatureEntry> iterator = featureEntries.values().iterator();
		Collection<String> deletedFeatures = new ArrayList<>();
		while (iterator.hasNext()) {
			FeatureEntry feature = iterator.next();
			// Note: in the future, we can check for absolute url as well.
			// For now, feature url is features/org.eclipse.foo/feature.xml
			File featureXML = new File(root, feature.getURL());
			if (!featureXML.exists()) {
				deletedFeatures.add(feature.getFeatureIdentifier());
			}
		}
		for (String string : deletedFeatures) {
			featureEntries.remove(string);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enable) {
		this.enabled = enable;
	}

	public FeatureEntry getFeatureEntry(String id) {
		for (FeatureEntry feature : getFeatureEntries()) {
			if (feature.getFeatureIdentifier().equals(id)) {
				return feature;
			}
		}
		return null;
	}

	public boolean unconfigureFeatureEntry(FeatureEntry feature) {
		FeatureEntry existingFeature = getFeatureEntry(feature.getFeatureIdentifier());
		if (existingFeature != null) {
			featureEntries.remove(existingFeature.getFeatureIdentifier());
		}
		return existingFeature != null;
	}

	/*
	 * This is a bit of a hack. When no features were added to the site, but the
	 * site is initialized from platform.xml we need to set the feature set to
	 * empty, so we don't try to detect them.
	 */
	public void initialized() {
		if (featureEntries == null) {
			featureEntries = new HashMap<>();
		}
	}
}
