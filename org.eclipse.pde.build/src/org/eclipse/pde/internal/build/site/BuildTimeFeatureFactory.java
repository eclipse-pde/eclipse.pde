/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.internal.core.*;

/**
 *
 *
 */
public class BuildTimeFeatureFactory extends BaseFeatureFactory implements IFeatureFactory, IPDEBuildConstants {
	public final static String BUILDTIME_FEATURE_FACTORY_ID = PI_PDEBUILD + ".BuildTimeFeatureFactory"; //$NON-NLS-1$

	public IFeature createFeature(URL url, ISite site, IProgressMonitor p) throws CoreException {
		Feature feature = null;
		InputStream featureStream = null;

		if (url == null)
			return createFeature(site);

		try {
			//	FIXME FeatureExecutableContentProvider is a non API class
			IFeatureContentProvider contentProvider = new FeatureExecutableContentProvider(url);

			URL nonResolvedURL = contentProvider.getFeatureManifestReference(null /*IProgressMonitor*/
			).asURL();
			URL resolvedURL = URLEncoder.encode(nonResolvedURL);

			featureStream = resolvedURL.openStream();
			feature = (Feature) this.parseFeature(featureStream);
			feature.setSite(site);
			feature.setFeatureContentProvider(contentProvider);
			feature.resolve(url, url);
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			String message = Policy.bind("exception.readingFile", url.toString()); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
		} finally {
			try {
				if (featureStream != null)
					featureStream.close();
			} catch (IOException e) {
			}
		}
		return feature;
	}

	/*
	 * Creates an empty feature on the site 
	 */
	private IFeature createFeature(ISite site) throws CoreException {
		Feature feature = null;
		//FIXME FeatureExecutableContentProvider is a non API class
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
