/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build.site;

import java.io.IOException;
import java.net.URL;
import org.eclipse.pde.internal.build.site.compatibility.Feature;
import org.eclipse.pde.internal.build.site.compatibility.FeatureParser;
import org.xml.sax.SAXException;


public class BuildTimeFeatureParser extends FeatureParser {
	
	protected Feature createFeature(String id, String version) {
		return new BuildTimeFeature(id, version);
	}

	public Feature parse(URL featureURL) throws SAXException, IOException {
		return super.parse(featureURL);
	}
}
