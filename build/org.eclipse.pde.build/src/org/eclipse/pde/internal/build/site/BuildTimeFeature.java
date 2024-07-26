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
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.compatibility.SiteManager;

public class BuildTimeFeature extends Feature {
	/**
	 * Simple file name of the default feature manifest file
	 * @since 3.4.0
	 */
	public static final String FEATURE_FILE = "feature"; //$NON-NLS-1$

	/**
	 * File extension of the default feature manifest file
	 * @since 3.4.0
	 */
	public static final String FEATURE_XML = FEATURE_FILE + ".xml"; //$NON-NLS-1$

	public BuildTimeFeature(String id, String version) {
		super(id, version);
	}

	public BuildTimeFeature() {
		super("", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Boolean binary = null;
	private int contextQualifierLength = -1;
	private BuildTimeSiteContentProvider contentProvider = null;
	private BuildTimeSite site = null;
	private Path path = null;
	private String rootLocation = null;

	public FeatureEntry[] getRawIncludedFeatureReferences() {
		ArrayList<FeatureEntry> included = new ArrayList<>();
		FeatureEntry[] entries = getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isRequires() || entry.isPlugin())
				continue;
			included.add(entry);
		}
		return included.toArray(new FeatureEntry[included.size()]);
	}

	public FeatureEntry[] getIncludedFeatureReferences() {
		ArrayList<FeatureEntry> included = new ArrayList<>();
		FeatureEntry[] entries = getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isRequires() || entry.isPlugin())
				continue;

			if (SiteManager.isValidEnvironment(entry)) {
				included.add(entry);
			}
		}

		return included.toArray(new FeatureEntry[included.size()]);
	}

	public FeatureEntry[] getPluginEntries() {
		ArrayList<FeatureEntry> plugins = new ArrayList<>();
		FeatureEntry[] entries = getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isRequires() || !entry.isPlugin())
				continue;
			if (SiteManager.isValidEnvironment(entry)) {
				plugins.add(entry);
			}
		}
		return plugins.toArray(new FeatureEntry[plugins.size()]);
	}

	public FeatureEntry[] getRawPluginEntries() {
		ArrayList<FeatureEntry> plugins = new ArrayList<>();
		FeatureEntry[] entries = getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isRequires() || !entry.isPlugin())
				continue;
			plugins.add(entry);
		}
		return plugins.toArray(new FeatureEntry[plugins.size()]);
	}

	public FeatureEntry[] getImports() {
		ArrayList<FeatureEntry> imports = new ArrayList<>();
		FeatureEntry[] entries = getEntries();
		for (FeatureEntry entry : entries) {
			if (!entry.isRequires())
				continue;
			imports.add(entry);
		}
		return imports.toArray(new FeatureEntry[imports.size()]);
	}

	public boolean isBinary() {
		if (binary == null) {
			String root = getRootLocation();
			File properties = new File(root, IPDEBuildConstants.PROPERTIES_FILE);
			if (!properties.exists())
				binary = Boolean.TRUE;
			else
				binary = Boolean.FALSE;
		}
		return binary.booleanValue();
	}

	public void setBinary(boolean isCompiled) {
		this.binary = isCompiled ? Boolean.TRUE : Boolean.FALSE;
	}

	public void setContextQualifierLength(int l) {
		contextQualifierLength = l;
	}

	public int getContextQualifierLength() {
		return contextQualifierLength;
	}

	public void setSite(BuildTimeSite site) {
		this.site = site;
	}

	public BuildTimeSite getSite() {
		return site;
	}

	public void setFeatureContentProvider(BuildTimeSiteContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public BuildTimeSiteContentProvider getFeatureContentProvider() {
		return contentProvider;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	/**
	 * @return the local filesystem location of the directory containing the feature.xml file. 
	 */
	public String getRootLocation() {
		if (rootLocation == null) {
			Path location = getPath();
			if (location == null) {
				return null;
			}
			location = location.toAbsolutePath();
			if (location.endsWith(Constants.FEATURE_FILENAME_DESCRIPTOR)) {
				location = location.getParent();
			}
			rootLocation = location.toString() + File.separator;
		}
		return rootLocation;
	}

	public FeatureEntry findPluginEntry(String id, String version) {
		FeatureEntry[] entries = getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isPlugin() && entry.getId().equals(id))
				if (Utils.matchVersions(version, entry.getVersion()))
					return entry;
		}
		return null;
	}

	private static final Path FEATURE_XML_PATH = Path.of(FEATURE_XML);

	static Path ensureEndsWithFeatureXml(Path path) {
		return path != null && !path.endsWith(BuildTimeFeature.FEATURE_XML_PATH) //
				? path.resolve(BuildTimeFeature.FEATURE_XML_PATH)
				: path;
	}
}
