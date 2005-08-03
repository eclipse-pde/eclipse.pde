/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;

public class BuildSiteJob extends FeatureExportJob {

	private IFeatureModel[] fFeaturemodels;

	private ISiteModel fSiteModel;

	private IContainer fSiteContainer;
	
	private long fBuildTime;

	private String fFeatureLocation;
	
	private static FeatureExportInfo getInfo(ISiteModel siteModel, IFeatureModel[] models) {
		FeatureExportInfo info = new FeatureExportInfo();
		info.useJarFormat = true;
		info.toDirectory = true;
		info.destinationDirectory = siteModel.getUnderlyingResource().getParent().getLocation().toOSString();
		info.items = models;
		return info;
	}

	public BuildSiteJob(Display display, IFeatureModel[] models,
			ISiteModel siteModel) {		
		super(getInfo(siteModel, models));
		fFeaturemodels = models;
		fSiteModel = siteModel;
		fSiteContainer = siteModel.getUnderlyingResource().getParent();
		setRule(MultiRule.combine(fSiteContainer.getProject(), getRule()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(final IProgressMonitor monitor) {
		fBuildTime = System.currentTimeMillis();
		IStatus status = super.run(monitor);
		try {
			fSiteContainer.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			updateSiteFeatureVersions();
		} catch (CoreException ce) {
			PDECore.logException(ce);
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#doExports(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void doExports(IProgressMonitor monitor)
			throws InvocationTargetException, CoreException {
		String[] config = { TargetPlatform.getOS(),
				TargetPlatform.getWS(), TargetPlatform.getOSArch(),
				TargetPlatform.getNL() };
		try {
			monitor.beginTask("", 10); //$NON-NLS-1$
			// create a feature to wrap all plug-ins and features
			String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
			fFeatureLocation = fBuildTempLocation + File.separator
					+ featureID;
			createFeature(featureID, fFeatureLocation, config, false);
			createBuildPropertiesFile(fFeatureLocation);
			if (fInfo.useJarFormat) {
				createPostProcessingFile(new File(fFeatureLocation,
						FEATURE_POST_PROCESSING));
				createPostProcessingFile(new File(fFeatureLocation,
						PLUGIN_POST_PROCESSING));
			}
			doExport(featureID, null, fFeatureLocation, config[0],
					config[1], config[2],
					new SubProgressMonitor(monitor, 7));
		} catch (IOException e) {
		} finally {
			for (int j = 0; j < fInfo.items.length; j++) {
				deleteBuildFiles(fInfo.items[j]);
			}
			cleanup(null, new SubProgressMonitor(monitor, 3));
			monitor.done();
		}	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getPaths()
	 */
	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + "feature.xml"; //$NON-NLS-1$
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();
		Properties prop = new Properties();
		prop.put("pde", "marker"); //$NON-NLS-1$ //$NON-NLS-2$
		save(new File(file, "build.properties"), prop, "Marker File"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void save(File file, Properties properties, String header) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, header);
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	private void updateSiteFeatureVersions() {
		try {
			for (int i = 0; i < fFeaturemodels.length; i++) {
				IFeature feature = fFeaturemodels[i].getFeature();
				PluginVersionIdentifier pvi = new PluginVersionIdentifier(
						feature.getVersion());

				if ("qualifier".equals(pvi.getQualifierComponent())) { //$NON-NLS-1$
					String newVersion = findBuiltVersion(feature.getId(), pvi
							.getMajorComponent(), pvi.getMinorComponent(), pvi
							.getServiceComponent());
					if (newVersion == null) {
						continue;
					}
					ISiteFeature reVersionCandidate = findSiteFeature(feature,
							pvi);
					if (reVersionCandidate != null) {
						reVersionCandidate.setVersion(newVersion);
						reVersionCandidate
								.setURL("features/" + feature.getId() + "_" //$NON-NLS-1$ //$NON-NLS-2$
										+ newVersion + ".jar"); //$NON-NLS-1$
					}
				}
			}
			((WorkspaceSiteModel)fSiteModel).save();
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		}
	}

	/**
	 * @param feature
	 * @param pvi
	 * @param siteFeatures
	 * @return
	 */
	private ISiteFeature findSiteFeature(IFeature feature,
			PluginVersionIdentifier pvi) {
		ISiteFeature reversionCandidate = null;
		// first see if version with qualifier being qualifier is present among
		// site features
		ISiteFeature[] siteFeatures = fSiteModel.getSite().getFeatures();
		for (int s = 0; s < siteFeatures.length; s++) {
			if (siteFeatures[s].getId().equals(feature.getId())
					&& siteFeatures[s].getVersion()
							.equals(feature.getVersion())) {
				return siteFeatures[s];
			}
		}
		String highestQualifier = null;
		// then find feature with the highest qualifier
		for (int s = 0; s < siteFeatures.length; s++) {
			if (siteFeatures[s].getId().equals(feature.getId())) {
				PluginVersionIdentifier candidatePvi = new PluginVersionIdentifier(
						siteFeatures[s].getVersion());
				if (pvi.getMajorComponent() == candidatePvi.getMajorComponent()
						&& pvi.getMinorComponent() == candidatePvi
								.getMinorComponent()
						&& pvi.getServiceComponent() == candidatePvi
								.getServiceComponent()) {
					if (reversionCandidate == null
							|| candidatePvi.getQualifierComponent().compareTo(
									highestQualifier) > 0) {
						reversionCandidate = siteFeatures[s];
						highestQualifier = candidatePvi.getQualifierComponent();
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
	 * @return
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getLogFoundMessage()
	 */
	protected String getLogFoundMessage() {
		return PDEUIMessages.BuildSiteJob_message; 
	}
}
