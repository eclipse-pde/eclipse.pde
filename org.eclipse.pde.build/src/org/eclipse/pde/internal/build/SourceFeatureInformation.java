/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;

import org.eclipse.osgi.service.resolver.BundleDescription;

public class SourceFeatureInformation implements IPDEBuildConstants {
	// Key : a configuration 
	// Value : the list of plugins that needs to get copied into a specific config.
	// This list will be used to build the content of the fragment that contains 
	// config specific code
	private Map sourceFeatureInformation = new HashMap(8);

	public SourceFeatureInformation() {
		// Initialize the content of the assembly information with the configurations
		for (Iterator iter = AbstractScriptGenerator.getConfigInfos().iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			sourceFeatureInformation.put(config, new HashSet());
		}
		sourceFeatureInformation.put(Config.genericConfig(), new HashSet(2));
	}

	public void addElementEntry(Config config, BundleDescription plugin) {
		Set entry = (Set) sourceFeatureInformation.get(config);
		entry.add(plugin);
	}

	public Map getElementEntries() {
		return sourceFeatureInformation;
	}
}
