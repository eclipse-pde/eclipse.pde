/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Issue #202
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

/**
 * Manages where PDE should look when looking for source.  The locations may
 * be specified by the user, by extension, or by bundle manifest entry.
 */
public class SourceLocationManager implements ICoreConstants {

	/**
	 * List of source locations that have been discovered using extension points
	 */
	private SourceExtensions fExtensionLocations;

	/**
	 * Manages locations of individual source bundles
	 */
	private BundleManifestSourceLocationManager fBundleManifestLocator = null;

	/**
	 * Searches source locations for one that provides source for the given pluginBase.
	 * Will search user specified locations, then bundle manifest specified locations, then
	 * extension point specified locations.  If a bundle manifest location is found, the
	 * location of the bundle jar will be returned.  If the source is found at a user defined
	 * or extension location, the archive file specified by the sourceLibraryPath will be
	 * returned (after checking for existence).  If the given sourceLibraryPath is <code>null</code>
	 * the folder or jar for the found source location is returned.
	 * @param pluginBase plugin that needs a source archive
	 * @param sourceLibraryPath relative path to where the specific source library can be found within the source location or <code>null</code>
	 * @return path to a source archive or <code>null</code> if a location could not be found
	 */
	public IPath findSourcePath(IPluginBase pluginBase, IPath sourceLibraryPath) {
		if (pluginBase.getId() == null || pluginBase.getVersion() == null) {
			return null;
		}
		IPath relativePath = getRelativePath(pluginBase, sourceLibraryPath);
		IPath result = searchUserSpecifiedLocations(relativePath);
		if (result == null) {
			result = searchBundleManifestLocations(pluginBase);
			if (result == null) {
				result = searchExtensionLocations(relativePath, pluginBase);
			}
		}
		return result;
	}

	/**
	 * Searches source locations providing source for the given plugin and then searches
	 * that location for the file specified by the filePath argument.  An unencoded URL to this location
	 * will be returned or <code>null</code> if the file could not be found.  Note that the
	 * URL may specify a file that is inside of a jar file.
	 *
	 * @param pluginBase plugin that needs the source file
	 * @param filePath relative path to where the needed file is inside the source location
	 * @return URL to the file, possibly inside of a jar file or <code>null</code>
	 */
	public URL findSourceFile(IPluginBase pluginBase, IPath filePath) {
		if (pluginBase.getId() == null || pluginBase.getVersion() == null) {
			return null;
		}
		IPath relativePath = getRelativePath(pluginBase, filePath);
		IPath result = searchUserSpecifiedLocations(relativePath);
		if (result == null) {
			result = searchBundleManifestLocations(pluginBase);
			if (result != null) {
				try {
					// We use URIs to create the combined jar/path url, but URIs encode special characters
					URI encodedUri = URIUtil.toURI(result.toFile().toURL());
					URI jarUri = URIUtil.toJarURI(encodedUri, filePath);
					return new URL(URIUtil.toUnencodedString(jarUri));
				} catch (MalformedURLException | URISyntaxException e) {
					PDECore.log(e);
				}
			}
			result = searchExtensionLocations(relativePath, pluginBase);
		}
		if (result != null) {
			try {
				return result.toFile().toURL();
			} catch (MalformedURLException e) {
				PDECore.log(e);
			}
		}
		return null;
	}

	/**
	 * Searches source locations for one that provides source for the given pluginBase.
	 * Will search user specified locations, then bundle manifest specified locations, then
	 * extension point specified locations.  The File representing the location of the jar
	 * or directory for the appropriate source plugin will be returned or <code>null</code>.
	 * Equivalent to calling findSourcePath(pluginBase, null).toFile()
	 * @param pluginBase plugin that needs a source archive
	 * @return file representing the source jar or directory, or <code>null</code> if a location could not be found
	 */
	public File findSourcePlugin(IPluginBase pluginBase) {
		if (pluginBase.getId() == null || pluginBase.getVersion() == null) {
			return null;
		}
		IPath path = findSourcePath(pluginBase, null);
		return path == null ? null : path.toFile();
	}

