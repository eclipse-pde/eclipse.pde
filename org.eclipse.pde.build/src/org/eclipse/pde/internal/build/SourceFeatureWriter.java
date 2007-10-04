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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.eclipse.pde.internal.build.site.compatibility.Feature;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;

public class SourceFeatureWriter extends FeatureWriter {

	public SourceFeatureWriter(OutputStream out, Feature feature, BuildTimeSite site) throws IOException {
		super(out, feature, site);
	}

	public void printIncludes() {
		Map parameters = new LinkedHashMap();
		// TO CHECK Here we should have the raw list...
		FeatureEntry[] features = feature.getEntries();
		for (int i = 0; i < features.length; i++) {
			if (features[i].isRequires() || features[i].isPlugin())
				continue;
			parameters.clear();
			parameters.put("id", features[i].getId()); //$NON-NLS-1$
			parameters.put("version", features[i].getVersion()); //$NON-NLS-1$

			printTag("includes", parameters, true, true, true); //$NON-NLS-1$
		}
	}
}
