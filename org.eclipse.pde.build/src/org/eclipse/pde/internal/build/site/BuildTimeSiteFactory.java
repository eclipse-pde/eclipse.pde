/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.core.model.SiteModel;

public class BuildTimeSiteFactory extends BaseSiteFactory implements ISiteFactory, IPDEBuildConstants {
	// The whole site : things to be compiled and the installedBase
	private Site site = null;

	// Indicate if the content of the site changed
	private boolean urlsChanged = false;

	// URLs from the the site will be built
	private String[] sitePaths;

	//	adress of the site used as a base
	private static String installedBaseLocation = null;

	/** 
	 * Create a build time site, using the sitePaths, and the installedBaseLocation.
	 * Note that the site object is not recomputed if no change has been done.
	 * 
	 * @return ISite
	 * @throws CoreException
	 */
	public ISite createSite() throws CoreException {
		if (site != null && urlsChanged == false)
			return site;

		urlsChanged = false;
		site = (Site) createSiteMapModel();

		// Here we find the features in the URLs
		Collection featureXMLs = findFeatureXMLs();

		// If an installed base is provided we need to look at it
		String installedBaseURL = null;
		if (installedBaseLocation != null && !installedBaseLocation.equals("")) { //$NON-NLS-1$
			if (!new File(installedBaseLocation).exists()) {
				String message = Policy.bind("error.incorrectDirectoryEntry", installedBaseLocation); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READ_DIRECTORY, message, null));
			}

			installedBaseURL = installedBaseLocation; //$NON-NLS-1$
			Collection installedFeatures = Utils.findFiles(installedBaseLocation, DEFAULT_FEATURE_LOCATION, DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (installedFeatures != null)
				featureXMLs.addAll(installedFeatures);

			//Search the features in the links
			String[] linkPaths = PluginPathFinder.getPluginPaths(installedBaseURL);
			for (int i = 0; i < linkPaths.length; i++) {
				Collection foundFeatures = Utils.findFiles(linkPaths[i], DEFAULT_FEATURE_LOCATION, DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
				if (foundFeatures != null)
					featureXMLs.addAll(foundFeatures);
			}
		}

		URL featureURL;
		SiteFeatureReferenceModel featureRef;

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
					featureRef.setType(BuildTimeFeatureFactory.BUILDTIME_FEATURE_FACTORY_ID);
					site.addFeatureReferenceModel(featureRef);
				} catch (MalformedURLException e) {
					BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_MISSING_SOURCE, Policy.bind("warning.cannotLocateSource", featureXML.getAbsolutePath()), e)); //$NON-NLS-1$
				}
			}
		}
		ISiteContentProvider contentProvider = new BuildTimeSiteContentProvider(sitePaths, installedBaseURL);
		site.setSiteContentProvider(contentProvider);
		contentProvider.setSite(site);
		return site;
	}

	/** 
	 * This method MUST not be called. The given URL is a pointer to the location
	 * of a site.xml file which describes our site, and we don't have this file.
	 */
	public ISite createSite(URL url) throws CoreException, InvalidSiteTypeException {
		String message = Policy.bind("error.incorrectDirectoryEntry"); //$NON-NLS-1$
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READ_DIRECTORY, message, null));
	}

	public SiteModel createSiteMapModel() {
		return new BuildTimeSite();
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
		for (int i = 0; i < sitePaths.length; i++) {
			Collection foundFeatures = Utils.findFiles(sitePaths[i], DEFAULT_FEATURE_LOCATION, DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (foundFeatures != null)
				features.addAll(foundFeatures);
		}
		return features;
	}
}