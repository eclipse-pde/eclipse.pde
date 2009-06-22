/*******************************************************************************
 *  Copyright (c) 2006, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.regex.Pattern;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.osgi.framework.Version;

/**
 * Performs a site build operation that will build any features needed by the site and generate
 * p2 metadata for those features.
 * 
 * @see FeatureBasedExportOperation
 * @see FeatureExportOperation
 */
public class SiteBuildOperation extends FeatureBasedExportOperation {

	private long fBuildTime;

	private IFeatureModel[] fFeatureModels;
	private ISiteModel fSiteModel;
	private IContainer fSiteContainer;

	public SiteBuildOperation(IFeatureModel[] features, ISiteModel site, String jobName) {
		super(getInfo(features, site), jobName);
		fFeatureModels = features;
		fSiteModel = site;
		fSiteContainer = site.getUnderlyingResource().getParent();
		setRule(MultiRule.combine(fSiteContainer.getProject(), getRule()));
	}

	private static FeatureExportInfo getInfo(IFeatureModel[] models, ISiteModel siteModel) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.useJarFormat = true;
		info.toDirectory = true;
		info.allowBinaryCycles = true;
		info.destinationDirectory = siteModel.getUnderlyingResource().getParent().getLocation().toOSString();
		info.items = models;
		return info;
	}

	protected IStatus run(IProgressMonitor monitor) {
		fBuildTime = System.currentTimeMillis();
		IStatus status = super.run(monitor);
		try {
			fSiteContainer.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			updateSiteFeatureVersions();
		} catch (CoreException ce) {
			return ce.getStatus();
		}
		return status;
	}

	private void updateSiteFeatureVersions() throws CoreException {
		for (int i = 0; i < fFeatureModels.length; i++) {
			IFeature feature = fFeatureModels[i].getFeature();
			Version pvi = Version.parseVersion(feature.getVersion());

			if ("qualifier".equals(pvi.getQualifier())) { //$NON-NLS-1$
				String newVersion = findBuiltVersion(feature.getId(), pvi.getMajor(), pvi.getMinor(), pvi.getMicro());
				if (newVersion == null) {
					continue;
				}
				ISiteFeature reVersionCandidate = findSiteFeature(feature, pvi);
				if (reVersionCandidate != null) {
					reVersionCandidate.setVersion(newVersion);
					reVersionCandidate.setURL("features/" + feature.getId() + "_" //$NON-NLS-1$ //$NON-NLS-2$
							+ newVersion + ".jar"); //$NON-NLS-1$
				}
			}
		}
		((WorkspaceSiteModel) fSiteModel).save();
	}

	private ISiteFeature findSiteFeature(IFeature feature, Version pvi) {
		ISiteFeature reversionCandidate = null;
		// first see if version with qualifier being qualifier is present among
		// site features
		ISiteFeature[] siteFeatures = fSiteModel.getSite().getFeatures();
		for (int s = 0; s < siteFeatures.length; s++) {
			if (siteFeatures[s].getId().equals(feature.getId()) && siteFeatures[s].getVersion().equals(feature.getVersion())) {
				return siteFeatures[s];
			}
		}
		String highestQualifier = null;
		// then find feature with the highest qualifier
		for (int s = 0; s < siteFeatures.length; s++) {
			if (siteFeatures[s].getId().equals(feature.getId())) {
				Version candidatePvi = Version.parseVersion(siteFeatures[s].getVersion());
				if (pvi.getMajor() == candidatePvi.getMajor() && pvi.getMinor() == candidatePvi.getMinor() && pvi.getMicro() == candidatePvi.getMicro()) {
					if (reversionCandidate == null || candidatePvi.getQualifier().compareTo(highestQualifier) > 0) {
						reversionCandidate = siteFeatures[s];
						highestQualifier = candidatePvi.getQualifier();
					}
				}
			}
		}
		return reversionCandidate;
	}

	/**
	 * Finds the highest version from feature jars. ID and version components
	 * are constant. Qualifier varies
	 * 
	 * @param builtJars
	 *            candidate jars in format id_version.jar
	 * @param id
	 * @param major
	 * @param minor
	 * @param service
	 */
	private String findBuiltVersion(String id, int major, int minor, int service) {
		IFolder featuresFolder = fSiteContainer.getFolder(new Path("features")); //$NON-NLS-1$
		if (!featuresFolder.exists()) {
			return null;
		}
		IResource[] featureJars = null;
		try {
			featureJars = featuresFolder.members();
		} catch (CoreException ce) {
			return null;
		}
		Pattern pattern = PatternConstructor.createPattern(id + "_" //$NON-NLS-1$
				+ major + "." //$NON-NLS-1$
				+ minor + "." //$NON-NLS-1$
				+ service + "*.jar", true); //$NON-NLS-1$ 
		// finding the newest feature archive
		String newestName = null;
		long newestTime = 0;
		for (int i = 0; i < featureJars.length; i++) {
			File file = new File(featureJars[i].getLocation().toOSString());
			long jarTime = file.lastModified();
			String jarName = featureJars[i].getName();

			if (jarTime < fBuildTime) {
				continue;
			}
			if (jarTime <= newestTime) {
				continue;
			}
			if (pattern.matcher(jarName).matches()) {
				newestName = featureJars[i].getName();
				newestTime = jarTime;
			}
		}
		if (newestName == null) {
			return null;
		}

		return newestName.substring(id.length() + 1, newestName.length() - 4);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureBasedExportOperation#createPostProcessingFiles()
	 */
	protected void createPostProcessingFiles() {
		createPostProcessingFile(new File(fFeatureLocation, FEATURE_POST_PROCESSING));
		createPostProcessingFile(new File(fFeatureLocation, PLUGIN_POST_PROCESSING));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#publishingP2Metadata()
	 */
	protected boolean publishingP2Metadata() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#setP2MetaDataProperties(java.util.Map)
	 */
	protected void setP2MetaDataProperties(Map map) {
		if (fInfo.toDirectory) {
			map.put(IXMLConstants.TARGET_P2_METADATA, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FLAVOR, P2Utils.P2_FLAVOR_DEFAULT);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_PUBLISH_ARTIFACTS, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FINAL_MODE_OVERRIDE, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_COMPRESS, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_GATHERING, Boolean.toString(publishingP2Metadata()));
			IResource siteXML = fSiteModel.getUnderlyingResource();
			if (siteXML.exists() && siteXML.getLocationURI() != null) {
				map.put(IBuildPropertiesConstants.PROPERTY_P2_CATEGORY_SITE, URIUtil.toUnencodedString(siteXML.getLocationURI()));
			}

			ISiteDescription description = fSiteModel.getSite().getDescription();
			String name = description != null && description.getName() != null && description.getName().length() > 0 ? description.getName() : PDECoreMessages.SiteBuildOperation_0;
			map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO_NAME, name);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO_NAME, name);

			try {
				String destination = new File(fBuildTempMetadataLocation).toURL().toString();
				map.put(IBuildPropertiesConstants.PROPERTY_P2_BUILD_REPO, destination);
			} catch (MalformedURLException e) {
				PDECore.log(e);
			}
		}
	}

}
