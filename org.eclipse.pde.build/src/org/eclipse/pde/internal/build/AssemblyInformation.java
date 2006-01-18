/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.update.core.IFeature;

public class AssemblyInformation implements IPDEBuildConstants {
	// List all the features and plugins to assemble sorted on a per config basis 
	//	key: string[] representing the tuple of a config 
	// value: (AssemblyLevelConfigInfo) representing the info for the given config
	private Map assembleInformation = new HashMap(8);

	public AssemblyInformation() {
		// Initialize the content of the assembly information with the configurations 
		for (Iterator iter = AbstractScriptGenerator.getConfigInfos().iterator(); iter.hasNext();) {
			assembleInformation.put(iter.next(), new AssemblyLevelConfigInfo());
		}
	}

	public void addFeature(Config config, IFeature feature) {
		AssemblyLevelConfigInfo entry = (AssemblyLevelConfigInfo) assembleInformation.get(config);
		entry.addFeature(feature);
	}

	public void removeFeature(Config config, IFeature feature) {
		AssemblyLevelConfigInfo entry = (AssemblyLevelConfigInfo) assembleInformation.get(config);
		entry.removeFeature(feature);
	}

	public void addPlugin(Config config, BundleDescription plugin) {
		AssemblyLevelConfigInfo entry = (AssemblyLevelConfigInfo) assembleInformation.get(config);
		entry.addPlugin(plugin);
	}

	public Collection getPlugins(Config config) {
		return ((AssemblyLevelConfigInfo) assembleInformation.get(config)).getPlugins();
	}

	public Collection getBinaryPlugins(Config config) {
		Collection allPlugins = getPlugins(config);
		Set result = new HashSet(allPlugins.size()); 
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
		Set result = new HashSet(allPlugins.size()); 
		for (Iterator iter = allPlugins.iterator(); iter.hasNext();) {
			BundleDescription bundle = (BundleDescription) iter.next();
			Properties bundleProperties = ((Properties) bundle.getUserObject());
			if (bundleProperties != null && Boolean.TRUE == bundleProperties.get(IS_COMPILED))
				result.add(bundle);
		}
		return result;
	}
	
	public Collection getAllCompiledPlugins() {
		Collection pluginsByConfig = assembleInformation.values();
		Set result = new HashSet(); 
		for (Iterator iter2 = pluginsByConfig.iterator(); iter2.hasNext();) {
			Collection allPlugins = ((AssemblyLevelConfigInfo) iter2.next()).getPlugins();
			for (Iterator iter = allPlugins.iterator(); iter.hasNext();) {
				BundleDescription bundle = (BundleDescription) iter.next();
				Properties bundleProperties = ((Properties) bundle.getUserObject());
				if (bundleProperties != null && Boolean.TRUE == bundleProperties.get(IS_COMPILED))
					result.add(bundle);
			}
			
		}
		return result;
	}
	
	public Collection getCompiledFeatures(Config config) {
		Collection allFeatures= getFeatures(config);
		ArrayList result = new ArrayList(allFeatures.size()); 
		for (Iterator iter = allFeatures.iterator(); iter.hasNext();) {
			Object tmp = iter.next();
			if (tmp instanceof BuildTimeFeature) {
				if (! ((BuildTimeFeature) tmp).isBinary())
					result.add(tmp);
			}
		}
		return result;
	}
	
	public Collection getBinaryFeatures(Config config) {
		Collection allFeatures= getFeatures(config);
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

	public void addRootFileProvider(Config config, IFeature feature) {
		((AssemblyLevelConfigInfo) assembleInformation.get(config)).addRootFileProvider(feature);
	}

	// All the information that will go into the assemble file for a specific info
	private class AssemblyLevelConfigInfo {
		// the plugins that are contained into this config
		private Collection plugins = new HashSet(20);
		// the features that are contained into this config
		private ArrayList features = new ArrayList(7);
		// indicate whether root files needs to be copied and where they are coming from
		private LinkedList rootFileProviders = new LinkedList();

		public void addRootFileProvider(IFeature feature) {
			if (rootFileProviders.contains(feature))
				return;
			for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
				if (feature == featureDescriptor)
					return;
				if (((BuildTimeFeature) feature).getFeatureIdentifier().equals(featureDescriptor.getFeatureIdentifier()) && ((BuildTimeFeature) feature).getFeatureVersion().equals(featureDescriptor.getFeatureVersion()))
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

		public void addFeature(IFeature feature) {
			for (Iterator iter = features.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
				if (((BuildTimeFeature) feature).getFeatureIdentifier().equals(featureDescriptor.getFeatureIdentifier()) && ((BuildTimeFeature) feature).getFeatureVersion().equals(featureDescriptor.getFeatureVersion()))
					return;
			}
			features.add(feature);
		}

		public void addPlugin(BundleDescription plugin) {
			plugins.add(plugin);
		}

		public void removeFeature(IFeature feature) {
			for (Iterator iter = features.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
				if (((BuildTimeFeature) feature).getFeatureIdentifier().equals(featureDescriptor.getFeatureIdentifier()) && ((BuildTimeFeature) feature).getFeatureVersion().equals(featureDescriptor.getFeatureVersion())) {
					features.remove(featureDescriptor);
					return;
				}
			}
		}
	}
}
