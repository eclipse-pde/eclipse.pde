/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    SAP AG - consolidation of publishers for PDE formats
 ******************************************************************************/

package org.eclipse.pde.internal.build.site;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureManifestParser;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.xml.sax.SAXException;

public class BuildTimeFeatureParser extends FeatureManifestParser {

	protected Feature createFeature(String id, String version) {
		return new BuildTimeFeature(id, version);
	}

	public Feature parse(URL featureURL) throws SAXException, IOException {
		InputStream in = featureURL.openStream();
		try {
			return super.parse(in, featureURL);
		} finally {
			in.close();
		}
	}
}
