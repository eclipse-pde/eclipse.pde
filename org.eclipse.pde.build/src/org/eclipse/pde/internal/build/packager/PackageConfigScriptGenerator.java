/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;

public class PackageConfigScriptGenerator extends AssembleConfigScriptGenerator {

	private Properties packagingProperties;
	private Collection archiveRootProviders = Collections.EMPTY_LIST;

	public void initialize(String directoryName, String feature, Config configurationInformation, Collection elementList, Collection featureList, Collection allFeaturesList, Collection rootProviders) throws CoreException {
		/* package scripts require the root file providers for creating the file archive, but don't want them for other rootfile
		 * stuff done by the assembly scripts, so keep them separate here */
		super.initialize(directoryName, feature, configurationInformation, elementList, featureList, allFeaturesList, new ArrayList(0));
		archiveRootProviders = rootProviders != null ? rootProviders : Collections.EMPTY_LIST;
	}

	protected Collection getArchiveRootFileProviders() {
		if (archiveRootProviders.size() > 0)
			return archiveRootProviders;
		return super.getArchiveRootFileProviders();
	}

	private String getFinalName(BundleDescription bundle, String shape) {
		final String JAR = "jar"; //$NON-NLS-1$
		final String DOT_JAR = '.' + JAR;
		if (!AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER)) {
			Path path = new Path(bundle.getLocation());
			if (shape.equals(ShapeAdvisor.FILE) && !JAR.equalsIgnoreCase(path.getFileExtension()))
				return path.lastSegment().concat(DOT_JAR);
			return path.lastSegment();
		}
		if (shape.equals(ShapeAdvisor.FILE))
			return ModelBuildScriptGenerator.getNormalizedName(bundle) + DOT_JAR;
		return ModelBuildScriptGenerator.getNormalizedName(bundle);
	}

	private String getFinalName(BuildTimeFeature feature) {
		if (!AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER)) {
			Path featurePath = new Path(feature.getURL().getPath());
			return featurePath.segment(featurePath.segmentCount() - 2);
		}
		return feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
	}

	protected void generateGatherBinPartsTarget() { //TODO Here we should try to use cp because otherwise we will loose the permissions
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, null, null, null, null);
		String excludedFiles = null;
		if (AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER))
			excludedFiles = "build.properties, .project, .classpath"; //$NON-NLS-1$
		IPath baseLocation = null;
		try {
			String url = getSite(false).getSiteContentProvider().getInstalledBaseURL();
			if (url != null)
				baseLocation = new Path(url);
		} catch (CoreException e) {
			//nothing
		}

		ArrayList p2Features = BuildDirector.p2Gathering ? new ArrayList() : null;
		ArrayList p2Bundles = BuildDirector.p2Gathering ? new ArrayList() : null;
		for (int i = 0; i < plugins.length; i++) {
			Path pluginLocation = new Path(plugins[i].getLocation());
			String location = pluginLocation.toOSString();
			boolean isFolder = isFolder(pluginLocation);

			//try to relate the plugin location to the ${baseLocation} property
			if (baseLocation != null && baseLocation.isPrefixOf(pluginLocation)) {
				IPath relative = pluginLocation.removeFirstSegments(baseLocation.segmentCount());
				location = new Path(Utils.getPropertyFormat(PROPERTY_BASE_LOCATION)).append(relative).toOSString();
			}
			if (BuildDirector.p2Gathering) {
				p2Bundles.add(new FileSet(pluginLocation.removeLastSegments(1).toOSString(), null, pluginLocation.lastSegment(), null, null, null, null));
			} else if (isFolder) {
				script.printCopyTask(null, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) + '/' + getFinalName(plugins[i], ShapeAdvisor.FOLDER), new FileSet[] {new FileSet(location, null, null, null, excludedFiles, null, null)}, false, false);
			} else {
				script.printCopyFileTask(location, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) + '/' + getFinalName(plugins[i], ShapeAdvisor.FILE), false);
			}
		}

		for (int i = 0; i < features.length; i++) {
			IPath featureLocation = new Path(features[i].getRootLocation()); // Here we assume that all the features are local
			String location = featureLocation.toOSString();
			if (baseLocation != null && baseLocation.isPrefixOf(featureLocation)) {
				IPath relative = featureLocation.removeFirstSegments(baseLocation.segmentCount());
				location = new Path(Utils.getPropertyFormat(PROPERTY_BASE_LOCATION)).append(relative).toOSString();
			}

			if (BuildDirector.p2Gathering) {
				p2Features.add(new FileSet(featureLocation.removeLastSegments(1).toOSString(), null, featureLocation.lastSegment(), null, null, null, null));
			} else {
				script.printCopyTask(null, Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES) + '/' + getFinalName(features[i]), new FileSet[] {new FileSet(location, null, null, null, null, null, null)}, false, false);
			}
		}

		if (BuildDirector.p2Gathering) {
			String repo = "file:" + getWorkingDirectory() + "/buildRepo"; //$NON-NLS-1$ //$NON-NLS-2$
			script.printP2PublishFeaturesAndBundles(repo, repo, (FileSet[]) p2Bundles.toArray(new FileSet[p2Bundles.size()]), (FileSet[]) p2Features.toArray(new FileSet[p2Features.size()]), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_SITE), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_PREFIX), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_DEFINITION), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_VERSION), contextMetadata);
		}

		if (packagingProperties.size() != 0) {
			String filesToPackage = null;
			filesToPackage = packagingProperties.getProperty(ROOT, null);
			if (filesToPackage != null)
				filesToPackage += ',';

			String tmp = packagingProperties.getProperty(ROOT_PREFIX + configInfo.toString("."), null); //$NON-NLS-1$
			if (tmp != null)
				filesToPackage += tmp;

			if (filesToPackage == null)
				filesToPackage = "**/**"; //$NON-NLS-1$

			FileSet rootFiles = new FileSet(Utils.getPropertyFormat("tempDirectory") + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + "/eclipse", null, filesToPackage, null, null, null, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			String target = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER); //$NON-NLS-1$
			script.printCopyTask(null, target, new FileSet[] {rootFiles}, false, false);

			Utils.generatePermissions(packagingProperties, configInfo, PROPERTY_ECLIPSE_BASE, script);
		}
		script.printTargetEnd();
		script.println();
	}

	public String getTargetName() {
		String config = getTargetConfig();
		return "package" + '.' + getTargetElement() + (config.length() > 0 ? "." : "") + config; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private boolean isFolder(Path pluginLocation) {
		return pluginLocation.toFile().isDirectory();
	}

	public void setPackagingPropertiesLocation(String packagingPropertiesLocation) throws CoreException {
		packagingProperties = new Properties();
		if (packagingPropertiesLocation == null || packagingPropertiesLocation.equals("")) //$NON-NLS-1$
			return;

		InputStream propertyStream = null;
		try {
			propertyStream = new BufferedInputStream(new FileInputStream(packagingPropertiesLocation));
			try {
				packagingProperties.load(propertyStream);
			} finally {
				propertyStream.close();
			}
		} catch (FileNotFoundException e) {
			String message = NLS.bind(Messages.exception_readingFile, packagingPropertiesLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_readingFile, packagingPropertiesLocation);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}

		if (packagingProperties.size() > 0) {
			//This is need so that the call in assemble config script generator gather the root files 
			if (rootFileProviders == null)
				rootFileProviders = new ArrayList(1);
			rootFileProviders.add("elt"); //$NON-NLS-1$
		}
	}

	protected void generateGatherSourceTarget() {
		//In the packager, we do not gather source
		script.printTargetDeclaration(TARGET_GATHER_SOURCES, null, null, null, null);
		script.printTargetEnd();
		script.println();
	}

	protected FileSet[] generatePermissions(String root, boolean zip) {
		if (packagingProperties != null && packagingProperties.size() > 0) {
			//In the packager there is nothing to do since, the features we are packaging are pre-built and do not have a build.properties
			return new FileSet[0];
		}
		return super.generatePermissions(root, zip);
	}

	protected void generateGZipTarget(boolean assembling) {
		super.generateGZipTarget(false);
	}

	public void generateTarGZTasks(boolean assembling) {
		super.generateTarGZTasks(false);
	}

	protected void generateDirectorTarget(boolean assembling) {
		super.generateDirectorTarget(false);
	}

	protected void generateMirrorTask(boolean assembling) {
		super.generateMirrorTask(false);
	}

	protected void generateCleanupAssembly(boolean assembling) {
		super.generateCleanupAssembly(false);
	}

	protected void generateArchivingTarget(boolean assembling) {
		super.generateArchivingTarget(false);
	}

	protected Object[] getFinalShape(BundleDescription bundle) {
		if (AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE) == true) {
			String shape = isFolder(new Path(bundle.getLocation())) ? ShapeAdvisor.FOLDER : ShapeAdvisor.FILE;
			return new Object[] {getFinalName(bundle, shape), shape};
		}
		return shapeAdvisor.getFinalShape(bundle);
	}

	protected Object[] getFinalShape(BuildTimeFeature feature) {
		if (AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE) == true)
			return new Object[] {getFinalName(feature), ShapeAdvisor.FOLDER};
		return shapeAdvisor.getFinalShape(feature);
	}

	protected void printP2GenerationModeCondition() {
		// "final" if we are overriding, else "incremental"
		script.printConditionIsSet(PROPERTY_P2_GENERATION_MODE, "final", PROPERTY_P2_FINAL_MODE_OVERRIDE, "incremental"); //$NON-NLS-1$//$NON-NLS-2$
	}
}
