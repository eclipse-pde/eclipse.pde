/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.PDEUIStateWrapper;

public class BuildTimeSiteContentProvider implements IPDEBuildConstants {
	private final String installedBaseURL;
	private final List<File> files;
	private final PDEUIStateWrapper pdeUIState;
	private BuildTimeSite site;
	private boolean filterP2Base = false;

	public BuildTimeSiteContentProvider(List<File> urls, String installedBaseURL, PDEUIStateWrapper initialState) {
		//super(null);
		this.installedBaseURL = installedBaseURL;
		this.files = urls;
		this.pdeUIState = initialState;
	}

	/**
	 * Returns the URL where an eclipse install can be provided. Can be null. 
	 * @return URL
	 */
	public String getInstalledBaseURL() {
		return installedBaseURL;
	}

	public Collection<File> getPluginPaths() {
		Collection<File> pluginsToCompile = findPluginXML(files);
		if (installedBaseURL != null) {
			pluginsToCompile.addAll(Arrays.asList(PluginPathFinder.getPluginPaths(installedBaseURL, filterP2Base)));
		}
		return pluginsToCompile;
	}

	public URL getURL() {
		throw new RuntimeException();
	}

	//For every entry, return all the children of this entry is it is named plugins, otherwise return the entry itself  
	private Collection<File> findPluginXML(List<File> location) {
		Collection<File> collectedElements = new ArrayList<>(10);
		for (File element : location) {
			File f = new File(element, DEFAULT_PLUGIN_LOCATION);
			if (f.exists()) {
				//location was the root of an eclipse install, list everything from the plugins directory
				collectedElements.addAll(Arrays.asList(f.listFiles()));
			} else if (new File(element, JarFile.MANIFEST_NAME).exists() || new File(element, Constants.PLUGIN_FILENAME_DESCRIPTOR).exists() || new File(element, Constants.FRAGMENT_FILENAME_DESCRIPTOR).exists()) {
				collectedElements.add(element);
			} else if (element.isDirectory()) {
				//at this point Manifest, plugin.xml, feature.xml and fragment.xml don't exist here
				//consider a project with "flexible root"
				if (new File(element, PDE_CORE_PREFS).exists()) {
					try {
						Properties properties = AbstractScriptGenerator.readProperties(element.getAbsolutePath(), PDE_CORE_PREFS, IStatus.OK);
						String root = properties.getProperty(BUNDLE_ROOT_PATH);
						if (root != null) {
							File actualRoot = new File(element, root);
							if (actualRoot.exists())
								collectedElements.add(actualRoot);
						}
					} catch (CoreException e) {
						// nope
					}
				} else {
					//a "workspace"
					collectedElements.addAll(Arrays.asList(element.listFiles()));
				}
			} else if (element.isFile() && element.getName().endsWith(".jar")) {//$NON-NLS-1$
				collectedElements.add(element);
			}
		}
		return collectedElements;
	}

	public File getBaseProfile() {
		if (installedBaseURL == null)
			return null;

		File configurationFolder = new File(installedBaseURL, "configuration"); //$NON-NLS-1$
		if (configurationFolder.exists()) {
			try {
				Properties config = AbstractScriptGenerator.readProperties(configurationFolder.getAbsolutePath(), "config.ini", IStatus.OK); //$NON-NLS-1$
				String dataArea = config.getProperty("eclipse.p2.data.area"); //$NON-NLS-1$
				String profileName = config.getProperty("eclipse.p2.profile"); //$NON-NLS-1$
				if (dataArea != null && profileName != null) {
					int idx = dataArea.indexOf("@config.dir"); //$NON-NLS-1$
					if (idx != -1)
						dataArea = dataArea.substring(0, idx) + configurationFolder.getAbsolutePath() + dataArea.substring(idx + 11);

					File profileArea = new File(dataArea, "org.eclipse.equinox.p2.engine/profileRegistry/" + profileName + ".profile"); //$NON-NLS-1$ //$NON-NLS-2$
					if (profileArea.exists())
						return profileArea;
				}
			} catch (CoreException e) {
				//won't happend
			}
		}

		return null;
	}

	public PDEUIStateWrapper getInitialState() {
		return pdeUIState;
	}

	public URL getArchiveReference(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public BuildTimeSite getSite() {
		// TODO Auto-generated method stub
		return site;
	}

	public void setSite(BuildTimeSite site) {
		this.site = site;
	}

	public void setFilterP2Base(boolean filter) {
		this.filterP2Base = filter;
	}
}
