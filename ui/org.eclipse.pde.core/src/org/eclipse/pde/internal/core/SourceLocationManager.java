/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
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
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Version;

/**
 * Manages where PDE should look when looking for source.  The locations may
 * be specified by the user, by extension, or by bundle manifest entry.
 */
public class SourceLocationManager implements ICoreConstants {

	/**
	 * List of source locations that have been discovered using extension points
	 */
	private List<SourceLocation> fExtensionLocations = null;

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
				result = searchExtensionLocations(relativePath);
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
			result = searchExtensionLocations(relativePath);
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
	public List<SourceLocation> getUserLocations() {
		List<SourceLocation> userLocations = new ArrayList<>();
		String pref = PDECore.getDefault().getPreferencesManager().getString(P_SOURCE_LOCATIONS);
		if (pref.length() > 0) {
			parseSavedSourceLocations(pref, userLocations);
		}
		return userLocations;
	}

	/**
	 * @return array of source locations that have been added via extension point
	 */
	public List<SourceLocation> getExtensionLocations() {
		if (fExtensionLocations == null) {
			fExtensionLocations = processExtensions();
		}
		return fExtensionLocations;
	}

	/**
	 * @return array of source locations defined by a bundle manifest entry
	 */
	public Collection<SourceLocation> getBundleManifestLocations() {
		return getBundleManifestLocator().getSourceLocations();
	}

	/**
	 * @return source location that was specified by a bundle manifest entry to provide source for the given plugin.
	 */
	private SourceLocation getBundleManifestLocation(String pluginID, Version version) {
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
			IPath path = new Path(pluginDir);
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
		List<SourceLocation> userLocations = getUserLocations();
		for (SourceLocation location : userLocations) {
			IPath fullPath = location.getPath().append(relativePath);
			File file = fullPath.toFile();
			if (file.exists()) {
				return fullPath;
			}
		}
		return null;
	}

	/**
	 * Searches through all known source locations added via extension points, appending
	 * the relative path and checking if that file exists.
	 * @param relativePath location of source file within the source location
	 * @return path to the source file or <code>null</code> if one could not be found or if the file does not exist
	 */
	private IPath searchExtensionLocations(IPath relativePath) {
		List<SourceLocation> extensionLocations = getExtensionLocations();
		for (SourceLocation location : extensionLocations) {
			IPath fullPath = location.getPath().append(relativePath);
			File file = fullPath.toFile();
			if (file.exists()) {
				return fullPath;
			}
		}
		return null;
	}

	/**
	 * Searches through all known source locations specified by bundle manifest entries.
	 * Checks for a source location with a bundle entry that specifies that it provides
	 * source for the given plugin.
	 * @param pluginBase the plugin we are trying to find source for
	 * @return path to the source file or <code>null</code> if one could not be found or if the file does not exist
	 */
	private IPath searchBundleManifestLocations(IPluginBase pluginBase) {
		SourceLocation location = getBundleManifestLocation(pluginBase.getId(), new Version(pluginBase.getVersion()));
		if (location != null && location.getPath().toFile().exists()) {
			return location.getPath();
		}
		return null;
	}

	/**
	 * Parses serialized source locations into an array list of user specified source locations
	 * @param text text to parse
	 * @param entries list to add source locations to
	 */
	private void parseSavedSourceLocations(String text, List<SourceLocation> entries) {
		text = text.replace(File.pathSeparatorChar, ';');
		StringTokenizer stok = new StringTokenizer(text, ";"); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			SourceLocation location = parseSourceLocation(token);
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
	private SourceLocation parseSourceLocation(String text) {
		String path;
		try {
			text = text.trim();
			int commaIndex = text.lastIndexOf(',');
			if (commaIndex == -1) {
				return new SourceLocation(new Path(text));
			}

			int atLoc = text.indexOf('@');
			path = (atLoc == -1) ? text.substring(0, commaIndex) : text.substring(atLoc + 1, commaIndex);
		} catch (RuntimeException e) {
			return null;
		}
		return new SourceLocation(new Path(path));
	}

	/**
	 * @return array of source locations that were added via extension point
	 */
	private static List<SourceLocation> processExtensions() {
		ArrayList<SourceLocation> result = new ArrayList<>();
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions(PDECore.PLUGIN_ID + ".source", false); //$NON-NLS-1$
		for (IExtension extension : extensions) {
			IConfigurationElement[] children = extension.getConfigurationElements();
			RegistryContributor contributor = (RegistryContributor) extension.getContributor();
			long bundleId = Long.parseLong(contributor.getActualId());
			BundleDescription desc = PDECore.getDefault().getModelManager().getState().getState().getBundle(Long.parseLong(contributor.getActualId()));
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
					IPath path = new Path(base.getInstallLocation()).append(pathValue);
					if (path.toFile().exists()) {
						SourceLocation location = new SourceLocation(path);
						location.setUserDefined(false);
						if (!result.contains(location)) {
							result.add(location);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns a bundle manifest location manager that knows about source bundles in the current
	 * platform.
	 * @return bundle manifest source location manager
	 */
	protected BundleManifestSourceLocationManager initializeBundleManifestLocations() {
		BundleManifestSourceLocationManager manager = new BundleManifestSourceLocationManager();
		manager.setPlugins(PDECore.getDefault().getModelManager().getExternalModels());
		return manager;
	}

}