	/**
	 * Returns whether the given path describes a source location with a source bundle manifest entry.
	 * @param plugin the plug-in to test
	 * @return whether the given path is a bundle manifest location
	 */
	public boolean hasBundleManifestLocation(IPluginBase plugin) {
		if (plugin.getId() == null || plugin.getVersion() == null) {
			return false;
		}
		return getBundleManifestLocator().hasValidSourceLocation(plugin.getId(), new Version(plugin.getVersion()));
	}

	/**
	 * Searches bundle manifest source locations for the one that provides source
	 * for the given plugin.  Gets all source roots for the source bundle by parsing
	 * the manifest file.  If the source bundle provides source for multiple plugins,
	 * the roots specified for all of them (duplicates removed).  If the source bundle
	 * only provides source for a single plugin/version, this method will return the
	 * same result as #findSourceRoots(IPluginModelBase).  If the given plugin does not have
	 * a known source location with a bundle manifest entry an empty Set will be returned.
	 *
	 * @param plugin plugin to lookup source for
	 * @return set of String paths that are source roots for the bundle, possibly empty
	 */
	public Set<String> findAllSourceRootsInSourceLocation(IPluginBase plugin) {
		if (plugin.getId() == null || plugin.getVersion() == null) {
			return Collections.emptySet();
		}
		return getBundleManifestLocator().getAllSourceRoots(plugin.getId(), new Version(plugin.getVersion()));
	}

	/**
	 * Searches bundle manifest source locations for the one that provides source
	 * for the given plugin.  Gets the source roots (String paths) for the plugin
	 * by parsing the source bundle's manifest.  If the given plugin does not have
	 * a known source location with a bundle manifest entry an empty Set will be returned.
	 *
	 * @param plugin plugin to lookup source for
	 * @return set of String paths that are source roots for the plugin, possibly empty
	 */
	public Set<String> findSourceRoots(IPluginBase plugin) {
		if (plugin.getId() == null || plugin.getVersion() == null) {
			return Collections.emptySet();
		}
		return getBundleManifestLocator().getSourceRoots(plugin.getId(), new Version(plugin.getVersion()));
	}

	/**
	 * Clears the cache of all known extension and bundle manifest locations.
	 */
	public void reset() {
		fExtensionLocations = null;
		fBundleManifestLocator = null;
	}

	/**
	 * While there is no UI to access this feature, we still support additional
	 * source locations being provided via preference.
	 *
	 * @return array of source locations that have been specified by the user
	 */
	@SuppressWarnings("deprecation")
	public List<IPath> getUserLocations() {
		List<IPath> userLocations = new ArrayList<>();
		String pref = PDECore.getDefault().getPreferencesManager().getString(P_SOURCE_LOCATIONS);
		if (pref.length() > 0) {
			parseSavedSourceLocations(pref, userLocations);
		}
		return userLocations;
	}

	/**
	 * @return array of source locations that have been added via extension point
	 */
	public Collection<IPath> getExtensionLocations() {
		return getExtensions().locations;
	}

	private SourceExtensions getExtensions() {
		if (fExtensionLocations == null) {
			fExtensionLocations = processExtensions();
		}
		return fExtensionLocations;
	}

	/**
	 * @return array of source locations defined by a bundle manifest entry
	 */
	public Collection<IPath> getBundleManifestLocations() {
		return getBundleManifestLocator().getSourceLocations();
	}

	/**
	 * @return source location that was specified by a bundle manifest entry to provide source for the given plugin.
	 */
	private IPath getBundleManifestLocation(String pluginID, Version version) {
		return getBundleManifestLocator().getSourceLocation(pluginID, version);
	}

	/**
	 * @return manager for bundle manifest source locations
	 */
	private BundleManifestSourceLocationManager getBundleManifestLocator() {
		if (fBundleManifestLocator == null) {
			fBundleManifestLocator = initializeBundleManifestLocations();
		}
		return fBundleManifestLocator;
	}

