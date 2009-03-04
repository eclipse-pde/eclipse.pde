/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;

public class FeaturesAndBundlesTask extends AbstractPublisherTask {
	private final ArrayList features = new ArrayList();
	private final ArrayList bundles = new ArrayList();
	private URI siteXML = null;

	public void execute() throws BuildException {
		File[] f = getLocations(features);
		File[] b = getLocations(bundles);

		BuildPublisherApplication application = createPublisherApplication();
		if (f.length > 0)
			application.addAction(new FeaturesAction(f));
		if (b.length > 0)
			application.addAction(new BundlesAction(b));
		if (siteXML != null)
			application.addAction(new SiteXMLAction(siteXML, null));

		try {
			application.run(getPublisherInfo());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private File[] getLocations(List collection) {
		ArrayList results = new ArrayList();
		for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
			FileSet set = (FileSet) iterator.next();

			DirectoryScanner scanner = set.getDirectoryScanner(getProject());
			String[][] elements = new String[][] {scanner.getIncludedDirectories(), scanner.getIncludedFiles()};
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < elements[i].length; j++) {
					results.add(new File(set.getDir(), elements[i][j]));
				}
			}
		}
		return (File[]) results.toArray(new File[results.size()]);
	}

	public FileSet createFeatures() {
		FileSet set = new FileSet();
		features.add(set);
		return set;
	}

	public FileSet createBundles() {
		FileSet set = new FileSet();
		bundles.add(set);
		return set;
	}

	public void setSite(String value) {
		if (value != null && value.length() > 0 && !value.startsWith("${")) { //$NON-NLS-1$
			try {
				siteXML = URIUtil.fromString(value);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Artifact repository location (" + value + ") must be a URL."); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}
}
