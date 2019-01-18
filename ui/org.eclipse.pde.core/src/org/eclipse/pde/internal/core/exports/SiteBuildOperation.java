/*******************************************************************************
 *  Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
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

	private final IFeatureModel[] fFeatureModels;
	private final ISiteModel fSiteModel;
	private final IContainer fSiteContainer;

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

	@Override
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
		for (IFeatureModel featureModel : fFeatureModels) {
			IFeature feature = featureModel.getFeature();
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
		for (ISiteFeature siteFeature : siteFeatures) {
			if (siteFeature.getId().equals(feature.getId()) && siteFeature.getVersion().equals(feature.getVersion())) {
				return siteFeature;
			}
		}
		String highestQualifier = null;
		// then find feature with the highest qualifier
		for (ISiteFeature siteFeature : siteFeatures) {
			if (siteFeature.getId().equals(feature.getId())) {
				Version candidatePvi = Version.parseVersion(siteFeature.getVersion());
				if (pvi.getMajor() == candidatePvi.getMajor() && pvi.getMinor() == candidatePvi.getMinor() && pvi.getMicro() == candidatePvi.getMicro()) {
					if (reversionCandidate == null || candidatePvi.getQualifier().compareTo(highestQualifier) > 0) {
						reversionCandidate = siteFeature;
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
		for (IResource featureJar : featureJars) {
			File file = new File(featureJar.getLocation().toOSString());
			long jarTime = file.lastModified();
			String jarName = featureJar.getName();

			if (jarTime < fBuildTime) {
				continue;
			}
			if (jarTime <= newestTime) {
				continue;
			}
			if (pattern.matcher(jarName).matches()) {
				newestName = featureJar.getName();
				newestTime = jarTime;
			}
		}
		if (newestName == null) {
			return null;
		}

		return newestName.substring(id.length() + 1, newestName.length() - 4);
	}

	@Override
	protected void createPostProcessingFiles() {
		createPostProcessingFile(new File(fFeatureLocation, FEATURE_POST_PROCESSING));
		createPostProcessingFile(new File(fFeatureLocation, PLUGIN_POST_PROCESSING));
	}

	@Override
	protected boolean publishingP2Metadata() {
		return true;
	}

	@Override
	protected void setP2MetaDataProperties(Map<String, String> map) {
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
