/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.pde.internal.build.site.BuildTimeSite;

public class SourceFeatureWriter extends FeatureWriter {

	public SourceFeatureWriter(OutputStream out, Feature feature, BuildTimeSite site) {
		super(out, feature, site);
	}

	@Override
	public void printIncludes() {
		Map<String, String> parameters = new LinkedHashMap<>();
		// TO CHECK Here we should have the raw list...
		FeatureEntry[] features = feature.getEntries();
		for (FeatureEntry feature2 : features) {
			if (feature2.isRequires() || feature2.isPlugin()) {
				continue;
			}
			parameters.clear();
			parameters.put(ID, feature2.getId());
			parameters.put(VERSION, feature2.getVersion());
			if (feature2.isOptional()) {
				parameters.put("optional", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (feature2.getArch() != null) {
				parameters.put("arch", feature2.getArch()); //$NON-NLS-1$
			}
			if (feature2.getWS() != null) {
				parameters.put("ws", feature2.getWS()); //$NON-NLS-1$
			}
			if (feature2.getOS() != null) {
				parameters.put("os", feature2.getOS()); //$NON-NLS-1$
			}
			printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
	}
}
