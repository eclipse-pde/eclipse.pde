/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
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
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Stores location information about bundles that provide source for
 * other bundles.  Source bundles are specified by a specific entry
 * in their manifest file.  After constructing, you must call
 * setPlugins.
 * @see SourceLocationManager
 * @since 3.4
 */
public class BundleManifestSourceLocationManager {

	/**
	 * Maps SourceLocationKeys (plugin name and version) to IPluginModelBase objects representing source bundles
	 */
	private Map<SourceLocationKey, IPluginModelBase> fPluginToSourceBundle = Map.of();

	private Map<SourceLocationKey, TargetBundle> fPluginToTargetBundle = Map.of();

	/**
	 * Returns a source location that provides source for a specific plugin (specified by name and version)
	 * @param pluginName name of the plugin
	 * @param pluginVersion version of the plugin
	 * @return a source location or <code>null</code> if no location exists for this plugin
	 */
	public IPath getSourceLocation(String pluginName, Version pluginVersion) {
		return locationOf(new SourceLocationKey(pluginName, pluginVersion)).orElse(null);
	}

	private Optional<IPath> locationOf(SourceLocationKey key) {
		return pluginModel(key).or(() -> targetBundle(key));
	}

	private Optional<IPath> targetBundle(SourceLocationKey key) {
		TargetBundle targetBundle = fPluginToTargetBundle.get(key);
		if (targetBundle != null) {
			BundleInfo bundleInfo = targetBundle.getBundleInfo();
			if (bundleInfo != null) {
				URI location = bundleInfo.getLocation();
				if (location != null) {
					try {
						File file = new File(location);
						IPath sourceLocation = IPath.fromOSString(file.getAbsolutePath());
						return Optional.of(sourceLocation);
					} catch (RuntimeException e) {
						// cannot be used then...
					}
				}
			}
		}
		return Optional.empty();
	}

