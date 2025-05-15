/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Messages;
import org.eclipse.pde.internal.build.PDEUIStateWrapper;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.site.compatibility.FeatureReference;

public class BuildTimeSiteFactory /*extends BaseSiteFactory*/ implements IPDEBuildConstants {
	// The whole site : things to be compiled and the installedBase
	private BuildTimeSite site = null;

	// Indicate if the content of the site changed
	private boolean urlsChanged = false;

	// URLs from the the site will be built
	private List<File> sitePaths;
	private String[] eeSources;

	//	address of the site used as a base
	private static String installedBaseLocation = null;

	private boolean reportResolutionErrors;

	private PDEUIStateWrapper pdeUIState;

	//Support for filtering the state
	private List<String> rootFeaturesForFilter;
	private List<String> rootPluginsForFilter;
	private boolean filterState;
	private boolean filterP2Base = false;

	/**
	 * Create a build time site, using the sitePaths, and the installedBaseLocation.
	 * Note that the site object is not recomputed if no change has been done.
	 *
	 * @return ISite
	 */
	public BuildTimeSite createSite() throws CoreException {
		if (site != null && urlsChanged == false) {
			return site;
		}

		urlsChanged = false;
		site = createSiteMapModel();

		// Here we find the features in the URLs
		Collection<File> featureXMLs = findFeatureXMLs();

		// If an installed base is provided we need to look at it
		String installedBaseURL = null;
		if (installedBaseLocation != null && !installedBaseLocation.equals("")) { //$NON-NLS-1$
			if (!new File(installedBaseLocation).exists()) {
				String message = NLS.bind(Messages.error_incorrectDirectoryEntry, installedBaseLocation);
				installedBaseLocation = null;
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READ_DIRECTORY, message, null));
			}

			installedBaseURL = installedBaseLocation;
			Collection<File> installedFeatures = Utils.findFiles(new File(installedBaseLocation), DEFAULT_FEATURE_LOCATION, Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (installedFeatures != null) {
				featureXMLs.addAll(installedFeatures);
			}

			// extract features from platform.xml
			List<File> featureDirectories = PluginPathFinder.getFeaturePaths(installedBaseURL);
			for (File element : featureDirectories) {
				File featureXML = new File(element, Constants.FEATURE_FILENAME_DESCRIPTOR);
				if (featureXML.exists()) {
					featureXMLs.add(featureXML);
				}
			}

		}

		for (File featureXML : featureXMLs) {
			if (featureXML.exists()) {
				FeatureReference featureRef = createFeatureReferenceModel();
				featureRef.setSiteModel(site);
				featureRef.setPath(featureXML.toPath().toAbsolutePath());
				//featureRef.setType(BuildTimeFeatureFactory.BUILDTIME_FEATURE_FACTORY_ID);
				site.addFeatureReferenceModel(featureRef);
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

	public void setSitePaths(List<File> paths) {
		if (sitePaths == null) {
			sitePaths = paths;
			urlsChanged = true;
			return;
		}

		//Check if urls are not the same than sitePaths.
		if (!new HashSet<>(this.sitePaths).equals(new HashSet<>(paths))) {
			sitePaths = paths;
			urlsChanged = true;
		}
	}

	/**
	 * Look for the feature.xml files and return a collection of java.io.File objects
	 * which point to their locations. Only look in directories which are direct descendants
	 * of the /features directory. (do not do an infinite depth look-up)
	 */
	private Collection<File> findFeatureXMLs() {
		Collection<File> features = new ArrayList<>();
		Collection<File> foundFeatures = null;
		for (File sitePath : sitePaths) {
			File file = new File(sitePath, Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (file.exists()) {
				//path is a feature itself
				features.add(file);
				continue;
			} else if (new File(sitePath, DEFAULT_FEATURE_LOCATION).exists()) {
				//path is a eclipse root and contains a features subdirectory
				foundFeatures = Utils.findFiles(sitePath, DEFAULT_FEATURE_LOCATION, Constants.FEATURE_FILENAME_DESCRIPTOR);
			} else {
				// treat as a flat directory containing features
				foundFeatures = Utils.findFiles(sitePath, ".", Constants.FEATURE_FILENAME_DESCRIPTOR); //$NON-NLS-1$
			}
			if (foundFeatures != null) {
				features.addAll(foundFeatures);
			}
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

	public void setFilterRoots(List<String> featuresForFilterRoots, List<String> pluginsForFilterRoots) {
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
