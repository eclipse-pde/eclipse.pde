/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

public class AssemblyInformation {
	// The list of all the features and plugins to assemble listed on a per config basis 
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

	public Collection getFeatures(Config config) {
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
		private Collection features = new HashSet(7);
		// indicate whether root files needs to be copied and where they are coming from
		private Collection rootFileProviders = new HashSet(7);

		public void addRootFileProvider(IFeature feature) {
			for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = (BuildTimeFeature) iter.next();
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

		public Collection getFeatures() {
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
