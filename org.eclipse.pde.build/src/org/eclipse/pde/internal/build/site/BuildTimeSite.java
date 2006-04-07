/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.*;
import org.osgi.framework.Version;

/**
 * This site represent a site at build time. A build time site is made of code
 * to compile, and a potential installation of eclipse (or derived products)
 * against which the code must be compiled. Moreover this site provide access to
 * a pluginRegistry.
 */
public class BuildTimeSite extends Site implements ISite, IPDEBuildConstants, IXMLConstants {
	private PDEState state;
	private Properties repositoryVersions; //version for the features
	private boolean reportResolutionErrors;
	
	public void setReportResolutionErrors(boolean value) {
		reportResolutionErrors = value;
	}
	
	public Properties getFeatureVersions() {
		if (repositoryVersions == null) {
			repositoryVersions = new Properties();
			try {
				InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_FEATURE_REPOTAG_FILENAME_DESCRIPTOR));
				try {
					repositoryVersions.load(input);
				} finally {
					input.close();
				}
			} catch (IOException e) {
				//Ignore
			}
		}
		return repositoryVersions;
	}

	public PDEState getRegistry() throws CoreException {
		if (state == null) {
			// create the registry according to the site where the code to
			// compile is, and a existing installation of eclipse
			BuildTimeSiteContentProvider contentProvider = (BuildTimeSiteContentProvider) getSiteContentProvider();

			if (contentProvider.getInitialState() != null) {
				state = new PDEState(contentProvider.getInitialState());
				return state;
			}
			
			if (AbstractScriptGenerator.isBuildingOSGi()) {
				state = new PDEState(); 
			} else {
				state = new PluginRegistryConverter();
			}
			state.addBundles(contentProvider.getPluginPaths());

			state.resolveState();
			BundleDescription[] allBundles = state.getState().getBundles();
			BundleDescription[] resolvedBundles = state.getState().getResolvedBundles();
			if (allBundles.length == resolvedBundles.length)
				return state;

			if (reportResolutionErrors) {
				MultiStatus errors = new MultiStatus(IPDEBuildConstants.PI_PDEBUILD, 1, Messages.exception_registryResolution, null);
				BundleDescription[] all = state.getState().getBundles();
				StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
				for (int i = 0; i < all.length; i++) {
					if (!all[i].isResolved()) {
						VersionConstraint[] unsatisfiedConstraints = helper.getUnsatisfiedConstraints(all[i]);
						for (int j = 0; j < unsatisfiedConstraints.length; j++) {
							String message = getResolutionFailureMessage(unsatisfiedConstraints[j]);
							errors.add(new Status(IStatus.WARNING, all[i].getSymbolicName(), IStatus.WARNING, message, null));
						}
					}
				}
				BundleHelper.getDefault().getLog().log(errors);
			}
		}
		if (!state.getState().isResolved())
			state.state.resolve(true);
		return state;
	}

	public String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification)
			return NLS.bind(Messages.unsatisfied_import, displayVersionConstraint(unsatisfied));
		if (unsatisfied instanceof BundleSpecification) {
			if (((BundleSpecification) unsatisfied).isOptional())
				return NLS.bind(Messages.unsatisfied_optionalBundle, displayVersionConstraint(unsatisfied));
			return NLS.bind(Messages.unsatisfied_required, displayVersionConstraint(unsatisfied));
		}
		return NLS.bind(Messages.unsatisfied_host, displayVersionConstraint(unsatisfied));
	}

	private String displayVersionConstraint(VersionConstraint constraint) {
		VersionRange versionSpec = constraint.getVersionRange();
		if (versionSpec == null)
			return constraint.getName();
		return constraint.getName() + '_' + versionSpec;
	}

	public IFeature findFeature(String featureId, String versionId, boolean throwsException) throws CoreException {
		ISiteFeatureReference[] features = getFeatureReferences();
		if (GENERIC_VERSION_NUMBER.equals(versionId))
			versionId = null;
		for (int i = 0; i < features.length; i++) {
			IFeature verifiedFeature;
			try {
				verifiedFeature = features[i].getFeature(null);
			} catch(CoreException e) {
				String message = NLS.bind(Messages.exception_featureParse, features[i].getURL());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}
			if (verifiedFeature.getVersionedIdentifier().getIdentifier().equals(featureId))
				if (versionId == null || features[i].getVersionedIdentifier().getVersion().equals(new PluginVersionIdentifier(versionId)))
					return features[i].getFeature(null);
		}
		int qualifierIdx = -1;
		if (versionId != null && (qualifierIdx = versionId.indexOf('.' + IBuildPropertiesConstants.PROPERTY_QUALIFIER))!= -1) {
			Version versionToMatch = Version.parseVersion(versionId.substring(0, qualifierIdx));
			for (int i = 0; i < features.length; i++) {
				Version featureVersion = Version.parseVersion(features[i].getVersionedIdentifier().getVersion().toString());
				if (features[i].getVersionedIdentifier().getIdentifier().equals(featureId) &&
						featureVersion.getMajor() == versionToMatch.getMajor() &&
						featureVersion.getMinor() == versionToMatch.getMinor() &&
						featureVersion.getMicro() >= versionToMatch.getMicro() &&
						featureVersion.getQualifier().compareTo(versionToMatch.getQualifier()) >= 0)
					return features[i].getFeature(null);
			}		
		}
		if (throwsException) {
			String message = NLS.bind(Messages.exception_missingFeature, featureId);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
		}
		return null;
	}

	public void addFeatureReferenceModel(File featureXML) {
		URL featureURL;
		SiteFeatureReferenceModel featureRef;
		if (featureXML.exists()) {
			// Here we could not use toURL() on currentFeatureDir, because the
			// URL has a slash after the colons (file:/c:/foo) whereas the
			// plugins don't
			// have it (file:d:/eclipse/plugins) and this causes problems later
			// to compare URLs... and compute relative paths
			try {
				featureURL = new URL("file:" + featureXML.getAbsolutePath() + '/'); //$NON-NLS-1$
				featureRef = new SiteFeatureReference();
				featureRef.setSiteModel(this);
				featureRef.setURLString(featureURL.toExternalForm());
				featureRef.setType(BuildTimeFeatureFactory.BUILDTIME_FEATURE_FACTORY_ID);
				addFeatureReferenceModel(featureRef);
			} catch (MalformedURLException e) {
				BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_MISSING_SOURCE, NLS.bind(Messages.warning_cannotLocateSource, featureXML.getAbsolutePath()), e));
			}
		}
	}
}
