/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.compatibility.URLEncoder;

/**
 *
 *
 */
public class BuildTimeFeatureFactory /*extends BaseFeatureFactory */implements /*IFeatureFactory,*/ IPDEBuildConstants, IBuildPropertiesConstants {
	public final static String BUILDTIME_FEATURE_FACTORY_ID = PI_PDEBUILD + ".BuildTimeFeatureFactory"; //$NON-NLS-1$

	private static BuildTimeFeatureFactory factoryInstance = null;
	
	public BuildTimeFeatureFactory() {
		factoryInstance = this;
	}
	
	public static BuildTimeFeatureFactory getInstance() {
		if (factoryInstance == null)
			factoryInstance = new BuildTimeFeatureFactory();
		return factoryInstance;
	}
	
	public BuildTimeFeature createFeature(URL url, BuildTimeSite site) throws CoreException {
		BuildTimeFeature feature = null;

		if (url == null) {
			if (site != null)
				return createFeature(site);
			return null;
		}

		try {
			URL nonResolvedURL = new URL(url, BuildTimeFeature.FEATURE_XML);
			URL resolvedURL = URLEncoder.encode(nonResolvedURL);

			feature = parseBuildFeature(resolvedURL);

			String qualifier = AbstractScriptGenerator.readProperties(new Path(url.getFile()).removeLastSegments(1).toOSString(), PROPERTIES_FILE, IStatus.OK).getProperty(PROPERTY_QUALIFIER);
			String newVersion = QualifierReplacer.replaceQualifierInVersion(feature.getVersion(), feature.getId(), qualifier, site != null ? site.getFeatureVersions() : null);
			if (newVersion != null) {
				//a feature version ending in qualifier using context will be further modified based on its included plugins				
				if (feature.getVersion().endsWith(PROPERTY_QUALIFIER) && (qualifier == null || qualifier.equalsIgnoreCase(PROPERTY_CONTEXT))) {
					int idx = feature.getVersion().lastIndexOf("."); //$NON-NLS-1$
					feature.setContextQualifierLength(newVersion.length() - idx - 1);
				}
				feature.setVersion(newVersion);
			}

			feature.setSite(site);
			//feature.setFeatureContentProvider(contentProvider);
			feature.setURL(resolvedURL);
		} catch (CoreException e) {
			String message = NLS.bind(Messages.error_creatingFeature, url);
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_PARSE, message, e));
			throw e;
		} catch (Exception e) {
			String message = NLS.bind(Messages.exception_readingFile, url);
			Status status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e);
			BundleHelper.getDefault().getLog().log(status); //Logging here because the caller consumes CoreExceptions.
			throw new CoreException(status);
		}
		return feature;
	}

	public BuildTimeFeature parseBuildFeature(URL featureURL) {
		BuildTimeFeatureParser parser = new BuildTimeFeatureParser();
		return (BuildTimeFeature)parser.parse(featureURL);
	}

	/*
	 * Creates an empty feature on the site 
	 */
	private BuildTimeFeature createFeature(BuildTimeSite site) {
		BuildTimeSiteContentProvider contentProvider = new BuildTimeSiteContentProvider(null, null, null);
		BuildTimeFeature feature = createFeatureModel();
		feature.setSite(site);
		feature.setFeatureContentProvider(contentProvider);
		return feature;
	}

	public BuildTimeFeature createFeatureModel() {
		return new BuildTimeFeature();
	}

}
