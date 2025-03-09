/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;

public class FeaturesAndBundlesTask extends AbstractPublisherTask {
	private final ArrayList<FileSet> features = new ArrayList<>();
	private final ArrayList<FileSet> bundles = new ArrayList<>();
	private URI siteXML = null;
	private URI categoryXML = null;
	private String siteQualifier = ""; //$NON-NLS-1$
	private String categoryVersion = null;

	@Override
	public void execute() throws BuildException {
		File[] f = getLocations(features);
		File[] b = getLocations(bundles);

		BuildPublisherApplication application = createPublisherApplication();
		if (f.length > 0) {
			application.addAction(new FeaturesAction(f));
		}
		if (b.length > 0) {
			application.addAction(new BundlesAction(b));
		}
		if (siteXML != null) {
			SiteXMLAction action = new SiteXMLAction(siteXML, siteQualifier);
			if (categoryVersion != null) {
				action.setCategoryVersion(categoryVersion);
			}
			application.addAction(action);
		}
		if (categoryXML != null) {
			CategoryXMLAction action = new CategoryXMLAction(categoryXML, siteQualifier);
			if (categoryVersion != null) {
				action.setCategoryVersion(categoryVersion);
			}
			application.addAction(action);
		}

		try {
			application.run(getPublisherInfo());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private File[] getLocations(List<FileSet> collection) {
		ArrayList<File> results = new ArrayList<>();
		for (FileSet set : collection) {
			DirectoryScanner scanner = set.getDirectoryScanner(getProject());
			String[][] elements = new String[][] {scanner.getIncludedDirectories(), scanner.getIncludedFiles()};
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < elements[i].length; j++) {
					results.add(new File(set.getDir(), elements[i][j]));
				}
			}
		}
		return results.toArray(new File[results.size()]);
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

	public void setSiteQualifier(String siteQualifier) {
		if (siteQualifier != null && !siteQualifier.startsWith(ANT_PREFIX)) {
			this.siteQualifier = siteQualifier;
		}
	}

	public void setCategory(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX)) {
			try {
				categoryXML = URIUtil.fromString(value);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Category description location (" + value + ") must be a URL."); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}

	public void setCategoryVersion(String version) {
		if (version != null && !version.startsWith(ANT_PREFIX)) {
			categoryVersion = version;
		}
	}

	public void setSite(String value) {
		if (value != null && value.length() > 0 && !value.startsWith(ANT_PREFIX)) {
			try {
				siteXML = URIUtil.fromString(value);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Artifact repository location (" + value + ") must be a URL."); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}
}