	/**
	 * Generates the relative path where source is expected to be stored in a source location.
	 * Combines the plugin id and plugin version to create a directory name and then appends
	 * the given source file path.  The result will typically be of the form
	 * PluginID_PluginVersion/src.zip.
	 * @param pluginBase the plugin that source is being looked up for
	 * @param sourceFilePath the path to append that specifies the source file location
	 * @return relative path describing where to find the source file
	 */
	private IPath getRelativePath(IPluginBase pluginBase, IPath sourceFilePath) {
		try {
			String pluginDir = pluginBase.getId();
			if (pluginDir == null) {
				return null;
			}
			String version = pluginBase.getVersion();
			if (version != null) {
				Version vid = new Version(version);
				pluginDir += "_" + vid; //$NON-NLS-1$
			}
			IPath path = IPath.fromOSString(pluginDir);
			return sourceFilePath == null ? path : path.append(sourceFilePath);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Searches through all known user specified locations, appending the relative
	 * path and checking if that file exists.
	 * @param relativePath location of source file within the source location
	 * @return path to the source file or <code>null</code> if one could not be found or if the file does not exist
	 */
	private IPath searchUserSpecifiedLocations(IPath relativePath) {
		List<IPath> userLocations = getUserLocations();
		for (IPath location : userLocations) {
			IPath fullPath = location.append(relativePath);
			File file = fullPath.toFile();
			if (file.exists()) {
				return fullPath;
			}
		}
		return null;
	}

	/**
	 * Searches through all known source locations added via extension points,
	 * appending the relative path and checking if that file exists.
	 *
	 * @param relativePath
	 *            location of source file within the source location
	 * @return path to the source file or <code>null</code> if one could not be
	 *         found or if the file does not exist
	 */
	private IPath searchExtensionLocations(IPath relativePath, IPluginBase plugin) {
		for (IPath location : getExtensionLocations()) {
			IPath fullPath = location.append(relativePath);
			File file = fullPath.toFile();
			if (file.exists()) {
				return fullPath;
			}
		}
		return getExtensions().locators.stream().map(locator -> {
			try {
				return locator.locator.locateSource(plugin);
			} catch (RuntimeException e) {
				return null;
			}
		}).filter(Objects::nonNull).findFirst().orElse(null);
	}

	/**
	 * Searches through all known source locations specified by bundle manifest entries.
	 * Checks for a source location with a bundle entry that specifies that it provides
	 * source for the given plugin.
	 * @param pluginBase the plugin we are trying to find source for
	 * @return path to the source file or <code>null</code> if one could not be found or if the file does not exist
	 */
	private IPath searchBundleManifestLocations(IPluginBase pluginBase) {
		IPath location = getBundleManifestLocation(pluginBase.getId(), new Version(pluginBase.getVersion()));
		if (location != null && location.toFile().exists()) {
			return location;
		}
		return null;
	}

	/**
	 * Parses serialized source locations into an array list of user specified source locations
	 * @param text text to parse
	 * @param entries list to add source locations to
	 */
	private void parseSavedSourceLocations(String text, List<IPath> entries) {
		text = text.replace(File.pathSeparatorChar, ';');
		StringTokenizer stok = new StringTokenizer(text, ";"); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			IPath location = parseSourceLocation(token);
			if (location != null) {
				entries.add(location);
			}
		}
	}

	/**
	 * Parses the given text into a single source location
	 * @param text text to parse
	 * @return a source location or <code>null</code>
	 */
	private IPath parseSourceLocation(String text) {
		String path;
		try {
			text = text.trim();
			int commaIndex = text.lastIndexOf(',');
			if (commaIndex == -1) {
				return IPath.fromOSString(text);
			}
			int atLoc = text.indexOf('@');
			path = (atLoc == -1) ? text.substring(0, commaIndex) : text.substring(atLoc + 1, commaIndex);
		} catch (RuntimeException e) {
			return null;
		}
		return IPath.fromOSString(path);
	}

	/**
	 * @return array of source locations that were added via extension point
	 */
	private static SourceExtensions processExtensions() {
		SourceExtensions result = new SourceExtensions();
		PDEExtensionRegistry extensionsRegistry = PDECore.getDefault().getExtensionsRegistry();
		IExtension[] extensions = extensionsRegistry.findExtensions(PDECore.PLUGIN_ID + ".source", false); //$NON-NLS-1$
		for (IExtension extension : extensions) {
			IConfigurationElement[] children = extension.getConfigurationElements();
			RegistryContributor contributor = (RegistryContributor) extension.getContributor();
			long bundleId = Long.parseLong(contributor.getActualId());
			Resource desc = PDECore.getDefault().getModelManager().getState().getState()
					.getBundle(Long.parseLong(contributor.getActualId()));
			IPluginModelBase base = null;
			if (desc != null) {
				base = PluginRegistry.findModel(desc);
			} else {
				// desc might be null if the workspace contains a plug-in with the same Bundle-SymbolicName
				ModelEntry entry = PluginRegistry.findEntry(contributor.getActualName());
				IPluginModelBase externalModels[] = entry.getExternalModels();
				for (IPluginModelBase externalModel : externalModels) {
					BundleDescription extDesc = externalModel.getBundleDescription();
					if (extDesc != null && extDesc.getBundleId() == bundleId) {
						base = externalModel;
					}
				}
			}
			if (base == null) {
				continue;
			}
			for (IConfigurationElement element : children) {
				if (element.getName().equals("location")) { //$NON-NLS-1$
					String pathValue = element.getAttribute("path"); //$NON-NLS-1$
					IPath path = IPath.fromOSString(base.getInstallLocation()).append(pathValue);
					if (path.toFile().exists()) {
						result.locations.add(path);
					}
				}
			}
		}
		// For the source locators we need to query the platform registry
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDECore.PLUGIN_ID, "dynamicSource"); //$NON-NLS-1$
		for (IExtension extension : point.getExtensions()) {
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				if (element.getName().equals("locator")) { //$NON-NLS-1$
					try {
						IPluginSourcePathLocator locator = (IPluginSourcePathLocator) element
								.createExecutableExtension("class"); //$NON-NLS-1$
						LocatorComplexity complexity = getComplexity(element);
						result.locators.add(new OrderedPluginSourcePathLocator(locator, complexity));
					} catch (CoreException e) {
						PDECore.log(e.getStatus());
					}
				}
			}
		}
		Collections.sort(result.locators);
		return result;
	}

	private static LocatorComplexity getComplexity(IConfigurationElement element) {
		try {
			return LocatorComplexity.valueOf(element.getAttribute("complexity")); //$NON-NLS-1$
		} catch (RuntimeException e) {
			return LocatorComplexity.unkown;
		}
	}

	/**
	 * Returns a bundle manifest location manager that knows about source bundles in the current
	 * platform.
	 * @return bundle manifest source location manager
	 */
	protected BundleManifestSourceLocationManager initializeBundleManifestLocations() {
		BundleManifestSourceLocationManager manager = new BundleManifestSourceLocationManager();
		manager.setPlugins(PDECore.getDefault().getModelManager().getExternalModels());
		try {
			ITargetDefinition definition = TargetPlatformService.getDefault().getWorkspaceTargetDefinition();
			if (definition != null && definition.isResolved()) {
				TargetBundle[] bundles = definition.getAllBundles();
				if (bundles != null) {
					manager.setTargetBundles(bundles);
				}
			}
		} catch (CoreException e) {
			// can't use this then...
		}
		return manager;
	}

	private static final class SourceExtensions {
		final Collection<IPath> locations = new LinkedHashSet<>();
		final List<OrderedPluginSourcePathLocator> locators = new ArrayList<>();
	}

	private static final class OrderedPluginSourcePathLocator implements Comparable<OrderedPluginSourcePathLocator> {

		private final IPluginSourcePathLocator locator;
		private final LocatorComplexity complexity;

		OrderedPluginSourcePathLocator(IPluginSourcePathLocator locator, LocatorComplexity complexity) {
			this.locator = locator;
			this.complexity = complexity;
		}

		@Override
		public int compareTo(OrderedPluginSourcePathLocator o) {
			return complexity.compareTo(o.complexity);
		}

	}

	private enum LocatorComplexity {
		low, medium, high, unkown;
	}
}
