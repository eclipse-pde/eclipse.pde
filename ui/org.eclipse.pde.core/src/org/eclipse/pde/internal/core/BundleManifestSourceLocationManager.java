/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
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

	private Map fNameVersionMapping = null;
	
	/**
	 * Returns a source location that provides source for a specific plugin (specified by name and version)
	 * @param pluginName name of the plugin
	 * @param version version of the plugin
	 * @return a source location or <code>null</code>
	 */
	public SourceLocation getSourceLocation(String pluginName, Version version){
		if (fNameVersionMapping == null)
			return null;
		return (SourceLocation)fNameVersionMapping.get(new SourceLocationKey(pluginName, version));
	}
	
	/**
	 * Returns the set of source locations found when searching
	 * @return set of source locations, possibly empty
	 */
	public Collection getSourceLocations() {
		if (fNameVersionMapping == null)
			return Collections.EMPTY_SET;
		return fNameVersionMapping.values();
	}
	
	/**
	 * Searches through the specified bundles for source bundles.  Source bundles are determined by
	 * looking for a specific entry in the plugin manifest.
	 * @param externalModels bundles to search through
	 */
	public void setPlugins(IPluginModelBase[] externalModels){
		fNameVersionMapping = new HashMap();
		for (int i = 0; i < externalModels.length; i++) {
			IPluginBase currentPlugin = externalModels[i].getPluginBase();
			if (currentPlugin instanceof PluginBase){
				String bundleSourceEntry = ((PluginBase)currentPlugin).getBundleSourceEntry();
				if (bundleSourceEntry != null){
					ManifestElement[] manifestElements = null;
					try{
						manifestElements = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, bundleSourceEntry);
					} catch (BundleException e){
						PDECore.log(new Status(IStatus.ERROR,PDECore.PLUGIN_ID,PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader,e));
						manifestElements = null;
					}
					if (manifestElements != null){
						for (int j = 0; j < manifestElements.length; j++) {
							ManifestElement currentElement = manifestElements[j];
							String binaryPluginName = currentElement.getValue();
							String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
							// Currently the version attribute is required
							if (binaryPluginName != null && binaryPluginName.length() > 0 && versionEntry != null && versionEntry.length() > 0){
								IPath path = new Path(externalModels[i].getInstallLocation());
								if (path.toFile().exists()) {
									SourceLocation location = new SourceLocation(path);
									location.setUserDefined(false);
									Version version = null;
									if (versionEntry != null){
										try{
											version = new Version(versionEntry);
										} catch (IllegalArgumentException e){
											PDECore.log(new Status(IStatus.ERROR,PDECore.PLUGIN_ID,PDECoreMessages.SourceLocationManager_problemProcessingBundleManifestSourceHeader,e));										
										}
									}
									fNameVersionMapping.put(new SourceLocationKey(binaryPluginName,version), location);
								}
							} else {
								PDECore.log(new Status(IStatus.WARNING,PDECore.PLUGIN_ID,NLS.bind(PDECoreMessages.BundleManifestSourceLocationManager_problemProcessBundleManifestHeaderAttributeMissing, currentPlugin.getName())));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Used as the key for the bundle manifest location map.  Contains
	 * both the bundle name and the version.  The version attribute can
	 * be null.
	 * @since 3.4
	 */
	class SourceLocationKey {
		private String fBundleName;
		private Version fVersion;
		
		public SourceLocationKey(String bundleName, Version version){
			fBundleName = bundleName;
			fVersion = version;
		}
		
		public SourceLocationKey(String bundleName){
			this(bundleName,null);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof SourceLocationKey){
				SourceLocationKey key = (SourceLocationKey)obj;
				if (fVersion != null && key.fVersion != null){
					return fBundleName.equals(((SourceLocationKey) obj).fBundleName) && fVersion.equals(((SourceLocationKey) obj).fVersion);
				} else if (fVersion == null && key.fVersion == null){
					return fBundleName.equals(((SourceLocationKey) obj).fBundleName);
				}
			}
			return false;
		}
		
		public int hashCode(){
			if (fVersion == null){
				return fBundleName.hashCode();
			}
			int result = 1;
			result = 31 * result + fBundleName.hashCode();
			result = 31 * result + fVersion.hashCode();
			return result;
		}
	}

}
