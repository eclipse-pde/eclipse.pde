/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.osgi.framework.Version;

public class AssemblyInformation implements IPDEBuildConstants {
	// List all the features and plugins to assemble sorted on a per config basis 
	//	key: string[] representing the tuple of a config 
	// value: (AssemblyLevelConfigInfo) representing the info for the given config
	private final Map assembleInformation = new HashMap(8);
	private final Map bundleMap = new HashMap();
	private final Map rootMap = new HashMap();

	public AssemblyInformation() {
		// Initialize the content of the assembly information with the configurations 
		for (Iterator iter = AbstractScriptGenerator.getConfigInfos().iterator(); iter.hasNext();) {
			assembleInformation.put(iter.next(), new AssemblyLevelConfigInfo());
		}
	}

	public void addFeature(Config config, BuildTimeFeature feature) {
		AssemblyLevelConfigInfo entry = (AssemblyLevelConfigInfo) assembleInformation.get(config);
		entry.addFeature(feature);
	}

	public void removeFeature(Config config, BuildTimeFeature feature) {
		AssemblyLevelConfigInfo entry = (AssemblyLevelConfigInfo) assembleInformation.get(config);
		entry.removeFeature(feature);
	}

	public void addPlugin(Config config, BundleDescription plugin) {
		AssemblyLevelConfigInfo entry = (AssemblyLevelConfigInfo) assembleInformation.get(config);
		entry.addPlugin(plugin);

		String id = plugin.getSymbolicName();
		BundleDescription existing = (BundleDescription) bundleMap.get(id);
		if (existing == null || existing.getVersion().compareTo(plugin.getVersion()) < 0)
			bundleMap.put(id, plugin);
		bundleMap.put(id + '_' + plugin.getVersion().toString(), plugin);
	}

	public BundleDescription getPlugin(String id, String version) {
		if (version != null && !GENERIC_VERSION_NUMBER.equals(version))
			return (BundleDescription) bundleMap.get(id + '_' + version);
		return (BundleDescription) bundleMap.get(id);
	}

	public BuildTimeFeature getRootProvider(String id, String version) {
		if (version != null && !GENERIC_VERSION_NUMBER.equals(version))
			return (BuildTimeFeature) rootMap.get(id + '_' + version);
		return (BuildTimeFeature) rootMap.get(id);
	}

	public Collection getPlugins(Config config) {
		return ((AssemblyLevelConfigInfo) assembleInformation.get(config)).getPlugins();
	}

	public Set getAllPlugins() {
		Collection pluginsByConfig = assembleInformation.values();
		Set result = new LinkedHashSet();
		for (Iterator iter = pluginsByConfig.iterator(); iter.hasNext();) {
			Collection allPlugins = ((AssemblyLevelConfigInfo) iter.next()).getPlugins();
			result.addAll(allPlugins);
		}
		return result;
	}

	public Collection getBinaryPlugins(Config config) {
		Collection allPlugins = getPlugins(config);
		Set result = new LinkedHashSet(allPlugins.size());
		for (Iterator iter = allPlugins.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			Properties bundleProperties = ((Properties) bundle.getUserObject());
			if (bundleProperties == null || bundleProperties.get(IS_COMPILED) == null || Boolean.FALSE == bundleProperties.get(IS_COMPILED))
				result.add(bundle);
		}
		return result;
	}

	public Collection getCompiledPlugins(Config config) {
		Collection allPlugins = getPlugins(config);
		Set result = new LinkedHashSet(allPlugins.size());
		for (Iterator iter = allPlugins.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			Properties bundleProperties = ((Properties) bundle.getUserObject());
			if (bundleProperties != null && Boolean.TRUE == bundleProperties.get(IS_COMPILED))
				result.add(bundle);
		}
		return result;
	}

