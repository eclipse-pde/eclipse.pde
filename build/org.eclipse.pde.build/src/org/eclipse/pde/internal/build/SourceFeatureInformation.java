/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.service.resolver.BundleDescription;

public class SourceFeatureInformation implements IPDEBuildConstants {
	// Key : a source bundle 
	// Value : the list of plugins that needs to get copied into the given source bundle
	// This list will be used to build the content of the fragment that contains 
	// config specific code
	private final Map<String, Set<BundleDescription>> sourceFeatureInformation = new HashMap<>(8);

	public SourceFeatureInformation() {
		//empty
	}

	public void addElementEntry(String bundle, BundleDescription plugin) {
		Set<BundleDescription> entry = sourceFeatureInformation.get(bundle);
		if (entry == null) {
			entry = new HashSet<>();
			sourceFeatureInformation.put(bundle, entry);
		}
		entry.add(plugin);
	}

	public Map<String, Set<BundleDescription>> getElementEntries() {
		return sourceFeatureInformation;
	}
}
