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
package org.eclipse.pde.internal.build.builder;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.IncludedFeatureReferenceModel;
import org.eclipse.update.core.model.URLEntryModel;

/**
 * Generates build.xml script for features.
 */
public class FeatureBuildScriptGenerator extends AbstractBuildScriptGenerator {
	// GENERATION FLAGS
	/**
	 * Indicates whether scripts for this feature included features should be
	 * generated.
	 */
	protected boolean analyseIncludedFeatures = false;
	/**
	 * Indicates whether scripts for this feature children' should be
	 * generated.
	 */
	protected boolean analysePlugins = true;
	/** Indicates whether a source feature should be generated for this feature */
	protected boolean sourceFeatureGeneration = false;
	/** Indicates whether the feature is binary */
	protected boolean binaryFeature = true;
	/** Indicates if the build scripts files should be produced or not */
	private boolean scriptGeneration = true;
	
	//FEATURE RELATED INFORMATION
	/** The identifier of the feature that the build script is being generated for. */
	protected String featureIdentifier;
	protected String searchedVersion;
	/** Target feature. */
	protected IFeature feature;
	/** The featurename with its version number */
	protected String featureFullName;
	protected String featureFolderName;
	protected String featureRootLocation;
	protected String featureTempFolder;
	protected Feature sourceFeature;
	protected PluginEntry sourcePlugin;
	protected String sourceFeatureFullName;
	protected String sourceFeatureFullNameVersionned;
	protected SourceFeatureInformation sourceToGather;
	protected boolean sourcePluginOnly = false;
	private String[] extraPlugins = new String[0];
	private boolean generateJnlp = false;
	private boolean signJars = false;
	private boolean generateVersionSuffix = false;

	//Cache the result of compteElements for performance
	private List computedElements = null;
	
	private static final String TEMPLATE = "data"; //$NON-NLS-1$
	
	public FeatureBuildScriptGenerator() {
		super();
	}

