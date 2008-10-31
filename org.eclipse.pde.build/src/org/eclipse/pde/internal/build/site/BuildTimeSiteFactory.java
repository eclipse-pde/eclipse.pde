/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.compatibility.FeatureReference;

public class BuildTimeSiteFactory /*extends BaseSiteFactory*/implements IPDEBuildConstants {
	// The whole site : things to be compiled and the installedBase
	private BuildTimeSite site = null;

	// Indicate if the content of the site changed
	private boolean urlsChanged = false;

	// URLs from the the site will be built
	private String[] sitePaths;
	private String[] eeSources;

	//	address of the site used as a base
	private static String installedBaseLocation = null;

	private boolean reportResolutionErrors;

	private PDEUIStateWrapper pdeUIState;

	//Support for filtering the state
	private List rootFeaturesForFilter;
	private List rootPluginsForFilter;
	private boolean filterState;
	private boolean filterP2Base = false;

	/** 
	 * Create a build time site, using the sitePaths, and the installedBaseLocation.
	 * Note that the site object is not recomputed if no change has been done.
	 * 
	 * @return ISite
	 * @throws CoreException
	 */
	public BuildTimeSite createSite() throws CoreException {
		if (site != null && urlsChanged == false)
			return site;

		urlsChanged = false;
		site = createSiteMapModel();

		// Here we find the features in the URLs
		Collection featureXMLs = findFeatureXMLs();

		// If an installed base is provided we need to look at it
		String installedBaseURL = null;
		if (installedBaseLocation != null && !installedBaseLocation.equals("")) { //$NON-NLS-1$
			if (!new File(installedBaseLocation).exists()) {
				String message = NLS.bind(Messages.error_incorrectDirectoryEntry, installedBaseLocation);
				installedBaseLocation = null;
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READ_DIRECTORY, message, null));
			}

			installedBaseURL = installedBaseLocation;
			Collection installedFeatures = Utils.findFiles(new File(installedBaseLocation), DEFAULT_FEATURE_LOCATION, Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (installedFeatures != null)
				featureXMLs.addAll(installedFeatures);

			// extract features from platform.xml
			File[] featureDirectories = PluginPathFinder.getFeaturePaths(installedBaseURL);
			for (int i = 0; i < featureDirectories.length; i++) {
				File featureXML = new File(featureDirectories[i], Constants.FEATURE_FILENAME_DESCRIPTOR);
				if (featureXML.exists())
					featureXMLs.add(featureXML);
			}

		}

		URL featureURL;
		FeatureReference featureRef;

		for (Iterator iter = featureXMLs.iterator(); iter.hasNext();) {
			File featureXML = (File) iter.next();
			if (featureXML.exists()) {
				// Here we could not use toURL() on currentFeatureDir, because the URL has a slash after the colons (file:/c:/foo) whereas the plugins don't
				// have it (file:d:/eclipse/plugins) and this causes problems later to compare URLs... and compute relative paths
				try {
					featureURL = new URL("file:" + featureXML.getAbsolutePath()); //$NON-NLS-1$
					featureRef = createFeatureReferenceModel();
					featureRef.setSiteModel(site);
					featureRef.setURLString(featureURL.toExternalForm());
					//featureRef.setType(BuildTimeFeatureFactory.BUILDTIME_FEATURE_FACTORY_ID);
					site.addFeatureReferenceModel(featureRef);
				} catch (MalformedURLException e) {
					BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_MISSING_SOURCE, NLS.bind(Messages.warning_cannotLocateSource, featureXML.getAbsolutePath()), e));
				}
			}
		}
		BuildTimeSiteContentProvider contentProvider = new BuildTimeSiteContentProvider(sitePaths, installedBaseURL, pdeUIState);
		contentProvider.setFilterP2Base(filterP2Base);
		site.setSiteContentProvider(contentProvider);
		contentProvider.setSite(site);
		return site;
	}

	public BuildTimeSite createSiteMapModel() {
		BuildTimeSite model = new BuildTimeSite();
		model.setReportResolutionErrors(reportResolutionErrors);
		model.setFilter(filterState);
		model.setRootFeaturesForFilter(rootFeaturesForFilter);
		model.setRootPluginsForFiler(rootPluginsForFilter);
		model.setEESources(eeSources);
		return model;
	}

	public static void setInstalledBaseSite(String installedBaseSite) {
		BuildTimeSiteFactory.installedBaseLocation = installedBaseSite;
	}

	public void setSitePaths(String[] urls) {
		if (sitePaths == null) {
			sitePaths = urls;
			urlsChanged = true;
			return;
		}

		//Check if urls are not the same than sitePaths.  
		int i = 0;
		boolean found = true;
		while (found && i < sitePaths.length) {
			found = false;
			for (int j = 0; j < urls.length; j++) {
				if (sitePaths[i].equals(urls[j])) {
					found = true;
					break;
				}
			}
			i++;
		}
		if (!found) {
			sitePaths = urls;
			urlsChanged = true;
		}
	}

	/**
	 * Look for the feature.xml files and return a collection of java.io.File objects
	 * which point to their locations. Only look in directories which are direct descendants
	 * of the /features directory. (do not do an infinite depth look-up)
	 */
	private Collection findFeatureXMLs() {
		Collection features = new ArrayList();
		Collection foundFeatures = null;
		for (int i = 0; i < sitePaths.length; i++) {
			File file = new File(sitePaths[i], Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (file.exists()) {
				//path is a feature itself
				features.add(file);
				continue;
			} else if (new File(sitePaths[i], DEFAULT_FEATURE_LOCATION).exists()) {
				//path is a eclipse root and contains a features subdirectory
				foundFeatures = Utils.findFiles(new File(sitePaths[i]), DEFAULT_FEATURE_LOCATION, Constants.FEATURE_FILENAME_DESCRIPTOR);
			} else {
				// treat as a flat directory containing features
				foundFeatures = Utils.findFiles(new File(sitePaths[i]), ".", Constants.FEATURE_FILENAME_DESCRIPTOR); //$NON-NLS-1$
			}
			if (foundFeatures != null)
				features.addAll(foundFeatures);
		}
		return features;
	}

	public void setReportResolutionErrors(boolean value) {
		reportResolutionErrors = value;
	}

	public void setInitialState(PDEUIStateWrapper uiState) {
		this.pdeUIState = uiState;
	}

	public void setFilterState(boolean b) {
		this.filterState = b;
	}

	public void setFilterRoots(List featuresForFilterRoots, List pluginsForFilterRoots) {
		this.rootFeaturesForFilter = featuresForFilterRoots;
		this.rootPluginsForFilter = pluginsForFilterRoots;
	}

	public FeatureReference createFeatureReferenceModel() {
		return new FeatureReference();
	}

	public void setFilterP2Base(boolean filterP2Base) {
		this.filterP2Base = filterP2Base;
	}

	public void setEESources(String[] sources) {
		this.eeSources = sources;
	}
}
