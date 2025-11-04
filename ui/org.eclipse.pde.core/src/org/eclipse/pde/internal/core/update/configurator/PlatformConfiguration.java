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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;

/**
 * This class is responsible for providing the features and plugins (bundles) to
 * the runtime. Configuration data is stored in the
 * configuration/org.eclipse.update/platform.xml file. When eclipse starts, it
 * tries to load the config info from platform.xml. If the file does not exist,
 * then it also tries to read it from a temp or backup file. If this does not
 * succeed, a platform.xml is created by inspecting the eclipse installation
 * directory (its features and plugin folders). If platform.xml already exists,
 * a check is made to see when it was last modified and whether there are any
 * file system changes that are newer (users may manually unzip features and
 * plugins). In this case, the newly added features and plugins are picked up. A
 * check for existence of features and plugins is also performed, to detect
 * deletions.
 */
@Deprecated
public class PlatformConfiguration implements IConfigurationConstants {

	private Configuration config;
	private static int defaultPolicy = DEFAULT_POLICY_TYPE;

	private static final String CONFIG_FILE_TEMP_SUFFIX = ".tmp"; //$NON-NLS-1$

	private static URL installURL;

	@Deprecated
	public PlatformConfiguration(URL url) throws Exception {
		URL installLocation = Utils.getInstallURL();
		// Retrieve install location with respect to given url if possible
		try {
			if (url != null && url.getProtocol().equals("file") //$NON-NLS-1$
					&& url.getPath().endsWith("configuration/org.eclipse.update/platform.xml")) { //$NON-NLS-1$
				installLocation = IPath.fromOSString(url.getPath()).removeLastSegments(3).toFile().toURL();
			}
		} catch (Exception e) {
			//
		}
		initialize(url, installLocation);
	}

	@Deprecated
	public static int getDefaultPolicy() {
		return defaultPolicy;
	}

	@Deprecated
	public SiteEntry[] getConfiguredSites() {
		if (config == null) {
			return new SiteEntry[0];
		}

		SiteEntry[] sites = config.getSites();
		ArrayList<SiteEntry> enabledSites = new ArrayList<>(sites.length);
		for (SiteEntry site : sites) {
			if (site.isEnabled()) {
				enabledSites.add(site);
			}
		}
		return enabledSites.toArray(new SiteEntry[enabledSites.size()]);
	}

	private synchronized void initialize(URL url, URL installLocation) throws Exception {
		if (url != null) {
			config = loadConfig(url, installLocation);
		}
		if (config == null) {
			config = new Configuration();
		}
		config.setURL(url);
		config.setInstallLocation(installLocation);
	}

	private Configuration loadConfig(URL url, URL installLocation) throws Exception {
		if (url == null) {
			throw new IOException(Messages.cfig_unableToLoad_noURL);
		}

		// try to load saved configuration file (watch for failed prior save())
		ConfigurationParser parser = new ConfigurationParser();

		config = null;
		Exception originalException = null;
		try {
			config = parser.parse(url, installLocation);
			if (config == null) {
				throw new Exception(Messages.PlatformConfiguration_cannotFindConfigFile);
			}
		} catch (Exception e1) {
			// check for save failures, so open temp and backup configurations
			originalException = e1;
			try {
				URL tempURL = new URL(url.toExternalForm() + CONFIG_FILE_TEMP_SUFFIX);
				config = parser.parse(tempURL, installLocation);
				if (config == null) {
					throw new Exception();
				}
				config.setDirty(true); // force saving to platform.xml
			} catch (Exception e2) {
				try {
					// check the backup
					if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
						File cfigFile = new File(url.getFile().replace('/', File.separatorChar));
						File workingDir = cfigFile.getParentFile();
						if (workingDir != null && workingDir.exists()) {
							File[] backups = workingDir.listFiles(
									(FileFilter) pathname -> pathname.isFile() && pathname.getName().endsWith(".xml")); //$NON-NLS-1$
							if (backups != null && backups.length > 0) {
								URL backupUrl = backups[backups.length - 1].toURL();
								config = parser.parse(backupUrl, installLocation);
							}
						}
					}
					if (config == null)
					 {
						throw originalException; // we tried, but no config here
					}
													// ...
					config.setDirty(true); // force saving to platform.xml
				} catch (IOException e3) {
					throw originalException; // we tried, but no config here ...
				}
			}
		}

		return config;
	}

	@Deprecated
	public static boolean supportsDetection(URL url, URL installLocation) {
		String protocol = url.getProtocol();
		if (protocol.equals("file")) { //$NON-NLS-1$
			return true;
		} else if (protocol.equals("platform")) { //$NON-NLS-1$
			URL resolved = null;
			try {
				resolved = resolvePlatformURL(url, installLocation); // 19536
			} catch (IOException e) {
				return false; // we tried but failed to resolve the platform URL
			}
			return resolved.getProtocol().equals("file"); //$NON-NLS-1$
		} else {
			return false;
		}
	}

	@Deprecated
	public static URL resolvePlatformURL(URL url, URL base_path_Location) throws IOException {
		if (url.getProtocol().equals("platform")) { //$NON-NLS-1$
			if (base_path_Location == null) {
				url = FileLocator.toFileURL(url);
				File f = new File(url.getFile());
				url = f.toURL();
			} else {
				final String BASE = "platform:/base/"; //$NON-NLS-1$
				final String CONFIG = "platform:/config/"; //$NON-NLS-1$
				String toResolve = url.toExternalForm();
				if (toResolve.startsWith(BASE)) {
					url = new URL(base_path_Location, toResolve.substring(BASE.length()));
				} else if (toResolve.startsWith(CONFIG)) {
					url = new URL(base_path_Location, toResolve.substring(CONFIG.length()));
				} else {
					url = base_path_Location;
				}
			}
		}
		return url;
	}

	@Deprecated
	public static URL getInstallURL() {
		return installURL;
	}

	@Deprecated
	public Configuration getConfiguration() {
		return config;
	}
}