	/**
	 * Constructor FeatureBuildScriptGenerator.
	 */
	public FeatureBuildScriptGenerator(String featureId, String versionId, AssemblyInformation informationGathering) throws CoreException {
		if (featureId == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Messages.error_missingFeatureId, null));
		}
		this.featureIdentifier = featureId;
		this.searchedVersion = versionId;
		assemblyData = informationGathering;
	}

	/**
	 * Returns a list of BundleDescription objects representing the elements delivered by the feature. 
	 *  
	 * @return List of BundleDescription
	 * @throws CoreException
	 */
	protected List computeElements() throws CoreException {
		if (computedElements != null)
			return computedElements;
		
		computedElements = new ArrayList(5);
		IPluginEntry[] pluginList = feature.getPluginEntries();
		for (int i = 0; i < pluginList.length; i++) {
			IPluginEntry entry = pluginList[i];
			VersionedIdentifier identifier = entry.getVersionedIdentifier();
			BundleDescription model;
			if (selectConfigs(entry).size()==0)
				continue;

			String versionRequested = identifier.getVersion().toString();
			model = getSite(false).getRegistry().getResolvedBundle(identifier.getIdentifier(), versionRequested);
			if (model == null && getBuildProperties().containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + identifier.getIdentifier())) {
				generateEmbeddedSource(identifier.getIdentifier());
				model = getSite(false).getRegistry().getResolvedBundle(identifier.getIdentifier(), versionRequested);
			}
			if (model == null) {
				String message = NLS.bind(Messages.exception_missingPlugin, entry.getVersionedIdentifier());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}
			
			associateModelAndEntry(model, entry);
			
			computedElements.add(model);
			collectElementToAssemble(pluginList[i]);
			collectSourcePlugins(pluginList[i], model);
		}
		return computedElements;
	}

	private void associateModelAndEntry(BundleDescription model, IPluginEntry entry) {
		Properties bundleProperties = ((Properties) model.getUserObject());
		if (bundleProperties == null) { 
			bundleProperties = new Properties();
			model.setUserObject(bundleProperties);
		}
		ArrayList entries = (ArrayList) bundleProperties.get(PLUGIN_ENTRY);
		if (entries == null) {
			entries = new ArrayList();
			bundleProperties.put(PLUGIN_ENTRY, entries);
		}
		entries.add(entry);
	}

	private void generateEmbeddedSource(String pluginId) throws CoreException {
		FeatureBuildScriptGenerator featureGenerator = new FeatureBuildScriptGenerator(Utils.getArrayFromString(getBuildProperties().getProperty(GENERATION_SOURCE_PLUGIN_PREFIX + pluginId))[0], null, assemblyData);
		featureGenerator.setGenerateIncludedFeatures(false);
		featureGenerator.setAnalyseChildren(analysePlugins);
		featureGenerator.setSourceFeatureGeneration(true);
		featureGenerator.setExtraPlugins(Utils.getArrayFromString(getBuildProperties().getProperty(GENERATION_SOURCE_PLUGIN_PREFIX + pluginId)));
		featureGenerator.setBinaryFeatureGeneration(false);
		featureGenerator.setScriptGeneration(false);
		featureGenerator.setPluginPath(pluginPath);
		featureGenerator.setBuildSiteFactory(siteFactory);
		featureGenerator.setDevEntries(devEntries);
		featureGenerator.setCompiledElements(getCompiledElements());
		featureGenerator.setSourceToGather(sourceToGather);
		featureGenerator.setSourcePluginOnly(true);
		featureGenerator.setBuildingOSGi(isBuildingOSGi());
		featureGenerator.includePlatformIndependent(isPlatformIndependentIncluded());
		featureGenerator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
		featureGenerator.setGenerateVersionSuffix(generateVersionSuffix);
		featureGenerator.generate();
	}

	public void setSourcePluginOnly(boolean b) {
		sourcePluginOnly = b;
	}

	private void collectSourcePlugins(IPluginEntry pluginEntry, BundleDescription model) {
		if (!sourceFeatureGeneration)
			return;
		// The generic entry may not be part of the configuration we are building however,
		// the code for a non platform specific plugin still needs to go into a generic source plugin
		if (pluginEntry.getOS() == null && pluginEntry.getWS() == null && pluginEntry.getOSArch() == null) {
			sourceToGather.addElementEntry(Config.genericConfig(), model);
			return;
		}
		// Here we fan the plugins into the source fragment where they should go
		List correctConfigs = selectConfigs(pluginEntry);
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			sourceToGather.addElementEntry((Config) iter.next(), model);
		}
	}

	/**
	 * Set the boolean for whether or not children scripts should be generated.
	 * 
	 * @param generate
	 *                   <code>true</code> if the children scripts should be
	 *                   generated, <code>false</code> otherwise
	 */
	public void setAnalyseChildren(boolean generate) {
		analysePlugins = generate;
	}

	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		String message;
		if (workingDirectory == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_BUILDDIRECTORY_LOCATION_MISSING, Messages.error_missingInstallLocation, null));
		}
		initializeVariables();

		// if the feature defines its own custom script, we do not generate a
		// new one but we do try to update the version number
		String custom = (String) getBuildProperties().get(PROPERTY_CUSTOM);
		if (TRUE.equalsIgnoreCase(custom)) {
			File buildFile = new File(featureRootLocation, DEFAULT_BUILD_SCRIPT_FILENAME);
			if (!buildFile.exists()) {
				message = NLS.bind(Messages.error_missingCustomBuildFile, buildFile);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, null));
			}
			try {
				updateVersion(buildFile, PROPERTY_FEATURE_VERSION_SUFFIX, feature.getVersionedIdentifier().getVersion().toString());
			} catch (IOException e) {
				message = NLS.bind(Messages.exception_writeScript, buildFile);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, e));
			}
			return;
		}
		if (analyseIncludedFeatures)
			generateIncludedFeatureBuildFile();
		if (sourceFeatureGeneration)
			generateSourceFeature();
		if (analysePlugins)
			generateChildrenScripts();
		if (sourceFeatureGeneration) {
			addSourceFragmentsToFeature();
			writeSourceFeature();
		}
		if (!sourcePluginOnly)
			collectElementToAssemble(feature);

		// Do the recursive generation of build files for the features required by the current feature
		if (sourceFeatureGeneration)
			generateSourceFeatureScripts();
		if (scriptGeneration) {
			openScript(featureRootLocation, DEFAULT_BUILD_SCRIPT_FILENAME);
			try {
				generateBuildScript();
			} finally {
				closeScript();
			}
		}
	}

	protected void generateIncludedFeatureBuildFile() throws CoreException {
		IIncludedFeatureReference[] referencedFeatures = feature.getIncludedFeatureReferences();
		for (int i = 0; i < referencedFeatures.length; i++) {
			String featureId = ((IncludedFeatureReferenceModel) referencedFeatures[i]).getFeatureIdentifier();
			String featureVersion = ((IncludedFeatureReferenceModel) referencedFeatures[i]).getFeatureVersion();
			//If the feature which is included is a source feature, then instead of calling the generation of the featureID we are
			// calling the generation of the corresponding binary feature but without generating the  scripts (set binaryFeatureGeneration to false)
			boolean doSourceFeatureGeneration = getBuildProperties().containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId);
			FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator(doSourceFeatureGeneration == true ? Utils.getArrayFromString(getBuildProperties().getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId))[0] : featureId, featureVersion, assemblyData);
			//If we are  generating a  source  feature we don't  want to go recursively
			generator.setGenerateIncludedFeatures(doSourceFeatureGeneration ? false : true);
			generator.setAnalyseChildren(analysePlugins);
			generator.setSourceFeatureGeneration(doSourceFeatureGeneration);
			generator.setBinaryFeatureGeneration(!doSourceFeatureGeneration);
			//We don't want to regenerate the scripts for the binary feature we are reading to build the source feature
			generator.setScriptGeneration(doSourceFeatureGeneration ? false : true);
			if (doSourceFeatureGeneration)
				generator.setExtraPlugins(Utils.getArrayFromString(getBuildProperties().getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId)));
			generator.setPluginPath(pluginPath);
			generator.setBuildSiteFactory(siteFactory);
			generator.setDevEntries(devEntries);
			generator.setCompiledElements(getCompiledElements());
			generator.setSourceToGather(new SourceFeatureInformation());
			generator.setBuildingOSGi(isBuildingOSGi());
			generator.includePlatformIndependent(isPlatformIndependentIncluded());
			generator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
			generator.setGenerateVersionSuffix(generateVersionSuffix);
			generator.generate();
		}
	}

	protected void setExtraPlugins(String[] plugins) {
		extraPlugins = plugins;
	}

	/**
	 * Main call for generating the script.
	 * 
	 * @throws CoreException
	 */
	private void generateBuildScript() throws CoreException {
		if (BundleHelper.getDefault().isDebugging())
			System.out.println("Generating feature " + featureFullName); //$NON-NLS-1$
		generatePrologue();
		generateAllPluginsTarget();
		generateAllFeaturesTarget();
		generateUpdateFeatureFile();
		generateAllChildrenTarget();
		generateChildrenTarget();
		generateBuildJarsTarget();
		generateBuildZipsTarget();
		generateBuildUpdateJarTarget();
		generateGatherBinPartsTarget();
		generateZipDistributionWholeTarget();
		generateZipSourcesTarget();
		generateZipLogsTarget();
		generateCleanTarget();
		generateRefreshTarget();
		generateGatherSourcesTarget();
		generateGatherLogsTarget();
		generateEpilogue();
	}

	/**
	 * Method generateGatherSource. Used to enable the recursive call of
	 * gathering the sources for the features
	 */
	private void generateGatherSourcesTarget() {
		script.printTargetDeclaration(TARGET_GATHER_SOURCES, null, null, null, null);
		Map params = new HashMap(2);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_FEATURE_TEMP_FOLDER) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + sourceFeatureFullNameVersionned + '/' + "src"); //$NON-NLS-1$
		params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
		script.printAntCallTask(TARGET_CHILDREN, null, params);
		script.printTargetEnd();
	}

	/**
	 * Method generateGatherSource. Used to enable the recursive call of
	 * gathering the sources for the features
	 */
	private void generateGatherLogsTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_LOGS, TARGET_INIT, null, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_GATHER_LOGS);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(featureTempFolder).append(DEFAULT_PLUGIN_LOCATION).toString());
		script.printAntCallTask(TARGET_ALL_CHILDREN, "false", params); //$NON-NLS-1$
		script.printTargetEnd();
	}

	private void generateUpdateFeatureFile() {
		script.printTargetDeclaration(TARGET_UPDATE_FEATURE_FILE, TARGET_INIT, null, null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>build.zips</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateBuildZipsTarget() throws CoreException {
		StringBuffer zips = new StringBuffer();
		Properties props = getBuildProperties();
		for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_ZIP_SUFFIX)) {
				String zipName = key.substring(PROPERTY_SOURCE_PREFIX.length());
				zips.append(',');
				zips.append(zipName);
				generateZipIndividualTarget(zipName, (String) entry.getValue());
			}
		}
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
		Map params = new HashMap(2);
		params.put(PROPERTY_TARGET, TARGET_BUILD_ZIPS);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
	}

	/**
	 * Add a <code>zip</code> target to the given Ant script.
	 * 
	 * @param zipName the name of the zip file to create
	 * @param source the directory name to read the files from
	 */
	private void generateZipIndividualTarget(String zipName, String source) {
		script.println();
		script.printTargetDeclaration(zipName, TARGET_INIT, null, null, null);
		script.printZipTask(Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + zipName, Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + source, false, false, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>clean</code> target to the given Ant script.
	 */
	private void generateCleanTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_CLEAN, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_clean, featureIdentifier));
		script.printDeleteTask(null, Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".jar", null); //$NON-NLS-1$
		script.printDeleteTask(null, Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".bin.dist.zip", null); //$NON-NLS-1$
		script.printDeleteTask(null, Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".log.zip", null); //$NON-NLS-1$
		script.printDeleteTask(null, Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".src.zip", null); //$NON-NLS-1$
		script.printDeleteTask(featureTempFolder, null, null);
		Map params = new HashMap(2);
		params.put(PROPERTY_TARGET, TARGET_CLEAN);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>zip.logs</code> target to the given Ant script.
	 */
	private void generateZipLogsTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_LOGS, TARGET_INIT, null, null, null);
		script.printDeleteTask(featureTempFolder, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
		params.put(PROPERTY_TARGET, TARGET_GATHER_LOGS);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(featureTempFolder).append(DEFAULT_PLUGIN_LOCATION).toString()); 
		script.printAntCallTask(TARGET_ALL_CHILDREN, "false", params); //$NON-NLS-1$
		IPath destination = new Path(Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION)).append(featureFullName + ".log.zip"); //$NON-NLS-1$
		script.printZipTask(destination.toString(), featureTempFolder, true, false, null);
		script.printDeleteTask(featureTempFolder, null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>zip.sources</code> target to the given Ant script.
	 */
	protected void generateZipSourcesTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
		script.printDeleteTask(featureTempFolder, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
		params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, featureTempFolder + '/' + DEFAULT_PLUGIN_LOCATION + '/' + sourceFeatureFullNameVersionned + '/' + "src"); //$NON-NLS-1$
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printZipTask(Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".src.zip", featureTempFolder, true, false, null); //$NON-NLS-1$
		script.printDeleteTask(featureTempFolder, null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>gather.bin.parts</code> target to the given Ant script
	 * 
	 * @throws CoreException
	 */
	private void generateGatherBinPartsTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(Utils.getPropertyFormat(PROPERTY_FEATURE_BASE)).append(DEFAULT_PLUGIN_LOCATION).toString());
		script.printAntCallTask(TARGET_CHILDREN, null, params);
		String include = (String) getBuildProperties().get(PROPERTY_BIN_INCLUDES);
		String exclude = (String) getBuildProperties().get(PROPERTY_BIN_EXCLUDES);
		String root = Utils.getPropertyFormat(PROPERTY_FEATURE_BASE) + '/' + featureFolderName;

		if (include != null) {
			script.printMkdirTask(root);
			if (include != null || exclude != null) {
				FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
				script.printCopyTask(null, root, new FileSet[] {fileSet}, true, false);
			}
			// Generate the parameters for the Id Replacer.
			String featureVersionInfo = ""; //$NON-NLS-1$
			// Here we get all the included features (independently of the config being built so the version numbers in the feature can be replaced)
			IIncludedFeatureReference[] includedFeatures = feature.getRawIncludedFeatureReferences();
			for (int i = 0; i < includedFeatures.length; i++) {
				String versionId = includedFeatures[i].getVersionedIdentifier().getVersion().toString();
				IFeature includedFeature = getSite(false).findFeature(includedFeatures[i].getVersionedIdentifier().getIdentifier(), versionId, true);
				VersionedIdentifier includedFeatureVersionId = includedFeature.getVersionedIdentifier();
				featureVersionInfo += (includedFeatureVersionId.getIdentifier() + ',' + includedFeatureVersionId.getVersion().toString() + ',');
			}
			String pluginVersionInfo = ""; //$NON-NLS-1$
			// Here we get all the included plugins (independently of the config being built so the version numbers in the feature can be replaced)
			IPluginEntry[] pluginsIncluded = feature.getRawPluginEntries();
			for (int i = 0; i < pluginsIncluded.length; i++) {
				VersionedIdentifier identifier = pluginsIncluded[i].getVersionedIdentifier();
				BundleDescription model;
				// If we ask for 0.0.0, the call to the registry must have null as a parameter
				String versionRequested = identifier.getVersion().toString();
				String entryIdentifier = identifier.getIdentifier();
				model = getSite(false).getRegistry().getResolvedBundle(entryIdentifier, versionRequested);
				if (model != null)
					pluginVersionInfo += (entryIdentifier + ',' + model.getVersion() + ',');
			}
			script.println("<eclipse.idReplacer featureFilePath=\"" + root + '/' + DEFAULT_FEATURE_FILENAME_DESCRIPTOR + "\"  selfVersion=\"" + feature.getVersionedIdentifier().getVersion() + "\" featureIds=\"" + featureVersionInfo + "\" pluginIds=\"" + pluginVersionInfo + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		generateRootFilesAndPermissionsCalls();
		script.printTargetEnd();
		generateRootFilesAndPermissions();
	}

	/**
	 *  
	 */
	private void generateRootFilesAndPermissionsCalls() {
		script.printAntCallTask(TARGET_ROOTFILES_PREFIX + Utils.getPropertyFormat(PROPERTY_OS) + '_' + Utils.getPropertyFormat(PROPERTY_WS) + '_' + Utils.getPropertyFormat(PROPERTY_ARCH), null, null);
	}

	/**
	 *  
	 */
	private void generateRootFilesAndPermissions() throws CoreException {
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();) {
			Config aConfig = (Config) iter.next();
			script.printTargetDeclaration(TARGET_ROOTFILES_PREFIX + aConfig.toString("_"), null, null, null, null); //$NON-NLS-1$
			generateCopyRootFiles(aConfig);
			Utils.generatePermissions(getBuildProperties(), aConfig, PROPERTY_FEATURE_BASE, script);
			script.printTargetEnd();
		}
	}

	private void generateCopyRootFiles(Config aConfig) throws CoreException {
		String configName;
		String baseList = getBuildProperties().getProperty(ROOT, ""); //$NON-NLS-1$
		String fileList = getBuildProperties().getProperty(ROOT_PREFIX + aConfig.toString("."), ""); //$NON-NLS-1$ //$NON-NLS-2$
		fileList = (fileList.length() == 0 ? "" : fileList + ',') + baseList; //$NON-NLS-1$
		if (fileList.equals("")) //$NON-NLS-1$
			return;

		assemblyData.addRootFileProvider(aConfig, feature);
		configName = aConfig.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
		script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_FEATURE_BASE) + '/' + configName + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER));
		String[] files = Utils.getArrayFromString(fileList, ","); //$NON-NLS-1$
		FileSet[] fileSet = new FileSet[files.length];
		for (int i = 0; i < files.length; i++) {
			String fromDir = Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/';
			String file = files[i];
			if (file.startsWith("absolute:")) { //$NON-NLS-1$
				file = file.substring(9);
				fromDir = ""; //$NON-NLS-1$
			}
			if (file.startsWith("file:")) { //$NON-NLS-1$
				IPath target = new Path(file.substring(5));
				fileSet[i] = new FileSet(fromDir + target.removeLastSegments(1), null, target.lastSegment(), null, null, null, null);
			} else {
				fileSet[i] = new FileSet(fromDir + file, null, "**", null, null, null, null); //$NON-NLS-1$
			}
		}
		String shouldOverwrite =  getBuildProperties().getProperty(PROPERTY_OVERWRITE_ROOTFILES, "true"); //$NON-NLS-1$
		script.printCopyTask(null, Utils.getPropertyFormat(PROPERTY_FEATURE_BASE) + '/' + configName + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER), fileSet, true, Boolean.valueOf(shouldOverwrite).booleanValue());
	}



	/**
	 * Add the <code>build.update.jar</code> target to the given script.
	 */
	private void generateBuildUpdateJarTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_buildUpdateJar, featureIdentifier));
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printProperty(PROPERTY_FEATURE_BASE, featureTempFolder);
		script.printDeleteTask(featureTempFolder, null, null);
		script.printMkdirTask(featureTempFolder);
		params.clear();
		params.put(PROPERTY_FEATURE_BASE, featureTempFolder);
		params.put(PROPERTY_OS, feature.getOS() == null ? Config.ANY : feature.getOS());
		params.put(PROPERTY_WS, feature.getWS() == null ? Config.ANY : feature.getWS());
		params.put(PROPERTY_ARCH, feature.getOSArch() == null ? Config.ANY : feature.getOSArch());
		params.put(PROPERTY_NL, feature.getNL() == null ? Config.ANY : feature.getNL());
		// Be sure to call the gather with children turned off. The only way to
		// do this is
		// to clear all inherited values. Must remember to setup anything that
		// is really expected.
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, "false", params); //$NON-NLS-1$
		String jar = Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".jar"; //$NON-NLS-1$
		script.printJarTask(jar, featureTempFolder + '/' + featureFolderName, null);
		script.printDeleteTask(featureTempFolder, null, null);
		if (generateJnlp)
			script.println("<eclipse.jnlpGenerator feature=\"" + jar + "\"  codebase=\"" + Utils.getPropertyFormat("jnlp.codebase") + "\" j2se=\"" + Utils.getPropertyFormat("jnlp.j2se") + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ 
		if (signJars)
			script.println("<signjar jar=\"" + jar + "\" alias=\"" + Utils.getPropertyFormat("sign.alias") + "\" keystore=\"" + Utils.getPropertyFormat("sign.keystore") + "\" storepass=\"" + Utils.getPropertyFormat("sign.storepass") + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ 
		script.printTargetEnd();
	}

	/**
	 * Add the <code>zip.distribution</code> target to the given Ant script.
	 * Zip up the whole feature.
	 */
	protected void generateZipDistributionWholeTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_zips, featureIdentifier));
		script.printDeleteTask(featureTempFolder, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_FEATURE_BASE, featureTempFolder);
		params.put(PROPERTY_INCLUDE_CHILDREN, TRUE);
		params.put(PROPERTY_OS, feature.getOS() == null ? Config.ANY : feature.getOS());
		params.put(PROPERTY_WS, feature.getWS() == null ? Config.ANY : feature.getWS());
		params.put(PROPERTY_ARCH, feature.getOSArch() == null ? Config.ANY : feature.getOSArch());
		params.put(PROPERTY_NL, feature.getNL() == null ? Config.ANY : feature.getNL());
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, null, params);
		script.printZipTask(Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".bin.dist.zip", featureTempFolder, false, false, null); //$NON-NLS-1$
		script.printDeleteTask(featureTempFolder, null, null);
		script.printTargetEnd();
	}

	/**
	 * Executes a given target in all children's script files.
	 */
	private void generateAllChildrenTarget() {
		StringBuffer depends = new StringBuffer();
		depends.append(TARGET_INIT);
		depends.append(',');
		depends.append(TARGET_ALL_FEATURES);
		depends.append(',');
		depends.append(TARGET_ALL_PLUGINS);
		depends.append(',');
		depends.append(TARGET_UPDATE_FEATURE_FILE);
		script.println();
		script.printTargetDeclaration(TARGET_ALL_CHILDREN, depends.toString(), null, null, null);
		script.printTargetEnd();
	}

	/**
	 * Target responsible for delegating target calls to plug-in's build.xml
	 * scripts. Plugins are sorted according to the requires chain. Fragments
	 * are inserted afterward
	 * 
	 * @throws CoreException
	 */
	protected void generateAllPluginsTarget() throws CoreException {
		List plugins = computeElements();
		plugins = Utils.extractPlugins(getSite(false).getRegistry().getSortedBundles(), plugins);
		script.println();
		script.printTargetDeclaration(TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
		Set writtenCalls = new HashSet(plugins.size());
		for (Iterator iter = plugins.iterator(); iter.hasNext();) {
			BundleDescription current = (BundleDescription) iter.next();
			//If it is not a compiled element, then we don't generate a call
			Properties bundleProperties = (Properties) current.getUserObject();
			if (bundleProperties == null || bundleProperties.get(IS_COMPILED) == null || bundleProperties.get(IS_COMPILED) == Boolean.FALSE)
				continue;
			// Get the os / ws / arch to pass as a parameter to the plugin
			if (writtenCalls.contains(current))
				continue;
			writtenCalls.add(current);
			IPluginEntry[] entries = Utils.getPluginEntry(feature, current.getSymbolicName(), false);	//TODO This can be improved to use the value from the user object in the bundleDescription
			for (int j = 0; j < entries.length; j++) {
				List list = selectConfigs(entries[j]);
				if (list.size() == 0)
					continue;
				Map params = null;
				Config aMatchingConfig = (Config) list.get(0);
				params = new HashMap(3);
				if (!aMatchingConfig.getOs().equals(Config.ANY))
					params.put(PROPERTY_OS, aMatchingConfig.getOs());
				if (!aMatchingConfig.getWs().equals(Config.ANY))
					params.put(PROPERTY_WS, aMatchingConfig.getWs());
				if (!aMatchingConfig.getArch().equals(Config.ANY))
					params.put(PROPERTY_ARCH, aMatchingConfig.getArch());
				IPath location = Utils.makeRelative(new Path(getLocation(current)), new Path(featureRootLocation));
				script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toString(), Utils.getPropertyFormat(PROPERTY_TARGET), null, null, params);
			}
		}
		script.printTargetEnd();
	}

	private void generateAllFeaturesTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_ALL_FEATURES, TARGET_INIT, null, null, null);
		if (analyseIncludedFeatures) {
			IIncludedFeatureReference[] features = feature.getIncludedFeatureReferences();
			for (int i = 0; i < features.length; i++) {
				String featureId = features[i].getVersionedIdentifier().getIdentifier();
				String versionId = features[i].getVersionedIdentifier().getVersion().toString();
				IFeature includedFeature = getSite(false).findFeature(featureId, versionId, false);
				if (includedFeature == null) {
					String message = NLS.bind(Messages.exception_missingFeature, featureId + ' ' + versionId);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
				}
				if (includedFeature instanceof BuildTimeFeature) {
					if (((BuildTimeFeature) includedFeature).isBinary())
						continue;
				}
				
				String includedFeatureDirectory = includedFeature.getURL().getPath();
				int j = includedFeatureDirectory.lastIndexOf(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
				if (j != -1)
					includedFeatureDirectory = includedFeatureDirectory.substring(0, j);
				IPath location;
				location = Utils.makeRelative(new Path(includedFeatureDirectory), new Path(featureRootLocation));
				script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toString(), Utils.getPropertyFormat(PROPERTY_TARGET), null, null, null);
			}
		}
		script.printTargetEnd();
	}

	/**
	 * Just ends the script.
	 */
	private void generateEpilogue() {
		script.println();
		script.printProjectEnd();
	}

	/**
	 * Defines, the XML declaration, Ant project and init target.
	 */
	private void generatePrologue() {
		script.printProjectDeclaration(feature.getVersionedIdentifier().getIdentifier(), TARGET_BUILD_UPDATE_JAR, "."); //$NON-NLS-1$
		script.println();
		script.printTargetDeclaration(TARGET_INIT, null, null, null, null);
		script.printProperty(PROPERTY_FEATURE_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + PROPERTY_FEATURE_TEMP_FOLDER);
		script.printProperty(PROPERTY_FEATURE_DESTINATION, Utils.getPropertyFormat(PROPERTY_BASEDIR));
		script.printTargetEnd();
	}

	/**
	 * @throws CoreException
	 */
	private void generateChildrenScripts() throws CoreException {
		List plugins = computeElements();
		String suffix = generateFeatureVersionSuffix((BuildTimeFeature) feature, plugins);
		if (suffix != null) {
			PluginVersionIdentifier versionId = feature.getVersionedIdentifier().getVersion();
			String qualifier = versionId.getQualifierComponent();
			qualifier = qualifier.substring(0, ((BuildTimeFeature) feature).getContextQualifierLength());
			qualifier = qualifier + '-' + suffix;
			versionId = new PluginVersionIdentifier(versionId.getMajorComponent(), versionId.getMinorComponent(), versionId.getServiceComponent(), qualifier);
			String newVersion = versionId.toString();
			((BuildTimeFeature) feature).setFeatureVersion(newVersion);
			initializeFeatureNames(); //reset our variables
		}
		generateModels(Utils.extractPlugins(getSite(false).getRegistry().getSortedBundles(), plugins));
	}

	private String generateFeatureVersionSuffix(BuildTimeFeature buildFeature, List plugins) throws CoreException {
		if (!generateVersionSuffix || buildFeature.getContextQualifierLength() == -1)
			return null; //no qualifier, do nothing 

		Properties properties = getBuildProperties();
		String significantDigits = (String) properties.get(PROPERTY_SIGNIFICANT_VERSION_DIGITS);
		int maxDigits = 9;
		if (significantDigits != null) {
			try {
				maxDigits = Integer.parseInt(significantDigits);
			} catch (NumberFormatException e) {
				//exception, leave at default
			}
		}
		String generatedLength = (String) properties.get(PROPERTY_GENERATED_VERSION_LENGTH);
		int finalLength = 10;
		if (generatedLength != null) {
			try {
				finalLength = Integer.parseInt(generatedLength);
			} catch (NumberFormatException e) {
				//exception, leave at default
			}
		}

		IIncludedFeatureReference[] referencedFeatures = buildFeature.getIncludedFeatureReferences();
		int numElements = plugins.size() + referencedFeatures.length;
		byte[][] versions = new byte[numElements][];
		int idx = -1;
		int longestByteArray = 0;
		int shift = '-';
		String qualifier;
		if (referencedFeatures != null) {
			for (int i = 0; i < referencedFeatures.length; i++) {
				BuildTimeFeature refFeature = (BuildTimeFeature) getSite(false).findFeature(referencedFeatures[i].getVersionedIdentifier().getIdentifier(), null, false);
				if (refFeature == null)
					continue;
				qualifier = refFeature.getVersionedIdentifier().getVersion().getQualifierComponent();
				if (qualifier.length() > 0) {
					versions[++idx] = qualifier.getBytes();
					if (versions[idx].length > longestByteArray)
						longestByteArray = versions[idx].length;
				}
			}
		}
		for (Iterator iterator = plugins.iterator(); iterator.hasNext();) {
			BundleDescription model = (BundleDescription) iterator.next();
			qualifier = model.getVersion().getQualifier();
			if (qualifier.length() > 0) {
				versions[++idx] = qualifier.getBytes();
				if (versions[idx].length > longestByteArray)
					longestByteArray = versions[idx].length;
			}
		}
		
		if (idx == -1)
			return null;
		
		if (longestByteArray > maxDigits)
			longestByteArray = maxDigits;

		int[] sums = new int[longestByteArray];
		for (int i = 0; i <= idx; i++) {
			for (int j = 0; j < longestByteArray; j++) {
				if (versions[i].length > j)
					sums[j] += versions[i][j] - shift;
			}
		}
		char[] result = new char[finalLength];
		int x = 0, val = 0, carry = 0;
		char c = 0;

		int i = -1;
		while (++i < sums.length || carry != 0) {
			x = carry + ((i < longestByteArray) ? sums[i] : 0);
			val = x % 64;
			carry = x >> 6;

			if (val == 0)
				c = '-';
			else if (val > 0 && val <= 10)
				c = (char) ('0' + val - 1);
			else if (val == 11)
				c = '_';
			else if (val > 11 && val <= 37)
				c = (char) ('A' + val - 12);
			else if (val > 37 && val <= 63)
				c = (char) ('a' + val - 38);

			result[finalLength - i - 1] = c;
		}
		for (; i < finalLength; i++) {
			result[finalLength - i - 1] = '-';
		}

		return String.valueOf(result);
	}

	/**
	 * @param models
	 * @throws CoreException
	 */
	private void generateModels(List models) throws CoreException {
		if (scriptGeneration == false)
			return;
		if (binaryFeature == false || models.isEmpty())
			return;
		
		Set generatedScripts = new HashSet(models.size());
		for (Iterator iterator = models.iterator(); iterator.hasNext();) {
			BundleDescription model = (BundleDescription) iterator.next();
			if (generatedScripts.contains(model))
				continue;
			generatedScripts.add(model);
			
			ArrayList matchingEntries = (ArrayList) ((Properties) model.getUserObject()).get(PLUGIN_ENTRY);
			for (Iterator entryIter = matchingEntries.iterator(); entryIter.hasNext();) {
				IPluginEntry correspondingEntry = (IPluginEntry) entryIter.next();
				List list = selectConfigs(correspondingEntry);
				if (list.size() == 0)
					continue;
				
				ModelBuildScriptGenerator generator = new ModelBuildScriptGenerator();
				generator.setBuildSiteFactory(siteFactory);
				generator.setCompiledElements(getCompiledElements());
				generator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
				generator.setModel(model); // setModel has to be called before configurePersistentProperties because it reads the model's properties
				generator.setFeatureGenerator(this);
				generator.setPluginPath(getPluginPath());
				generator.setBuildingOSGi(isBuildingOSGi());
				generator.setDevEntries(devEntries);
				generator.includePlatformIndependent(isPlatformIndependentIncluded());
				generator.setSignJars(signJars);
				generator.setAssociatedEntry(correspondingEntry);
				generator.generate();
			}
		}
	
	}

	/**
	 * Set this object's feature id to be the given value.
	 * 
	 * @param featureID the feature id
	 * @throws CoreException if the given feature id is <code>null</code>
	 */
	public void setFeature(String featureID) throws CoreException {
		if (featureID == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Messages.error_missingFeatureId, null));
		}
		this.featureIdentifier = featureID;
	}

	public static String getNormalizedName(IFeature feature) {
		return feature.getVersionedIdentifier().toString();
	}
	private void initializeVariables() throws CoreException {
		feature = getSite(false).findFeature(featureIdentifier, searchedVersion, true);

		if (featureRootLocation == null) {
			featureRootLocation = feature.getURL().getPath();
			int i = featureRootLocation.lastIndexOf(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (i != -1)
				featureRootLocation = featureRootLocation.substring(0, i);
		}
		initializeFeatureNames();

		if (feature instanceof BuildTimeFeature) {
			if (getBuildProperties() == MissingProperties.getInstance()) {
				BuildTimeFeature buildFeature = (BuildTimeFeature) feature;
				scriptGeneration = false;
				buildFeature.setBinary(true);
			}
		}
	}

	private void initializeFeatureNames() throws CoreException {
		featureFullName = getNormalizedName(feature);
		featureFolderName = DEFAULT_FEATURE_LOCATION + '/' + featureFullName;
		sourceFeatureFullName = computeSourceFeatureName(feature, false);
		sourceFeatureFullNameVersionned = computeSourceFeatureName(feature, true);
		featureTempFolder = Utils.getPropertyFormat(PROPERTY_FEATURE_TEMP_FOLDER);
	}

	private String computeSourceFeatureName(IFeature featureForName, boolean withNumber) throws CoreException {
		String sourceFeatureName = getBuildProperties().getProperty(PROPERTY_SOURCE_FEATURE_NAME);
		if (sourceFeatureName == null)
			sourceFeatureName = featureForName.getVersionedIdentifier().getIdentifier() + ".source"; //$NON-NLS-1$
		return sourceFeatureName + (withNumber ? "_" + featureForName.getVersionedIdentifier().getVersion().toString() : ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return a properties object constructed from the build.properties file
	 * for the given feature. If no file exists, then an empty properties
	 * object is returned.
	 * 
	 * @return Properties the feature's build.properties
	 * @throws CoreException
	 * @see Feature
	 */
	protected Properties getBuildProperties() throws CoreException {
		if (buildProperties == null)
			buildProperties = readProperties(featureRootLocation, PROPERTIES_FILE, isIgnoreMissingPropertiesFile() ? IStatus.OK : IStatus.WARNING);
		return buildProperties;
	}
	
	/**
	 * Add the <code>children</code> target to the given Ant script.
	 * Delegates some target call to all-template only if the property
	 * includeChildren is set.
	 */
	private void generateChildrenTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_CHILDREN, null, PROPERTY_INCLUDE_CHILDREN, null, null);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>build.jars</code> target to the given Ant script.
	 */
	private void generateBuildJarsTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_JARS, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_buildJars, featureIdentifier));
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_BUILD_JARS);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
		params.clear();
		params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>refresh</code> target to the given Ant script.
	 */
	private void generateRefreshTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_REFRESH, TARGET_INIT, PROPERTY_ECLIPSE_RUNNING, null, NLS.bind(Messages.build_feature_refresh, featureIdentifier));
		script.printConvertPathTask(new Path(featureRootLocation).removeLastSegments(0).toOSString().replace('\\', '/'), PROPERTY_RESOURCE_PATH, false);
		script.printRefreshLocalTask(Utils.getPropertyFormat(PROPERTY_RESOURCE_PATH), "infinite"); //$NON-NLS-1$
		Map params = new HashMap(2);
		params.put(PROPERTY_TARGET, TARGET_REFRESH);
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
	}

	public void setGenerateIncludedFeatures(boolean recursiveGeneration) {
		analyseIncludedFeatures = recursiveGeneration;
	}

	protected void collectElementToAssemble(IFeature featureToCollect) throws CoreException {
		if (scriptGeneration == true && (assemblyData == null || getBuildProperties().get(PROPERTY_BIN_INCLUDES) == null))
			return;
		List correctConfigs = selectConfigs(featureToCollect);
		// Here, we could sort if the feature is a common one or not by
		// comparing the size of correctConfigs
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			assemblyData.addFeature(config, feature);
		}
	}

	/**
	 * Method generateSourceFeature.
	 */
	private void generateSourceFeature() throws CoreException {
		Feature featureExample = (Feature) feature;
		sourceFeature = createSourceFeature(featureExample);
		associateExtraPlugins();
		if (isBuildingOSGi())
			sourcePlugin = create30SourcePlugin();
		else
			sourcePlugin = createSourcePlugin();

		generateSourceFragment();
	}

	private void generateSourceFragment() throws CoreException {
		Map fragments = sourceToGather.getElementEntries();
		for (Iterator iter = fragments.entrySet().iterator(); iter.hasNext();) {
			Map.Entry fragmentInfo = (Map.Entry) iter.next();
			Config configInfo = (Config) fragmentInfo.getKey();
			if (configInfo.equals(Config.genericConfig()))
				continue;
			PluginEntry sourceFragment = new PluginEntry();
			String sourceFragmentId = sourceFeature.getFeatureIdentifier() + "." + configInfo.toString("."); //$NON-NLS-1$ //$NON-NLS-2$
			sourceFragment.setPluginIdentifier(sourceFragmentId);
			sourceFragment.setPluginVersion(sourceFeature.getFeatureVersion());
			sourceFragment.setOS(configInfo.getOs());
			sourceFragment.setWS(configInfo.getWs());
			sourceFragment.setArch(configInfo.getArch());
			sourceFragment.isFragment(true);
			//sourceFeature.addPluginEntryModel(sourceFragment);
			if (isBuildingOSGi())
				create30SourceFragment(sourceFragment, sourcePlugin);
			else
				createSourceFragment(sourceFragment, sourcePlugin);
		}
	}

	//Add the relevant source fragments to the source feature
	private void addSourceFragmentsToFeature() {
		Map fragments = sourceToGather.getElementEntries();
		for (Iterator iter = fragments.entrySet().iterator(); iter.hasNext();) {
			Map.Entry fragmentInfo = (Map.Entry) iter.next();
			Config configInfo = (Config) fragmentInfo.getKey();
			if (configInfo.equals(Config.genericConfig()))
				continue;
			Set sourceList = (Set) fragmentInfo.getValue();
			if (sourceList.size() == 0)
				continue;
			PluginEntry sourceFragment = new PluginEntry();
			String sourceFragmentId = sourceFeature.getFeatureIdentifier() + "." + configInfo.toString("."); //$NON-NLS-1$ //$NON-NLS-2$
			sourceFragment.setPluginIdentifier(sourceFragmentId);
			sourceFragment.setPluginVersion(sourceFeature.getFeatureVersion());
			sourceFragment.setOS(configInfo.getOs());
			sourceFragment.setWS(configInfo.getWs());
			sourceFragment.setArch(configInfo.getArch());
			sourceFragment.isFragment(true);
			sourceFeature.addPluginEntryModel(sourceFragment);
			//createSourceFragment(sourceFragment, sourcePlugin);
		}
	}

	private void generateSourceFeatureScripts() throws CoreException {
		FeatureBuildScriptGenerator sourceScriptGenerator = new FeatureBuildScriptGenerator(sourceFeatureFullName, null, assemblyData);
		sourceScriptGenerator.setGenerateIncludedFeatures(true);
		sourceScriptGenerator.setAnalyseChildren(true);
		sourceScriptGenerator.setSourceToGather(sourceToGather);
		sourceScriptGenerator.setBinaryFeatureGeneration(true);
		sourceScriptGenerator.setSourceFeatureGeneration(false);
		sourceScriptGenerator.setScriptGeneration(true);
		sourceScriptGenerator.setPluginPath(pluginPath);
		sourceScriptGenerator.setBuildSiteFactory(siteFactory);
		sourceScriptGenerator.setDevEntries(devEntries);
		sourceScriptGenerator.setCompiledElements(getCompiledElements());
		sourceScriptGenerator.setSourcePluginOnly(sourcePluginOnly);
		sourceScriptGenerator.setBuildingOSGi(isBuildingOSGi());
		sourceScriptGenerator.includePlatformIndependent(isPlatformIndependentIncluded());
		sourceScriptGenerator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
		sourceScriptGenerator.setGenerateVersionSuffix(generateVersionSuffix);
		sourceScriptGenerator.generate();
	}

	// Add extra plugins into the given feature.
	private void associateExtraPlugins() throws CoreException {
		for (int i = 1; i < extraPlugins.length; i++) {
			BundleDescription model;
			// see if we have a plug-in or a fragment

			model = getSite(false).getRegistry().getResolvedBundle(extraPlugins[i].startsWith("plugin@") ? extraPlugins[i].substring(7) : extraPlugins[i].substring(8)); //$NON-NLS-1$

			if (model == null) {
				String message = NLS.bind(Messages.exception_missingPlugin, extraPlugins[i]);
				BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, extraPlugins[i], EXCEPTION_PLUGIN_MISSING, message, null));
				continue;
			}
			PluginEntry entry = new PluginEntry();
			entry.setPluginIdentifier(model.getSymbolicName());
			entry.setPluginVersion(model.getVersion().toString());
			sourceFeature.addPluginEntryModel(entry);
		}
	}

	private PluginEntry create30SourcePlugin() throws CoreException {
		//Create an object representing the plugin
		PluginEntry result = new PluginEntry();
		String sourcePluginId = sourceFeature.getFeatureIdentifier();
		result.setPluginIdentifier(sourcePluginId);
		result.setPluginVersion(sourceFeature.getFeatureVersion());
		sourceFeature.addPluginEntryModel(result);
		// create the directory for the plugin
		IPath sourcePluginDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(result, false) );
		File sourcePluginDir = sourcePluginDirURL.toFile();
		new File(sourcePluginDir, "META-INF").mkdirs(); //$NON-NLS-1$

		// Create the MANIFEST.MF
		StringBuffer buffer;
		Path templateManifest = new Path(TEMPLATE + "/30/plugin/" + DEFAULT_BUNDLE_FILENAME_DESCRIPTOR); //$NON-NLS-1$
		URL templateManifestURL = BundleHelper.getDefault().find(templateManifest);
		if (templateManifestURL == null) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, templateManifest), null);
			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
		try {
			buffer = readFile(templateManifestURL.openStream());
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, templateManifestURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		int beginId = scan(buffer, 0, REPLACED_PLUGIN_ID);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), result.getPluginIdentifier());
		//set the version number
		beginId = scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), result.getPluginVersion());
		try {
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourcePluginDirURL.append(DEFAULT_BUNDLE_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_writingFile, templateManifestURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		
		//Copy the plugin.xml
		try {
			InputStream pluginXML = BundleHelper.getDefault().getBundle().getEntry(TEMPLATE + "/30/plugin/plugin.xml").openStream(); //$NON-NLS-1$
			Utils.transferStreams(pluginXML, new FileOutputStream(sourcePluginDirURL.append(DEFAULT_PLUGIN_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, TEMPLATE + "/30/plugin/plugin.xml"); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e1));
		}

		//Copy the other files
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplatePlugin", sourcePluginDir.getAbsolutePath()); //$NON-NLS-1$
		//	If a build.properties file already exist then we use it supposing it is correct.
		File buildProperty = sourcePluginDirURL.append(PROPERTIES_FILE).toFile();
		if (!buildProperty.exists()) {
			copiedFiles.add(DEFAULT_PLUGIN_FILENAME_DESCRIPTOR); //Because the plugin.xml is not copied, we need to add it to the file
			copiedFiles.add("src/**/*.zip"); //$NON-NLS-1$
			copiedFiles.add(DEFAULT_BUNDLE_FILENAME_DESCRIPTOR);//Because the manifest.mf is not copied, we need to add it to the file
			Properties sourceBuildProperties = new Properties();
			sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
			sourceBuildProperties.put(SOURCE_PLUGIN_ATTRIBUTE, "true"); //$NON-NLS-1$
			try {
				OutputStream buildFile = new BufferedOutputStream(new FileOutputStream(buildProperty));
				try {
					sourceBuildProperties.store(buildFile, null);
				} finally {
					buildFile.close();
				}
			} catch (FileNotFoundException e) {
				String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			}
		}
		getSite(false).getRegistry().addBundle(sourcePluginDir);
		return result;
	}
	
	/**
	 * Method createSourcePlugin.
	 */
	private PluginEntry createSourcePlugin() throws CoreException {
		//Create an object representing the plugin
		PluginEntry result = new PluginEntry();
		String sourcePluginId = sourceFeature.getFeatureIdentifier();
		result.setPluginIdentifier(sourcePluginId);
		result.setPluginVersion(sourceFeature.getFeatureVersion());
		sourceFeature.addPluginEntryModel(result);
		// create the directory for the plugin
		IPath sourcePluginDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(result, false));
		File sourcePluginDir = sourcePluginDirURL.toFile();
		sourcePluginDir.mkdirs();

		// Create the plugin.xml
		StringBuffer buffer;
		Path templatePluginXML = new Path(TEMPLATE + "/21/plugin/" + DEFAULT_PLUGIN_FILENAME_DESCRIPTOR); //$NON-NLS-1$
		URL templatePluginURL = BundleHelper.getDefault().find(templatePluginXML);
		if (templatePluginURL == null) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, templatePluginXML), null);
			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
		try {
			buffer = readFile(templatePluginURL.openStream());
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, templatePluginURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		int beginId = scan(buffer, 0, REPLACED_PLUGIN_ID);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), result.getPluginIdentifier());
		//set the version number
		beginId = scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), result.getPluginVersion());
		try {
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourcePluginDirURL.append(DEFAULT_PLUGIN_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_writingFile, templatePluginURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplatePlugin", sourcePluginDir.getAbsolutePath()); //$NON-NLS-1$
		//	If a build.properties file already exist then we use it supposing it is correct.
		File buildProperty = sourcePluginDirURL.append(PROPERTIES_FILE).toFile();
		if (!buildProperty.exists()) {
			copiedFiles.add(DEFAULT_PLUGIN_FILENAME_DESCRIPTOR); //Because the plugin.xml is not copied, we need to add it to the file
			copiedFiles.add("src/**/*.zip"); //$NON-NLS-1$
			Properties sourceBuildProperties = new Properties();
			sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
			sourceBuildProperties.put(SOURCE_PLUGIN_ATTRIBUTE, "true"); //$NON-NLS-1$
			try {
				OutputStream buildFile = new BufferedOutputStream(new FileOutputStream(buildProperty));
				try {
					sourceBuildProperties.store(buildFile, null);
				} finally {
					buildFile.close();
				}
			} catch (FileNotFoundException e) {
				String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			}
		}
		getSite(false).getRegistry().addBundle(sourcePluginDir);
		return result;
	}

	private void create30SourceFragment(PluginEntry fragment, PluginEntry plugin) throws CoreException {
		// create the directory for the plugin
		Path sourceFragmentDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(fragment, false));
		File sourceFragmentDir = new File(sourceFragmentDirURL.toOSString());
		new File(sourceFragmentDir, "META-INF").mkdirs(); //$NON-NLS-1$
		try {
			// read the content of the template file
			Path fragmentPath = new Path(TEMPLATE + "/30/fragment/" + DEFAULT_BUNDLE_FILENAME_DESCRIPTOR);//$NON-NLS-1$
			URL templateLocation = BundleHelper.getDefault().find(fragmentPath);
			if (templateLocation == null) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, fragmentPath), null);
				BundleHelper.getDefault().getLog().log(status);
				return;
			}

			//Copy the fragment.xml
			try {
				InputStream fragmentXML = BundleHelper.getDefault().getBundle().getEntry(TEMPLATE + "/30/fragment/fragment.xml").openStream(); //$NON-NLS-1$
				Utils.transferStreams(fragmentXML, new FileOutputStream(sourceFragmentDirURL.append(DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR).toOSString()));
			} catch (IOException e1) {
				String message = NLS.bind(Messages.exception_readingFile, TEMPLATE + "/30/fragment/fragment.xml"); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e1));
			}
			
			StringBuffer buffer = readFile(templateLocation.openStream());
			//Set the Id of the fragment
			int beginId = scan(buffer, 0, REPLACED_FRAGMENT_ID);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_ID.length(), fragment.getPluginIdentifier());
			//		set the version number
			beginId = scan(buffer, beginId, REPLACED_FRAGMENT_VERSION);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_VERSION.length(), fragment.getPluginVersion());
			// Set the Id of the plugin for the fragment
			beginId = scan(buffer, beginId, REPLACED_PLUGIN_ID);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), plugin.getPluginIdentifier());
			//		set the version number of the plugin to which the fragment is attached to
			BundleDescription effectivePlugin = getSite(false).getRegistry().getResolvedBundle(plugin.getVersionedIdentifier().getIdentifier(), plugin.getPluginVersion());
			beginId = scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), effectivePlugin.getVersion().toString());
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourceFragmentDirURL.append(DEFAULT_BUNDLE_FILENAME_DESCRIPTOR).toOSString()));
			Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFragment", sourceFragmentDir.getAbsolutePath()); //$NON-NLS-1$
			File buildProperty = sourceFragmentDirURL.append(PROPERTIES_FILE).toFile();
			if (!buildProperty.exists()) { //If a build.properties file already exist  then we don't override it.
				copiedFiles.add(DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR); //Because the fragment.xml is not copied, we need to add it to the file
				copiedFiles.add("src/**"); //$NON-NLS-1$
				copiedFiles.add(DEFAULT_BUNDLE_FILENAME_DESCRIPTOR);
				Properties sourceBuildProperties = new Properties();
				sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
				sourceBuildProperties.put("sourcePlugin", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					OutputStream buildFile = new BufferedOutputStream(new FileOutputStream(buildProperty));
					try {
						sourceBuildProperties.store(buildFile, null);
					} finally {
						buildFile.close();
					}
				} catch (FileNotFoundException e) {
					String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				} catch (IOException e) {
					String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				}
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, sourceFragmentDir.getName());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, null));
		}
		getSite(false).getRegistry().addBundle(sourceFragmentDir);
	}
	
	private void createSourceFragment(PluginEntry fragment, PluginEntry plugin) throws CoreException {
		// create the directory for the plugin
		Path sourceFragmentDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(fragment, false));
		File sourceFragmentDir = new File(sourceFragmentDirURL.toOSString());
		sourceFragmentDir.mkdirs();
		try {
			// read the content of the template file
			Path fragmentPath = new Path(TEMPLATE + "/21/fragment/" + DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR);//$NON-NLS-1$
			URL templateLocation = BundleHelper.getDefault().find(fragmentPath);
			if (templateLocation == null) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, fragmentPath), null);
				BundleHelper.getDefault().getLog().log(status);
				return;
			}

			StringBuffer buffer = readFile(templateLocation.openStream());
			//Set the Id of the fragment
			int beginId = scan(buffer, 0, REPLACED_FRAGMENT_ID);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_ID.length(), fragment.getPluginIdentifier());
			//		set the version number
			beginId = scan(buffer, beginId, REPLACED_FRAGMENT_VERSION);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_VERSION.length(), fragment.getPluginVersion());
			// Set the Id of the plugin for the fragment
			beginId = scan(buffer, beginId, REPLACED_PLUGIN_ID);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), plugin.getPluginIdentifier());
			//		set the version number of the plugin to which the fragment is attached to
			beginId = scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), plugin.getPluginVersion());
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourceFragmentDirURL.append(DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR).toOSString()));
			Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFragment", sourceFragmentDir.getAbsolutePath()); //$NON-NLS-1$
			File buildProperty = sourceFragmentDirURL.append(PROPERTIES_FILE).toFile();
			if (!buildProperty.exists()) { //If a build.properties file already exist  then we don't override it.
				copiedFiles.add(DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR); //Because the fragment.xml is not copied, we need to add it to the file
				copiedFiles.add("src/**"); //$NON-NLS-1$
				Properties sourceBuildProperties = new Properties();
				sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
				sourceBuildProperties.put("sourcePlugin", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					OutputStream buildFile = new BufferedOutputStream(new FileOutputStream(buildProperty));
					try {
						sourceBuildProperties.store(buildFile, null);
					} finally {
						buildFile.close();
					}
				} catch (FileNotFoundException e) {
					String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				} catch (IOException e) {
					String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				}
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, sourceFragmentDir.getName());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, null));
		}
		getSite(false).getRegistry().addBundle(sourceFragmentDir);
	}

	public String getSourcePluginName(PluginEntry plugin, boolean versionSuffix) {
		return plugin.getPluginIdentifier() + (versionSuffix ? "_" + plugin.getPluginVersion() : ""); //$NON-NLS-1$	//$NON-NLS-2$
	}

	public void setFeatureRootLocation(String featureLocation) {
		this.featureRootLocation = featureLocation;
	}

	/**
	 * Method setSourceToGather.
	 * 
	 * @param sourceToGather
	 */
	public void setSourceToGather(SourceFeatureInformation sourceToGather) {
		this.sourceToGather = sourceToGather;
	}

	/**
	 * Sets the sourceFeatureGeneration.
	 * 
	 * @param sourceFeatureGeneration
	 *                   The sourceFeatureGeneration to set
	 */
	public void setSourceFeatureGeneration(boolean sourceFeatureGeneration) {
		this.sourceFeatureGeneration = sourceFeatureGeneration;
	}

	/**
	 * Sets the binaryFeatureGeneration.
	 * 
	 * @param binaryFeatureGeneration
	 *                   The binaryFeatureGeneration to set
	 */
	public void setBinaryFeatureGeneration(boolean binaryFeatureGeneration) {
		this.binaryFeature = binaryFeatureGeneration;
	}

	/**
	 * Sets the scriptGeneration.
	 * 
	 * @param scriptGeneration
	 *                   The scriptGeneration to set
	 */
	public void setScriptGeneration(boolean scriptGeneration) {
		this.scriptGeneration = scriptGeneration;
	}

	/**
	 * Returns the sourceFeatureGeneration.
	 * 
	 * @return boolean
	 */
	public boolean isSourceFeatureGeneration() {
		return sourceFeatureGeneration;
	}

	/**
	 * Sets whether or not to generate JNLP manifests
	 * 
	 * @param value whether or not to generate JNLP manifests
	 */
	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}

	/**
	 * Sets whether or not to sign any constructed jars.
	 * 
	 * @param value whether or not to sign any constructed JARs
	 */
	public void setSignJars(boolean value) {
		signJars = value;
	}
	
	/**
	 * Sets whether or not to generate the feature version suffix
	 * 
	 * @param value whether or not to generate the feature version suffix
	 */
	public void setGenerateVersionSuffix(boolean value) {
		generateVersionSuffix = value;
	}

	protected void collectElementToAssemble(IPluginEntry entryToCollect) throws CoreException {
		if (assemblyData == null)
			return;
		List correctConfigs = selectConfigs(entryToCollect);
		String versionRequested = entryToCollect.getVersionedIdentifier().getVersion().toString();
		BundleDescription effectivePlugin = null;
		effectivePlugin = getSite(false).getRegistry().getResolvedBundle(entryToCollect.getVersionedIdentifier().getIdentifier(), versionRequested);
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			assemblyData.addPlugin((Config) iter.next(), effectivePlugin);
		}
	}

	// Create a feature object representing a source feature based on the featureExample
	private Feature createSourceFeature(Feature featureExample) throws CoreException {
		BuildTimeFeature result = new BuildTimeFeature();
		result.setFeatureIdentifier(computeSourceFeatureName(featureExample, false));
		result.setFeatureVersion(featureExample.getVersionedIdentifier().getVersion().toString());
		result.setLabel(featureExample.getLabelNonLocalized());
		result.setProvider(featureExample.getProviderNonLocalized());
		result.setImageURLString(featureExample.getImageURLString());
		result.setInstallHandlerModel(featureExample.getInstallHandlerModel());
		result.setDescriptionModel(featureExample.getDescriptionModel());
		result.setCopyrightModel(featureExample.getCopyrightModel());
		result.setLicenseModel(featureExample.getLicenseModel());
		result.setUpdateSiteEntryModel(featureExample.getUpdateSiteEntryModel());
		URLEntryModel[] siteEntries = featureExample.getDiscoverySiteEntryModels();
		result.setDiscoverySiteEntryModels((siteEntries == null || siteEntries.length == 0) ? null : siteEntries);
		result.setOS(featureExample.getOS());
		result.setArch(featureExample.getOSArch());
		result.setWS(featureExample.getWS());
		int contextLength = featureExample instanceof BuildTimeFeature ? ((BuildTimeFeature) featureExample).getContextQualifierLength() : -1;
		result.setContextQualifierLength(contextLength);
		return result;
	}

	private void writeSourceFeature() throws CoreException {
		String sourceFeatureDir = workingDirectory + '/' + DEFAULT_FEATURE_LOCATION + '/' + sourceFeatureFullName;
		File sourceDir = new File(sourceFeatureDir);
		sourceDir.mkdirs();
		// write the source feature to the feature.xml
		File file = new File(sourceFeatureDir + '/' + DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
		try {
			SourceFeatureWriter writer = new SourceFeatureWriter(new FileOutputStream(file), sourceFeature, this);
			try {
				writer.printFeature();
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.error_creatingFeature, sourceFeature.getFeatureIdentifier());
			throw new CoreException(new Status(IStatus.OK, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFeature", sourceFeatureDir); //$NON-NLS-1$
		File buildProperty = new File(sourceFeatureDir + '/' + PROPERTIES_FILE);
		if (buildProperty.exists()) {//If a build.properties file already exist then we don't override it.
			getSite(false).addFeatureReferenceModel(sourceDir);
			return;
		}
		copiedFiles.add(DEFAULT_FEATURE_FILENAME_DESCRIPTOR); //Because the feature.xml is not copied, we need to add it to the file
		Properties sourceBuildProperties = new Properties();
		sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
		OutputStream output = null;
		try {
			output = new FileOutputStream(buildProperty);
			try {
				sourceBuildProperties.store(output, null);
			} finally {
				output.close();
			}
		} catch (FileNotFoundException e) {
			String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
		getSite(false).addFeatureReferenceModel(sourceDir);
	}
}