	private Optional<IPath> pluginModel(SourceLocationKey key) {
		IPluginModelBase plugin = fPluginToSourceBundle.get(key);
		if (plugin != null) {
			String path = plugin.getInstallLocation();
			if (path != null) {
				IPath location = IPath.fromOSString(path);
				return Optional.of(location);
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns the collection of source locations found when searching
	 * @return set of source locations, possibly empty
	 */
	public Collection<IPath> getSourceLocations() {
		return Stream.concat(fPluginToSourceBundle.keySet().stream(), fPluginToTargetBundle.keySet().stream())
				.distinct().flatMap(key -> locationOf(key).stream()).toList();
	}

	/**
	 * Returns whether this manager has a source bundle location for the given
	 * plugin name and version.
	 *
	 * @param pluginName name of the plugin to search for
	 * @param pluginVersion version of the plugin to search for
	 * @return whether this manager has a source location for the the given plugin
	 */
	public boolean hasValidSourceLocation(String pluginName, Version pluginVersion) {
		return getSourceLocation(pluginName, pluginVersion) != null;
	}

	/**
	 * Returns the source roots associated with a specific plugin name and version.
	 * The manager will look for a source location for the given plugin name/version,
	 * if one is found, it's manifest entry will be parsed and the source roots for
	 * the plugin will be returned (duplicates removed). If there are no roots
	 * specified for a plugin/version, ".", representing the root of the archive will
	 * be used as a default.
	 *
	 * @param pluginName name of the plugin to search for
	 * @param pluginVersion version of the plugin to search for
	 * @return set of String paths representing the source roots for the given plugin in the source bundle, possibly empty
	 */
	public Set<String> getSourceRoots(String pluginName, Version pluginVersion) {
		Set<String> pluginSourceRoots = new HashSet<>();
		ManifestElement[] manifestElements = getSourceEntries(pluginName, pluginVersion);
		if (manifestElements != null) {
			for (ManifestElement currentElement : manifestElements) {
				String binaryPluginName = currentElement.getValue();
				String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
				// Currently the version attribute is required
				if (binaryPluginName != null && binaryPluginName.equals(pluginName)) {
					if (versionEntry != null && versionEntry.length() > 0) {
						Version version = null;
						try {
							version = new Version(versionEntry);
						} catch (IllegalArgumentException e) {
							PDECore.log(Status.error(NLS.bind(PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, pluginName, pluginVersion), e));
						}
						if (pluginVersion.equals(version)) {
							addSourceRoots(currentElement.getDirective("roots"), pluginSourceRoots); //$NON-NLS-1$
							return pluginSourceRoots;
						}
					}
				}
			}
		}

		return pluginSourceRoots;
	}

	/**
	 * Returns all of the source roots specified in the source bundle providing
	 * source for the given plugin name and version.
	 * The manager will look for a source location for the given plugin name and
	 * version.  If one is found, it's manifest entry will be parsed and all of
	 * the source roots found in it will be returned (duplicates removed). If there
	 * are no roots specified for a plugin/version, ".", representing the root of the
	 * archive will be used as a default.
	 *
	 * @param pluginName name of the plugin to search for
	 * @param pluginVersion version of the plugin to search for
	 * @return set of String paths representing the source roots in the associated source bundle, possibly empty
	 */
	public Set<String> getAllSourceRoots(String pluginName, Version pluginVersion) {
		Set<String> pluginSourceRoots = new HashSet<>();
		ManifestElement[] manifestElements = getSourceEntries(pluginName, pluginVersion);
		if (manifestElements != null) {
			for (ManifestElement currentElement : manifestElements) {
				addSourceRoots(currentElement.getDirective("roots"), pluginSourceRoots); //$NON-NLS-1$
			}
		}
		return pluginSourceRoots;
	}

	/**
	 * Returns an array containing ManifestElements for the SourceBundle header of the source bundle for the
	 * name and version specified.  If no source bundle was found or the header was incorrectly formatted,
	 * null will be returned.
	 */
	private ManifestElement[] getSourceEntries(String pluginName, Version pluginVersion) {
		IPluginModelBase sourceBundle = fPluginToSourceBundle.get(new SourceLocationKey(pluginName, pluginVersion));
		if (sourceBundle != null) {
			if (sourceBundle.getPluginBase() instanceof PluginBase) {
				String bundleSourceEntry = ((PluginBase) sourceBundle.getPluginBase()).getBundleSourceEntry();
				if (bundleSourceEntry != null) {
					try {
						return ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, bundleSourceEntry);
					} catch (BundleException e) {
						PDECore.log(Status.error(NLS.bind(PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, pluginName, pluginVersion), e));
					}
				}
			}
		}
		return null;
	}

	/**
	 * Parses and adds the values of rootEntryDirective to the pluginSourceRoots set.
	 * @param rootEntryDirective - value of the "roots" directive of a SourceBundle header.
	 * @param pluginSourceRoots - set of pluginSourceRoots
	 */
	private void addSourceRoots(String rootEntryDirective, Set<String> pluginSourceRoots) {
		if (rootEntryDirective != null) {
			String[] roots = rootEntryDirective.split(","); //$NON-NLS-1$
			Collections.addAll(pluginSourceRoots, roots);
		} else {
			pluginSourceRoots.add("."); //$NON-NLS-1$
		}
	}

	/**
	 * Searches through the specified bundles for source bundles.  Source bundles are determined by
	 * looking for a specific entry in the plugin manifest.
	 * @param externalModels bundles to search through
	 */
	public void setPlugins(IPluginModelBase[] externalModels) {
		fPluginToSourceBundle = new LinkedHashMap<>();
		for (IPluginModelBase model : externalModels) {
			IPluginBase currentPlugin = model.getPluginBase();
			if (currentPlugin instanceof PluginBase) {
				String bundleSourceEntry = ((PluginBase) currentPlugin).getBundleSourceEntry();
				if (bundleSourceEntry != null) {
					ManifestElement[] manifestElements = null;
					try {
						manifestElements = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, bundleSourceEntry);
					} catch (BundleException e) {
						PDECore.log(Status.error(NLS.bind(PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, currentPlugin.getId(), currentPlugin.getVersion()), e));
					}
					if (manifestElements != null) {
						IPath path = IPath.fromOSString(model.getInstallLocation());
						if (path.toFile().exists()) {
							for (ManifestElement element : manifestElements) {
								String binaryPluginName = element.getValue();
								String versionEntry = element.getAttribute(Constants.VERSION_ATTRIBUTE);
								// Currently the version attribute is required
								if (binaryPluginName != null && binaryPluginName.length() > 0 && versionEntry != null && versionEntry.length() > 0) {
									Version version = null;
									try {
										version = new Version(versionEntry);
									} catch (IllegalArgumentException e) {
										PDECore.log(Status.error(NLS.bind(PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, new Object[] {currentPlugin.getName(), versionEntry, path.toString()}), e));

									}
									fPluginToSourceBundle.put(new SourceLocationKey(binaryPluginName, version), model);
								} else {
									PDECore.log(Status.warning(NLS.bind(PDECoreMessages.BundleManifestSourceLocationManager_problemProcessBundleManifestHeaderAttributeMissing, currentPlugin.getName())));
								}
							}
						}
					}
				}
			}
		}
	}

	public void setTargetBundles(TargetBundle[] bundles) {
		fPluginToTargetBundle = new LinkedHashMap<>();
		for (TargetBundle bundle : bundles) {

			if (bundle.isSourceBundle()) {
				// collect it in the map ...
				BundleInfo sourceTarget = bundle.getSourceTarget();
				if (sourceTarget != null) {
					try {
						Version version = Version.parseVersion(sourceTarget.getVersion());
						fPluginToTargetBundle.put(new SourceLocationKey(sourceTarget.getSymbolicName(), version),
								bundle);
					} catch (IllegalArgumentException e) {
						// can't parse the version...
					}
				}
			}
		}
	}
}
