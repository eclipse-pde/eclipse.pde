/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.osgi.framework.*;

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
	private Map fPluginToSourceBundle = new HashMap(0);

	/**
	 * Returns a source location that provides source for a specific plugin (specified by name and version)
	 * @param pluginName name of the plugin
	 * @param pluginVersion version of the plugin
	 * @return a source location or <code>null</code> if no location exists for this plugin
	 */
	public SourceLocation getSourceLocation(String pluginName, Version pluginVersion) {
		IPluginModelBase plugin = (IPluginModelBase) fPluginToSourceBundle.get(new SourceLocationKey(pluginName, pluginVersion));
		if (plugin != null) {
			SourceLocation location = new SourceLocation(new Path(plugin.getInstallLocation()));
			location.setUserDefined(false);
			return location;
		}
		return null;
	}

	/**
	 * Returns the collection of source locations found when searching
	 * @return set of source locations, possibly empty
	 */
	public Collection getSourceLocations() {
		Collection result = new ArrayList(fPluginToSourceBundle.values().size());
		for (Iterator iterator = fPluginToSourceBundle.values().iterator(); iterator.hasNext();) {
			IPluginModelBase currentBundle = (IPluginModelBase) iterator.next();
			SourceLocation currentLocation = new SourceLocation(new Path(currentBundle.getInstallLocation()));
			currentLocation.setUserDefined(false);
			result.add(currentLocation);
		}
		return result;
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
		return fPluginToSourceBundle.containsKey(new SourceLocationKey(pluginName, pluginVersion));
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
	public Set getSourceRoots(String pluginName, Version pluginVersion) {
		Set pluginSourceRoots = new HashSet();
		ManifestElement[] manifestElements = getSourceEntries(pluginName, pluginVersion);
		if (manifestElements != null) {
			for (int j = 0; j < manifestElements.length; j++) {
				ManifestElement currentElement = manifestElements[j];
				String binaryPluginName = currentElement.getValue();
				String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
				// Currently the version attribute is required
				if (binaryPluginName != null && binaryPluginName.equals(pluginName)) {
					if (versionEntry != null && versionEntry.length() > 0) {
						Version version = null;
						try {
							version = new Version(versionEntry);
						} catch (IllegalArgumentException e) {
							PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, e));
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
	public Set getAllSourceRoots(String pluginName, Version pluginVersion) {
		Set pluginSourceRoots = new HashSet();
		ManifestElement[] manifestElements = getSourceEntries(pluginName, pluginVersion);
		if (manifestElements != null) {
			for (int j = 0; j < manifestElements.length; j++) {
				ManifestElement currentElement = manifestElements[j];
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
		IPluginModelBase sourceBundle = (IPluginModelBase) fPluginToSourceBundle.get(new SourceLocationKey(pluginName, pluginVersion));
		if (sourceBundle != null) {
			if (sourceBundle.getPluginBase() instanceof PluginBase) {
				String bundleSourceEntry = ((PluginBase) sourceBundle.getPluginBase()).getBundleSourceEntry();
				if (bundleSourceEntry != null) {
					try {
						return ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, bundleSourceEntry);
					} catch (BundleException e) {
						PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, e));
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
	private void addSourceRoots(String rootEntryDirective, Set pluginSourceRoots) {
		if (rootEntryDirective != null) {
			String[] roots = rootEntryDirective.split(","); //$NON-NLS-1$
			for (int k = 0; k < roots.length; k++) {
				pluginSourceRoots.add(roots[k]);
			}
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
		fPluginToSourceBundle = new HashMap();
		for (int i = 0; i < externalModels.length; i++) {
			IPluginBase currentPlugin = externalModels[i].getPluginBase();
			if (currentPlugin instanceof PluginBase) {
				String bundleSourceEntry = ((PluginBase) currentPlugin).getBundleSourceEntry();
				if (bundleSourceEntry != null) {
					ManifestElement[] manifestElements = null;
					try {
						manifestElements = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, bundleSourceEntry);
					} catch (BundleException e) {
						PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, e));
						manifestElements = null;
					}
					if (manifestElements != null) {
						IPath path = new Path(externalModels[i].getInstallLocation());
						if (path.toFile().exists()) {
							for (int j = 0; j < manifestElements.length; j++) {
								ManifestElement currentElement = manifestElements[j];
								String binaryPluginName = currentElement.getValue();
								String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
								// Currently the version attribute is required
								if (binaryPluginName != null && binaryPluginName.length() > 0 && versionEntry != null && versionEntry.length() > 0) {
									Version version = null;
									try {
										version = new Version(versionEntry);
									} catch (IllegalArgumentException e) {
										PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader, new Object[] {currentPlugin.getName(), versionEntry, path.toString()}), e));

									}
									fPluginToSourceBundle.put(new SourceLocationKey(binaryPluginName, version), externalModels[i]);
								} else {
									PDECore.log(new Status(IStatus.WARNING, PDECore.PLUGIN_ID, NLS.bind(PDECoreMessages.BundleManifestSourceLocationManager_problemProcessBundleManifestHeaderAttributeMissing, currentPlugin.getName())));
								}
							}
						}
					}
				}
			}
		}
	}
}