	public Set getAllCompiledPlugins() {
		Collection pluginsByConfig = assembleInformation.values();
		Set result = new LinkedHashSet();
		for (Iterator iter2 = pluginsByConfig.iterator(); iter2.hasNext();) {
			Collection allPlugins = ((AssemblyLevelConfigInfo) iter2.next()).getPlugins();
			for (Iterator iter = allPlugins.iterator(); iter.hasNext();) {
				BundleDescription bundle = (BundleDescription) iter.next();
				if (!Utils.isBinary(bundle)) {
					result.add(bundle);
				}
			}
		}
		return result;
	}

	public Collection getCompiledFeatures(Config config) {
		Collection allFeatures = getFeatures(config);
		ArrayList result = new ArrayList(allFeatures.size());
		for (Iterator iter = allFeatures.iterator(); iter.hasNext();) {
			Object tmp = iter.next();
			if (tmp instanceof BuildTimeFeature) {
				if (!((BuildTimeFeature) tmp).isBinary())
					result.add(tmp);
			}
		}
		return result;
	}

	public Collection getBinaryFeatures(Config config) {
		Collection allFeatures = getFeatures(config);
		ArrayList result = new ArrayList(allFeatures.size());
		for (Iterator iter = allFeatures.iterator(); iter.hasNext();) {
			Object tmp = iter.next();
			if (tmp instanceof BuildTimeFeature) {
				if (((BuildTimeFeature) tmp).isBinary())
					result.add(tmp);
			} else {
				result.add(tmp);
			}
		}
		return result;
	}

	public ArrayList getFeatures(Config config) {
		return ((AssemblyLevelConfigInfo) assembleInformation.get(config)).getFeatures();
	}

	public boolean copyRootFile(Config config) {
		return ((AssemblyLevelConfigInfo) assembleInformation.get(config)).hasRootFile();
	}

	public Collection getRootFileProviders(Config config) {
		return ((AssemblyLevelConfigInfo) assembleInformation.get(config)).getRootFileProvider();
	}

	public void addRootFileProvider(Config config, BuildTimeFeature feature) {
		((AssemblyLevelConfigInfo) assembleInformation.get(config)).addRootFileProvider(feature);

		String id = feature.getId();
		BuildTimeFeature existing = (BuildTimeFeature) rootMap.get(id);
		if (existing == null || new Version(existing.getVersion()).compareTo(new Version(feature.getVersion())) < 0)
			rootMap.put(id, feature);
		rootMap.put(id + '_' + feature.getVersion(), feature);
	}

	// All the information that will go into the assemble file for a specific info
	protected static class AssemblyLevelConfigInfo {
		// the plugins that are contained into this config
		private final Collection plugins = new LinkedHashSet(20);
		// the features that are contained into this config
		private final ArrayList features = new ArrayList(7);
		// indicate whether root files needs to be copied and where they are coming from
		private final LinkedList rootFileProviders = new LinkedList();

		public void addRootFileProvider(BuildTimeFeature feature) {
			if (rootFileProviders.contains(feature))
				return;
			for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
				if (feature == featureDescriptor)
					return;
				if (feature.getId().equals(featureDescriptor.getId()) && feature.getVersion().equals(featureDescriptor.getVersion()))
					return;
			}
			rootFileProviders.add(feature);
		}

		public Collection getRootFileProvider() {
			return rootFileProviders;
		}

		public boolean hasRootFile() {
			return rootFileProviders.size() > 0;
		}

		public ArrayList getFeatures() {
			return features;
		}

		public Collection getPlugins() {
			return plugins;
		}

		public void addFeature(BuildTimeFeature feature) {
			for (Iterator iter = features.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
				if (feature.getId().equals(featureDescriptor.getId()) && (feature).getVersion().equals(featureDescriptor.getVersion()))
					return;
			}
			features.add(feature);
		}

		public void addPlugin(BundleDescription plugin) {
			plugins.add(plugin);
		}

		public void removeFeature(BuildTimeFeature feature) {
			for (Iterator iter = features.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
				if (feature.getId().equals(featureDescriptor.getId()) && feature.getVersion().equals(featureDescriptor.getVersion())) {
					features.remove(featureDescriptor);
					return;
				}
			}
		}
	}
}
