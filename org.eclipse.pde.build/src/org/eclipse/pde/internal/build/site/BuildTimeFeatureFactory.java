/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.internal.core.FeatureExecutableContentProvider;
import org.eclipse.update.internal.core.URLEncoder;

/**
 *
 *
 */
public class BuildTimeFeatureFactory extends BaseFeatureFactory implements IFeatureFactory, IPDEBuildConstants, IBuildPropertiesConstants {
	public final static String BUILDTIME_FEATURE_FACTORY_ID = PI_PDEBUILD + ".BuildTimeFeatureFactory"; //$NON-NLS-1$

	public IFeature createFeature(URL url, ISite site, IProgressMonitor p) throws CoreException {
		Feature feature = null;
		InputStream featureStream = null;

		if (url == null)
			return createFeature(site);

		try {
			//	TODO FeatureExecutableContentProvider is a non API class
			IFeatureContentProvider contentProvider = new FeatureExecutableContentProvider(url);

			URL nonResolvedURL = contentProvider.getFeatureManifestReference(null).asURL();
			URL resolvedURL = URLEncoder.encode(nonResolvedURL);

			featureStream = resolvedURL.openStream();
			feature = (Feature) this.parseFeature(featureStream);

			String qualifier = AbstractScriptGenerator.readProperties(new Path(url.getFile()).removeLastSegments(1).toOSString(), PROPERTIES_FILE, IStatus.OK).getProperty(PROPERTY_QUALIFIER);
			String newVersion = QualifierReplacer.replaceQualifierInVersion(feature.getFeatureVersion(), feature.getFeatureIdentifier(), qualifier, ((BuildTimeSite) site).getFeatureVersions());
			if (newVersion != null){
				//a feature version ending in .qualifier using context will be further modified based on its included plugins				
				if (feature.getFeatureVersion().endsWith('.' + PROPERTY_QUALIFIER) && (qualifier == null || qualifier.equalsIgnoreCase(PROPERTY_CONTEXT))) {
					int idx = feature.getFeatureVersion().lastIndexOf('.' + PROPERTY_QUALIFIER);
					((BuildTimeFeature)feature).setContextQualifierLength(newVersion.length() - idx - 1);
				}
				((BuildTimeFeature) feature).setFeatureVersion(newVersion);
			}

			feature.setSite(site);
			feature.setFeatureContentProvider(contentProvider);
			feature.resolve(url, url);
		} catch (CoreException e) {
			String message = NLS.bind(Messages.error_creatingFeature, url);
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_PARSE, message, e));
			throw e;
		} catch (Exception e) {
			String message = NLS.bind(Messages.exception_readingFile, url);
			Status status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e);
			BundleHelper.getDefault().getLog().log(status);	//Logging here because the caller consumes CoreExceptions.
			throw new CoreException(status);
		} finally {
			try {
				if (featureStream != null)
					featureStream.close();
			} catch (IOException e) {
				//ignore
			}
		}
		return feature;
	}

	/*
	 * Creates an empty feature on the site 
	 */
	private IFeature createFeature(ISite site) throws CoreException {
		Feature feature = null;
		//TODO FeatureExecutableContentProvider is a non API class
		IFeatureContentProvider contentProvider = new FeatureExecutableContentProvider(null);
		feature = (Feature) createFeatureModel();
		feature.setSite(site);
		feature.setFeatureContentProvider(contentProvider);
		return feature;
	}

	public FeatureModel createFeatureModel() {
		return new BuildTimeFeature();
	}
}
