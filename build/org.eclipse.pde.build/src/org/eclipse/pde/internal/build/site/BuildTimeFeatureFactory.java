/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Messages;
import org.xml.sax.SAXException;

public class BuildTimeFeatureFactory /*extends BaseFeatureFactory */ implements /*IFeatureFactory,*/IPDEBuildConstants, IBuildPropertiesConstants {
	public final static String BUILDTIME_FEATURE_FACTORY_ID = PI_PDEBUILD + ".BuildTimeFeatureFactory"; //$NON-NLS-1$

	private static BuildTimeFeatureFactory factoryInstance = null;

	public BuildTimeFeatureFactory() {
		factoryInstance = this;
	}

	public static BuildTimeFeatureFactory getInstance() {
		if (factoryInstance == null) {
			factoryInstance = new BuildTimeFeatureFactory();
		}
		return factoryInstance;
	}

	public BuildTimeFeature createFeature(Path featurePath, BuildTimeSite site) throws CoreException {
		if (featurePath == null) {
			if (site != null) {
				return createFeature(site);
			}
			return null;
		}
		try {
			featurePath = BuildTimeFeature.ensureEndsWithFeatureXml(featurePath);
			BuildTimeFeature feature = parseBuildFeature(featurePath);

			String qualifier = AbstractScriptGenerator.readProperties(featurePath.getParent().toString(), PROPERTIES_FILE, IStatus.OK).getProperty(PROPERTY_QUALIFIER);
			String newVersion = QualifierReplacer.replaceQualifierInVersion(feature.getVersion(), feature.getId(), qualifier, site != null ? site.getFeatureVersions() : null);
			if (newVersion != null) {
				//a feature version ending in qualifier using context will be further modified based on its included plugins
				if (feature.getVersion().endsWith(PROPERTY_QUALIFIER) && (qualifier == null || !qualifier.equalsIgnoreCase(PROPERTY_NONE))) {
					int idx = feature.getVersion().lastIndexOf("."); //$NON-NLS-1$
					feature.setContextQualifierLength(newVersion.length() - idx - 1);
				}
				feature.setVersion(newVersion);
			}

			feature.setSite(site);
			feature.setPath(featurePath);
			return feature;
		} catch (CoreException e) {
			String message = NLS.bind(Messages.error_creatingFeature, featurePath);
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_PARSE, message, e));
			throw e;
		} catch (Exception e) {
			String message = NLS.bind(Messages.exception_readingFile, featurePath);
			Status status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e);
			BundleHelper.getDefault().getLog().log(status); //Logging here because the caller consumes CoreExceptions.
			throw new CoreException(status);
		}
	}

	public BuildTimeFeature parseBuildFeature(Path featurePath) throws CoreException {
		BuildTimeFeatureParser parser = new BuildTimeFeatureParser();
		BuildTimeFeature feature = null;
		try {
			feature = (BuildTimeFeature) parser.parse(featurePath);
			if (parser.getStatus() != null) {
				// some internalError were detected
				throw new CoreException(parser.getStatus());
			}
		} catch (SAXException e) {
			String message = NLS.bind(Messages.error_creatingFeature, featurePath);
			Status status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_PARSE, message, e);
			BundleHelper.getDefault().getLog().log(status);
			throw new CoreException(status);
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_readingFile, featurePath);
			Status status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e);
			BundleHelper.getDefault().getLog().log(status); //Logging here because the caller consumes CoreExceptions.
			throw new CoreException(status);
		}
		return feature;
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
