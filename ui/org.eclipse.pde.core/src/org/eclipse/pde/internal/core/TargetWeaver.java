/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Hannes Wellmann - Bug 577541 - Clean up ClasspathHelper and TargetWeaver
 *     Hannes Wellmann - Bug 577543 - Only weave dev.properties for secondary launches if plug-in is from Running-Platform
 *     Hannes Wellmann - Bug 577118 - Handle multiple Plug-in versions in launching facility
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Supports target weaving (combining the target platform with workspace
 * projects to generate a woven target platform).
 *
 * @since 3.4
 */
public class TargetWeaver {
	private TargetWeaver() { // static use only
	}

	/**
	 * Location of dev.properties, {@code null} if the Platform is not in
	 * development mode.
	 */
	private static String fgDevPropertiesURL = null;

	/**
	 * Property file corresponding to dev.properties
	 */
	private static Properties fgDevProperties = null;

	/**
	 * Initializes system properties
	 */
	static {
		if (Platform.inDevelopmentMode()) {
			fgDevPropertiesURL = System.getProperty("osgi.dev"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the dev.properties as a property store.
	 *
	 * @return properties
	 */
	private static synchronized Properties getDevProperties() {
		if (fgDevPropertiesURL != null) {
			if (fgDevProperties == null) {
				fgDevProperties = new Properties();
				try {
					URL url = new URL(fgDevPropertiesURL);
					File file = toFile(url);
					if (file.exists()) {
						try (InputStream stream = new FileInputStream(file)) {
							fgDevProperties.load(stream);
						}
					}
				} catch (IOException e) {
					PDECore.log(e);
				}
			}
			return fgDevProperties;
		}
		return null;
	}

	/**
	 * Updates the bundle class path if this manifest refers to a project in development
	 * mode from the launching workspace.
	 *
	 * @param manifest manifest to update
	 * @param bundleLocation the location of the Manifest's bundle
	 */
	public static void weaveManifest(Map<String, String> manifest, File bundleLocation) {
		if (manifest != null && fgDevPropertiesURL != null) {
			Properties properties = getDevProperties();
			String id = manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			String version = manifest.get(Constants.BUNDLE_VERSION);
			if (id != null && version != null) {
				int index = id.indexOf(';');
				if (index != -1) {
					id = id.substring(0, index);
				}
				String property = getDevProperty(bundleLocation.toPath(), id, version, properties);
				if (property != null) {
					manifest.put(Constants.BUNDLE_CLASSPATH, property);
				}
			}
		}
	}

	/**
	 * When launching a secondary runtime workbench, all projects already in dev
	 * mode that participate in that runtime must continue in dev mode such that
	 * their class files are found.
	 *
	 * @param launchDevProperties dev.properties
	 * @param launchedPlugins the bundles that participate in secondary runtime
	 */
	static void weaveRunningPlatformDevProperties(Map<IPluginModelBase, String> launchDevProperties,
			Iterable<IPluginModelBase> launchedPlugins) {
		if (fgDevPropertiesURL != null) {
			Properties platformDevProperties = getDevProperties();
			for (IPluginModelBase launchedPlugin : launchedPlugins) {
				String devCP = getDevProperty(launchedPlugin, platformDevProperties);
				if (devCP != null) {
					launchDevProperties.put(launchedPlugin, devCP);
				}
			}
		}
	}

	/**
	 * If a source annotation is pointing to a host project that is being wove, returns
	 * an empty string so that the source annotation is the root of the project.
	 * Otherwise returns the given library name.
	 *
	 * @param model plug-in we are attaching source for
	 * @param libraryName the standard library name
	 * @return empty string or the standard library name
	 */
	static String getWeavedSourceLibraryName(IPluginModelBase model, String libraryName) {
		// Note that if the host project has binary-linked libraries, these libraries appear in the dev.properties file with full path names,
		// and the library name must be returned as-is.
		if (fgDevPropertiesURL != null && !new File(libraryName).isAbsolute()) {
			Properties properties = getDevProperties();
			String id = null;
			if (model.getBundleDescription() != null) {
				id = model.getBundleDescription().getSymbolicName();
			}
			/*
			 * Workaround for bug 332112: Do not hack the source path for
			 * bundles that are not coming from the host workspace.
			 *
			 * The architectural bug is that this weaving takes place at the
			 * wrong level. It should already be done while the target platform
			 * resolves bundles from the host workspace.
			 */
			if (id != null) {
				String property = getDevProperty(model, properties);
				if (property != null) {
					return ""; //$NON-NLS-1$
				}
			}
		}
		return libraryName;
	}

	private static String getDevProperty(IPluginModelBase plugin, Properties devProperties) {
		// If it has an underlying resource, then it's from the local workspace.
		if (plugin.getUnderlyingResource() == null) {
			Path pluginLocation = Path.of(plugin.getInstallLocation());
			IPluginBase pluginBase = plugin.getPluginBase();
			return getDevProperty(pluginLocation, pluginBase.getId(), pluginBase.getVersion(), devProperties);
		}
		return null;
	}

	private static String getDevProperty(Path bundleLocation, String id, String version, Properties devProperties) {
		String devCP = ClasspathHelper.getDevClasspath(devProperties, id, version);
		return devCP != null && isBundleOfRunningPlatform(bundleLocation, id, version) ? devCP : null;
	}

	private static boolean isBundleOfRunningPlatform(Path pluginLocation, String id, String version) {
		Bundle platformBundle = findRunningPlatformBundle(id, version);
		if (platformBundle != null) {
			try {
				Optional<File> bundleFile = FileLocator.getBundleFileLocation(platformBundle);
				return bundleFile.isPresent() && Files.isSameFile(pluginLocation, bundleFile.get().toPath());
			} catch (IOException e) {
				PDECore.logException(e);
			}
		}
		return false;
	}

	private static Bundle findRunningPlatformBundle(String symbolicName, String versionStr) {
		// Obtain all bundles of the running platform with given symbolicName
		// and filter for version here. This is likely faster than specifying a
		// range like: "[" + version + "," + version + "]"
		Version version = Version.parseVersion(versionStr);
		Bundle[] platformBundles = Platform.getBundles(symbolicName, null);
		if (platformBundles == null) {
			return null;
		}
		return Arrays.stream(platformBundles).filter(b -> b.getVersion().equals(version)).findAny().orElse(null);
	}

	private static File toFile(URL url) {
		try {
			URI uri = url.toURI();
			return new File(uri);
		} catch (URISyntaxException e) {
			return new File(url.getFile());
		}
	}
}
