/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *     G&H Softwareentwicklung GmbH - internationalization implementation (bug 150933)
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.IncludedFeatureReferenceModel;
import org.eclipse.update.core.model.URLEntryModel;
import org.osgi.framework.Version;

/**
 * Generates build.xml script for features.
 */
public class FeatureBuildScriptGenerator extends AbstractBuildScriptGenerator {
	private static final String COMMENT_START_TAG = "<!--"; //$NON-NLS-1$
	private static final String COMMENT_END_TAG = "-->"; //$NON-NLS-1$
	private static final String FEATURE_START_TAG = "<feature";//$NON-NLS-1$
	private static final String PLUGIN_START_TAG = "<plugin"; //$NON-NLS-1$
	private static final String FRAGMENT_START_TAG = "<fragment"; //$NON-NLS-1$
	private static final String VERSION = "version";//$NON-NLS-1$
	private static final String PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$

	// The 64 characters that are legal in a version qualifier, in lexicographical order.
	private static final String BASE_64_ENCODING = "-0123456789_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$

	private static final int QUALIFIER_SUFFIX_VERSION = 1;

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
	private String customFeatureCallbacks = null;
	private String customCallbacksBuildpath = null;
	private String customCallbacksFailOnError = null;
	private String customCallbacksInheritAll = null;
	private String product = null;
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
			if (selectConfigs(entry).size() == 0)
				continue;

			String versionRequested = identifier.getVersion().toString();
			model = getSite(false).getRegistry().getResolvedBundle(identifier.getIdentifier(), versionRequested);
			//we prefer a newly generated source plugin over a preexisting binary one. 
			if ((model == null || Utils.isBinary(model)) && getBuildProperties().containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + identifier.getIdentifier())) {
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
		Set entries = (Set) bundleProperties.get(PLUGIN_ENTRY);
		if (entries == null) {
			entries = new HashSet();
			bundleProperties.put(PLUGIN_ENTRY, entries);
		}
		entries.add(entry);
	}

	private void generateEmbeddedSource(String pluginId) throws CoreException {
		if (sourceFeatureGeneration)
			return;
		FeatureBuildScriptGenerator featureGenerator = new FeatureBuildScriptGenerator(Utils.getArrayFromString(getBuildProperties().getProperty(GENERATION_SOURCE_PLUGIN_PREFIX + pluginId))[0], null, assemblyData);
		featureGenerator.setGenerateIncludedFeatures(false);
		featureGenerator.setAnalyseChildren(analysePlugins);
		featureGenerator.setSourceFeatureId(pluginId);
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
		//Do not collect plug-ins for which we are not generating build.xml
		try {
			if (AbstractScriptGenerator.readProperties(model.getLocation(), PROPERTIES_FILE, IStatus.OK) == MissingProperties.getInstance())
				return;
		} catch (CoreException e) {
			return;
		}
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
		boolean custom = TRUE.equalsIgnoreCase((String) getBuildProperties().get(PROPERTY_CUSTOM));
		File customBuildFile = null;
		if (custom) {
			customBuildFile = new File(featureRootLocation, DEFAULT_BUILD_SCRIPT_FILENAME);
			if (!customBuildFile.exists()) {
				message = NLS.bind(Messages.error_missingCustomBuildFile, customBuildFile);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, null));
			}

			//turn off script generation so we don't overwrite the custom script
			scriptGeneration = false;
			
			/* need to do root files here because we won't be doing the gatherBinParts where it normally happens */
			List configs = getConfigInfos();
			for (Iterator iter = configs.iterator(); iter.hasNext();) {
				assemblyData.addRootFileProvider((Config) iter.next(), feature);
			}
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
			
		if (custom) {
			//Feature had a custom build script, we need to update the version in it.
			//Do it here after generateChildrenScripts since there may have been a suffix generated.
			try {
				updateVersion(customBuildFile, PROPERTY_FEATURE_VERSION_SUFFIX, feature.getVersionedIdentifier().getVersion().toString());
			} catch (IOException e) {
				message = NLS.bind(Messages.exception_writeScript, customBuildFile);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, e));
			}	
		}

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
			if (doSourceFeatureGeneration) {
				generator.setSourceFeatureId(featureId);
				generator.setExtraPlugins(Utils.getArrayFromString(getBuildProperties().getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId)));
			}
			generator.setPluginPath(pluginPath);
			generator.setBuildSiteFactory(siteFactory);
			generator.setDevEntries(devEntries);
			generator.setCompiledElements(getCompiledElements());
			generator.setSourceToGather(new SourceFeatureInformation());
			generator.setBuildingOSGi(isBuildingOSGi());
			generator.includePlatformIndependent(isPlatformIndependentIncluded());
			generator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
			generator.setGenerateVersionSuffix(generateVersionSuffix);
			try {
				generator.generate();
			} catch (CoreException exception) {
				absorbExceptionIfOptionalFeature(referencedFeatures[i], exception);
			}
		}
	}

	private void absorbExceptionIfOptionalFeature(IIncludedFeatureReference feature, CoreException toAbsorb) throws CoreException {
		if (toAbsorb.getStatus().getCode() != EXCEPTION_FEATURE_MISSING || (toAbsorb.getStatus().getCode() == EXCEPTION_FEATURE_MISSING && !feature.isOptional()))
			throw toAbsorb;
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
		script.printAntCallTask(TARGET_CHILDREN, true, params);
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, false, params); //$NON-NLS-1$
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, false, params); //$NON-NLS-1$
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
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
		String include = (String) getBuildProperties().get(PROPERTY_BIN_INCLUDES);
		String exclude = (String) getBuildProperties().get(PROPERTY_BIN_EXCLUDES);
		String root = Utils.getPropertyFormat(PROPERTY_FEATURE_BASE) + '/' + featureFolderName;

		script.println();
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
		if (include != null)
			script.printMkdirTask(root);

		Map callbackParams = null;
		if (customFeatureCallbacks != null) {
			callbackParams = new HashMap(1);
			callbackParams.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(Utils.getPropertyFormat(PROPERTY_FEATURE_BASE)).append(DEFAULT_PLUGIN_LOCATION).toString());
			callbackParams.put(PROPERTY_FEATURE_DIRECTORY, root);
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_GATHER_BIN_PARTS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, callbackParams, null);
		}

		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(Utils.getPropertyFormat(PROPERTY_FEATURE_BASE)).append(DEFAULT_PLUGIN_LOCATION).toString());
		script.printAntCallTask(TARGET_CHILDREN, true, params);

		if (include != null) {
			if (include != null || exclude != null) {
				FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
				script.printCopyTask(null, root, new FileSet[] {fileSet}, true, false);
			}
			// Generate the parameters for the Id Replacer.
			String featureVersionInfo = ""; //$NON-NLS-1$
			// Here we get all the included features (independently of the config being built so the version numbers in the feature can be replaced)
			IIncludedFeatureReference[] includedFeatures = feature.getRawIncludedFeatureReferences();
			for (int i = 0; i < includedFeatures.length; i++) {
				String versionRequested = includedFeatures[i].getVersionedIdentifier().getVersion().toString();
				IFeature includedFeature = null;
				try {
					includedFeature = getSite(false).findFeature(includedFeatures[i].getVersionedIdentifier().getIdentifier(), versionRequested, true);
				} catch (CoreException e) {
					absorbExceptionIfOptionalFeature(includedFeatures[i], e);
					continue;
				}
				VersionedIdentifier includedFeatureVersionId = includedFeature.getVersionedIdentifier();
				if (needsReplacement(versionRequested))
					featureVersionInfo += (includedFeatureVersionId.getIdentifier() + ':' + extract3Segments(versionRequested) + ',' + includedFeatureVersionId.getVersion().toString() + ',');
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
				if (model != null) {
					if (needsReplacement(versionRequested))
						pluginVersionInfo += (entryIdentifier + ':' + extract3Segments(versionRequested) + ',' + model.getVersion() + ',');
				}
			}
			script.println("<eclipse.idReplacer featureFilePath=\"" + AntScript.getEscaped(root) + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR + "\"  selfVersion=\"" + feature.getVersionedIdentifier().getVersion() + "\" featureIds=\"" + featureVersionInfo + "\" pluginIds=\"" + pluginVersionInfo + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		generateRootFilesAndPermissionsCalls();
		if (customFeatureCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_GATHER_BIN_PARTS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, callbackParams, null);
		}
		script.printTargetEnd();
		generateRootFilesAndPermissions();
	}

	private boolean needsReplacement(String s) {
		if (s.equalsIgnoreCase(GENERIC_VERSION_NUMBER) || s.endsWith(PROPERTY_QUALIFIER))
			return true;
		return false;
	}

	private Version extract3Segments(String s) {
		Version tmp = new Version(s);
		return new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
	}

	/**
	 *  
	 */
	private void generateRootFilesAndPermissionsCalls() {
		script.printAntCallTask(TARGET_ROOTFILES_PREFIX + Utils.getPropertyFormat(PROPERTY_OS) + '_' + Utils.getPropertyFormat(PROPERTY_WS) + '_' + Utils.getPropertyFormat(PROPERTY_ARCH), true, null);
	}

	/**
	 *  
	 */
	private void generateRootFilesAndPermissions() throws CoreException {
		if (product != null && !havePDEUIState()) {
			ProductGenerator generator = new ProductGenerator();
			generator.setProduct(product);
			generator.setBuildSiteFactory(siteFactory);
			generator.setBuildProperties(getBuildProperties());
			generator.setRoot(featureRootLocation);
			generator.setWorkingDirectory(getWorkingDirectory());
			try {
				generator.generate();
			} catch (CoreException e) {
				//problem with the .product file
				//TODO Log warning/error
			}
		}
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();) {
			Config aConfig = (Config) iter.next();
			script.printTargetDeclaration(TARGET_ROOTFILES_PREFIX + aConfig.toString("_"), null, null, null, null); //$NON-NLS-1$
			generateCopyRootFiles(aConfig);
			Utils.generatePermissions(getBuildProperties(), aConfig, PROPERTY_FEATURE_BASE, script);
			script.printTargetEnd();
		}
		script.printTargetDeclaration(TARGET_ROOTFILES_PREFIX + "group_group_group", null, null, null, null); //$NON-NLS-1$
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();) {
			Config aConfig = (Config) iter.next();
			script.printAntCallTask(TARGET_ROOTFILES_PREFIX + aConfig.toString("_"), true, null);//.getPropertyFormat(PROPERTY_OS) + '_' + Utils.getPropertyFormat(PROPERTY_WS) + '_' + Utils.getPropertyFormat(PROPERTY_ARCH))
		}
		script.printTargetEnd();
	}

	private void generateCopyRootFiles(Config aConfig) throws CoreException {
		Properties properties = getBuildProperties();
		Map foldersToCopy = new HashMap(2);

		/* normal root files */
		String baseList = getBuildProperties().getProperty(ROOT, ""); //$NON-NLS-1$
		String fileList = getBuildProperties().getProperty(ROOT_PREFIX + aConfig.toString("."), ""); //$NON-NLS-1$ //$NON-NLS-2$
		fileList = (fileList.length() == 0 ? "" : fileList + ',') + baseList; //$NON-NLS-1$
		if (fileList.length() > 0)
			foldersToCopy.put("", fileList); //$NON-NLS-1$

		/* root files going to subfolders */
		String configPrefix = ROOT_PREFIX + aConfig.toString(".") + FOLDER_INFIX; //$NON-NLS-1$
		for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String folder = null;
			if (key.startsWith(ROOT_FOLDER_PREFIX)) {
				folder = key.substring(ROOT_FOLDER_PREFIX.length(), key.length());
			} else if (key.startsWith(configPrefix)) {
				folder = key.substring(configPrefix.length(), key.length());
			}
			if (folder != null) {
				String value = properties.getProperty(key);
				if (foldersToCopy.containsKey(folder)) {
					String set = (String) foldersToCopy.get(folder);
					set += "," + value; //$NON-NLS-1$
					foldersToCopy.put(folder, set);
				} else {
					foldersToCopy.put(folder, value);
				}
			}
		}

		if (foldersToCopy.isEmpty())
			return;

		String configName = aConfig.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
		String shouldOverwrite = properties.getProperty(PROPERTY_OVERWRITE_ROOTFILES, "true"); //$NON-NLS-1$
		boolean overwrite = Boolean.valueOf(shouldOverwrite).booleanValue();

		assemblyData.addRootFileProvider(aConfig, feature);

		Object[] folders = foldersToCopy.keySet().toArray();
		for (int i = 0; i < folders.length; i++) {
			String folder = (String) folders[i];
			fileList = (String) foldersToCopy.get(folder);
			String[] files = Utils.getArrayFromString(fileList, ","); //$NON-NLS-1$
			FileSet[] fileSet = new FileSet[files.length];
			for (int j = 0; j < files.length; j++) {
				String file = files[j];
				String fromDir = Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/';
				if (file.startsWith("absolute:")) { //$NON-NLS-1$
					file = file.substring(9);
					fromDir = ""; //$NON-NLS-1$
				}
				if (file.startsWith("file:")) { //$NON-NLS-1$
					IPath target = new Path(file.substring(5));
					fileSet[j] = new FileSet(fromDir + target.removeLastSegments(1), null, target.lastSegment(), null, null, null, null);
				} else {
					fileSet[j] = new FileSet(fromDir + file, null, "**", null, null, null, null); //$NON-NLS-1$
				}
			}
			script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_FEATURE_BASE) + '/' + configName + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + folder);
			script.printCopyTask(null, Utils.getPropertyFormat(PROPERTY_FEATURE_BASE) + '/' + configName + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + folder, fileSet, true, overwrite);
		}
	}

	/**
	 * Add the <code>build.update.jar</code> target to the given script.
	 */
	private void generateBuildUpdateJarTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_buildUpdateJar, featureIdentifier));
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
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
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, false, params);
		String jar = Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".jar"; //$NON-NLS-1$
		script.printJarTask(jar, featureTempFolder + '/' + featureFolderName, null);
		script.printDeleteTask(featureTempFolder, null, null);
		if (generateJnlp)
			script.println("<eclipse.jnlpGenerator feature=\"" + AntScript.getEscaped(jar) + "\"  codebase=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_JNLP_CODEBASE) + "\" j2se=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_JNLP_J2SE) + "\" locale=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_JNLP_LOCALE) + "\" generateOfflineAllowed=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_GENOFFLINE) +  "\" configInfo=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_CONFIGS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (signJars) {
			if (generateJnlp) {
				script.printProperty(PROPERTY_UNSIGN, "true");  //$NON-NLS-1$
			}
			script.println("<eclipse.jarProcessor sign=\"" + Utils.getPropertyFormat(PROPERTY_SIGN) + "\" pack=\"" + Utils.getPropertyFormat(PROPERTY_PACK)+ "\" unsign=\"" + Utils.getPropertyFormat(PROPERTY_UNSIGN) +  "\" jar=\"" + AntScript.getEscaped(jar) + "\" alias=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS) + "\" keystore=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE) + "\" storepass=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}
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
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, true, params);
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
			IPluginEntry[] entries = Utils.getPluginEntry(feature, current.getSymbolicName(), false); //TODO This can be improved to use the value from the user object in the bundleDescription
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
					if (features[i].isOptional())
						continue;
					String message = NLS.bind(Messages.exception_missingFeature, featureId + ' ' + versionId);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
				}
				if (includedFeature instanceof BuildTimeFeature) {
					if (((BuildTimeFeature) includedFeature).isBinary())
						continue;
				}

				String includedFeatureDirectory = includedFeature.getURL().getPath();
				int j = includedFeatureDirectory.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
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
		if (customFeatureCallbacks != null) {
			script.printAvailableTask(PROPERTY_CUSTOM_BUILD_CALLBACKS, customCallbacksBuildpath + '/' + customFeatureCallbacks, customFeatureCallbacks);
		}
		script.printTargetEnd();
	}

	/**
	 * @throws CoreException
	 */
	private void generateChildrenScripts() throws CoreException {
		List plugins = computeElements();
		String suffix = generateFeatureVersionSuffix((BuildTimeFeature) feature);
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

	// Integer to character conversion in our base-64 encoding scheme.  If the
	// input is out of range, an illegal character will be returned.
	private static char base64Character(int number) {
		if (number < 0 || number > 63) {
			return ' ';
		}
		return BASE_64_ENCODING.charAt(number);
	}

	// Encode a non-negative number as a variable length string, with the
	// property that if X > Y then the encoding of X is lexicographically
	// greater than the enocding of Y.  This is accomplished by encoding the
	// length of the string at the beginning of the string.  The string is a
	// series of base 64 (6-bit) characters.  The first three bits of the first
	// character indicate the number of additional characters in the string.
	// The last three bits of the first character and all of the rest of the
	// characters encode the actual value of the number.  Examples:
	//     0 --> 000 000 --> "-"
	//     7 --> 000 111 --> "6"
	//     8 --> 001 000 001000 --> "77"
	//    63 --> 001 000 111111 --> "7z"
	//    64 --> 001 001 000000 --> "8-"
	//   511 --> 001 111 111111 --> "Dz"
	//   512 --> 010 000 001000 000000 --> "E7-"
	//   2^32 - 1 --> 101 011 111111 ... 111111 --> "fzzzzz"
	//   2^45 - 1 --> 111 111 111111 ... 111111 --> "zzzzzzzz"
	// (There are some wasted values in this encoding.  For example,
	// "7-" through "76" and "E--" through "E6z" are not legal encodings of
	// any number.  But the benefit of filling in those wasted ranges would not
	// be worth the added complexity.)
	private static String lengthPrefixBase64(long number) {
		int length = 7;
		for (int i = 0; i < 7; ++i) {
			if (number < (1L << ((i * 6) + 3))) {
				length = i;
				break;
			}
		}
		StringBuffer result = new StringBuffer(length + 1);
		result.append(base64Character((length << 3) + (int) ((number >> (6 * length)) & 0x7)));
		while (--length >= 0) {
			result.append(base64Character((int) ((number >> (6 * length)) & 0x3f)));
		}
		return result.toString();
	}

	private static int charValue(char c) {
		int index = BASE_64_ENCODING.indexOf(c);
		// The "+ 1" is very intentional.  For a blank (or anything else that
		// is not a legal character), we want to return 0.  For legal
		// characters, we want to return one greater than their position, so
		// that a blank is correctly distinguished from '-'.
		return index + 1;
	}

	private static void appendEncodedCharacter(StringBuffer buffer, int c) {
		while (c > 62) {
			buffer.append('z');
			c -= 63;
		}
		buffer.append(base64Character(c));
	}

	private static int getIntProperty(String property, int defaultValue) {
		int result = defaultValue;
		if (property != null) {
			try {
				result = Integer.parseInt(property);
				if (result < 1) {
					// It has to be a positive integer.  Use the default.
					result = defaultValue;
				}
			} catch (NumberFormatException e) {
				// Leave as default value
			}
		}
		return result;
	}

	private String generateFeatureVersionSuffix(BuildTimeFeature buildFeature) throws CoreException {
		if (!generateVersionSuffix || buildFeature.getContextQualifierLength() == -1) {
			return null; // do nothing
		}

		Properties properties = getBuildProperties();
		int significantDigits = getIntProperty((String) properties.get(PROPERTY_SIGNIFICANT_VERSION_DIGITS), -1);
		if (significantDigits == -1)
			significantDigits = getIntProperty(AbstractScriptGenerator.getImmutableAntProperty(PROPERTY_SIGNIFICANT_VERSION_DIGITS), Integer.MAX_VALUE);
		int maxGeneratedLength = getIntProperty((String) properties.get(PROPERTY_GENERATED_VERSION_LENGTH), -1);
		if (maxGeneratedLength == -1)
			maxGeneratedLength = getIntProperty(AbstractScriptGenerator.getImmutableAntProperty(PROPERTY_GENERATED_VERSION_LENGTH), 28);

		long majorSum = 0L;
		long minorSum = 0L;
		long serviceSum = 0L;

		// Include the version of this algorithm as part of the suffix, so that
		// we have a way to make sure all suffixes increase when the algorithm
		// changes.
		majorSum += QUALIFIER_SUFFIX_VERSION;

		IIncludedFeatureReference[] referencedFeatures = buildFeature.getIncludedFeatureReferences();
		IPluginEntry[] pluginList = buildFeature.getRawPluginEntries();
		int numElements = pluginList.length + referencedFeatures.length;
		if (numElements == 0) {
			// Empty feature.
			return null;
		}
		String[] qualifiers = new String[numElements];
		int idx = -1;

		// Loop through the included features, adding the version number parts
		// to the running totals and storing the qualifier suffixes.
		for (int i = 0; i < referencedFeatures.length; i++) {
			BuildTimeFeature refFeature = (BuildTimeFeature) getSite(false).findFeature(referencedFeatures[i].getVersionedIdentifier().getIdentifier(), null, false);
			if (refFeature == null) {
				qualifiers[++idx] = ""; //$NON-NLS-1$
				continue;
			}
			PluginVersionIdentifier version = refFeature.getVersionedIdentifier().getVersion();
			majorSum += version.getMajorComponent();
			minorSum += version.getMinorComponent();
			serviceSum += version.getServiceComponent();
			int contextLength = refFeature.getContextQualifierLength();
			++contextLength; //account for the '-' separating the context qualifier and suffix
			String qualifier = version.getQualifierComponent();
			// The entire qualifier of the nested feature is often too long to
			// include in the suffix computation for the containing feature,
			// and using it would result in extremely long qualifiers for
			// umbrella features.  So instead we want to use just the suffix
			// part of the qualifier, or just the context part (if there is no
			// suffix part).  See bug #162022.
			if (qualifier.length() > contextLength) {
				// Use the suffix part
				qualifiers[++idx] = qualifier.substring(contextLength);
			} else {
				// Use the context part
				qualifiers[++idx] = qualifier;
			}
		}

		// Loop through the included plug-ins and fragments, adding the version
		// number parts to the running totals and storing the qualifiers.
		
		for (int i = 0; i < pluginList.length; i++) {
			IPluginEntry entry = pluginList[i];
			VersionedIdentifier identifier = entry.getVersionedIdentifier();
			String versionRequested = identifier.getVersion().toString();
			BundleDescription model = getSite(false).getRegistry().getBundle(identifier.getIdentifier(), versionRequested, false);
			Version version = null;
			if (model != null) {
				version = model.getVersion();
			} else {
				if (versionRequested.endsWith(PROPERTY_QUALIFIER)) {
					int resultingLength = versionRequested.length() - PROPERTY_QUALIFIER.length();
					if (versionRequested.charAt(resultingLength - 1) == '.')
						resultingLength--;
					versionRequested = versionRequested.substring(0, resultingLength);
				}
				version = new Version(versionRequested);
			}
			
			majorSum += version.getMajor();
			minorSum += version.getMinor();
			serviceSum += version.getMicro();
			qualifiers[++idx] = version.getQualifier();
		}

		// Limit the qualifiers to the specified number of significant digits,
		// and figure out what the longest qualifier is.
		int longestQualifier = 0;
		for (int i = 0; i < numElements; ++i) {
			if (qualifiers[i].length() > significantDigits) {
				qualifiers[i] = qualifiers[i].substring(0, significantDigits);
			}
			if (qualifiers[i].length() > longestQualifier) {
				longestQualifier = qualifiers[i].length();
			}
		}

		StringBuffer result = new StringBuffer();

		// Encode the sums of the first three parts of the version numbers.
		result.append(lengthPrefixBase64(majorSum));
		result.append(lengthPrefixBase64(minorSum));
		result.append(lengthPrefixBase64(serviceSum));

		if (longestQualifier > 0) {
			// Calculate the sum at each position of the qualifiers.
			int[] qualifierSums = new int[longestQualifier];
			for (int i = 0; i < numElements; ++i) {
				for (int j = 0; j < qualifiers[i].length(); ++j) {
					qualifierSums[j] += charValue(qualifiers[i].charAt(j));
				}
			}
			// Normalize the sums to be base 65.
			int carry = 0;
			for (int k = longestQualifier - 1; k >= 1; --k) {
				qualifierSums[k] += carry;
				carry = qualifierSums[k] / 65;
				qualifierSums[k] = qualifierSums[k] % 65;
			}
			qualifierSums[0] += carry;
			
			// Always use one character for overflow.  This will be handled
			// correctly even when the overflow character itself overflows.
			result.append(lengthPrefixBase64(qualifierSums[0]));
			for (int m = 1; m < longestQualifier; ++m) {
				appendEncodedCharacter(result, qualifierSums[m]);
			}
		}
		// It is safe to strip any '-' characters from the end of the suffix.
		// (This won't happen very often, but it will save us a character or
		// two when it does.)
		while (result.length() > 0 && result.charAt(result.length() - 1) == '-') {
			result.deleteCharAt(result.length() - 1);
		}

		// If the resulting suffix is too long, shorten it to the designed length.
		if (maxGeneratedLength > result.length()) {
			return result.toString();
		}
		return result.substring(0, maxGeneratedLength);
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

			//Get the corresponding plug-in entries (from a feature object) associated with the model
			//and generate the script if one the configuration is being built. The generated scripts
			//are configuration agnostic so we only generate once.
			Set matchingEntries = (Set) ((Properties) model.getUserObject()).get(PLUGIN_ENTRY);
			if (matchingEntries.isEmpty())
				return;

			Iterator entryIter = matchingEntries.iterator();
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
			int i = featureRootLocation.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (i != -1)
				featureRootLocation = featureRootLocation.substring(0, i);
		}
		initializeFeatureNames();

		if (feature instanceof BuildTimeFeature) {
			if (getBuildProperties() == MissingProperties.getInstance() || AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE)) {
				BuildTimeFeature buildFeature = (BuildTimeFeature) feature;
				scriptGeneration = false;
				buildFeature.setBinary(true);
			}
		}

		Properties properties = getBuildProperties();
		customFeatureCallbacks = properties.getProperty(PROPERTY_CUSTOM_BUILD_CALLBACKS);
		if (TRUE.equalsIgnoreCase(customFeatureCallbacks))
			customFeatureCallbacks = DEFAULT_CUSTOM_BUILD_CALLBACKS_FILE;
		else if (FALSE.equalsIgnoreCase(customFeatureCallbacks))
			customFeatureCallbacks = null;
		customCallbacksBuildpath = properties.getProperty(PROPERTY_CUSTOM_CALLBACKS_BUILDPATH, "."); //$NON-NLS-1$
		customCallbacksFailOnError = properties.getProperty(PROPERTY_CUSTOM_CALLBACKS_FAILONERROR, FALSE);
		customCallbacksInheritAll = properties.getProperty(PROPERTY_CUSTOM_CALLBACKS_INHERITALL);
	}

	private void initializeFeatureNames() throws CoreException {
		featureFullName = getNormalizedName(feature);
		featureFolderName = DEFAULT_FEATURE_LOCATION + '/' + featureFullName;
		sourceFeatureFullName = computeSourceFeatureName(feature, false);
		sourceFeatureFullNameVersionned = computeSourceFeatureName(feature, true);
		featureTempFolder = Utils.getPropertyFormat(PROPERTY_FEATURE_TEMP_FOLDER);
	}

	public void setSourceFeatureId(String id) {
		sourceFeatureFullName = id;
	}

	private String computeSourceFeatureName(IFeature featureForName, boolean withNumber) throws CoreException {
		String sourceFeatureName = getBuildProperties().getProperty(PROPERTY_SOURCE_FEATURE_NAME);
		if (sourceFeatureName == null)
			sourceFeatureName = sourceFeatureFullName;
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, null);
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
		script.printTargetEnd();
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
		params.clear();
		params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
		script.printTargetEnd();
	}

	public void setGenerateIncludedFeatures(boolean recursiveGeneration) {
		analyseIncludedFeatures = recursiveGeneration;
	}

	protected void collectElementToAssemble(IFeature featureToCollect) throws CoreException {
		if (assemblyData == null)
			return;

		/* collect binary features */
		if (featureToCollect instanceof BuildTimeFeature && ((BuildTimeFeature) featureToCollect).isBinary()) {
			basicCollectElementToAssemble(featureToCollect);
			return;
		}

		// don't collect if bin.includes is empty, or we are generating source;
		if (getBuildProperties().get(PROPERTY_BIN_INCLUDES) == null || sourceFeatureGeneration)
			return;

		basicCollectElementToAssemble(featureToCollect);
	}

	private void basicCollectElementToAssemble(IFeature featureToCollect) {
		if (assemblyData == null)
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
		associateExtraPluginsAndFeatures();
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
		FeatureBuildScriptGenerator sourceScriptGenerator = new FeatureBuildScriptGenerator(sourceFeatureFullName, sourceFeature.getFeatureVersion(), assemblyData);
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
	private void associateExtraPluginsAndFeatures() throws CoreException {
		for (int i = 1; i < extraPlugins.length; i++) {
			BundleDescription model;
			// see if we have a plug-in or a fragment
			if (extraPlugins[i].startsWith("feature@")) { //$NON-NLS-1$
				String id = extraPlugins[i].substring(8);
				IncludedFeatureReference include = new IncludedFeatureReference();
				include.setFeatureIdentifier(id);
				include.setFeatureVersion(GENERIC_VERSION_NUMBER);
				sourceFeature.addIncludedFeatureReferenceModel(include);
			} else {
				Object[] items = Utils.parseExtraBundlesString(extraPlugins[i], true);
				model = getSite(false).getRegistry().getResolvedBundle((String) items[0], ((Version) items[1]).toString());
				if (model == null) {
					String message = NLS.bind(Messages.exception_missingPlugin, extraPlugins[i]);
					BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, extraPlugins[i], EXCEPTION_PLUGIN_MISSING, message, null));
					continue;
				}
				PluginEntry entry = new PluginEntry();
				entry.setPluginIdentifier(model.getSymbolicName());
				entry.setPluginVersion(model.getVersion().toString());
				entry.setUnpack(((Boolean) items[2]).booleanValue());
				sourceFeature.addPluginEntryModel(entry);
			}
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
		IPath sourcePluginDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(result, false));
		File sourcePluginDir = sourcePluginDirURL.toFile();
		new File(sourcePluginDir, "META-INF").mkdirs(); //$NON-NLS-1$

		// Create the MANIFEST.MF
		StringBuffer buffer;
		Path templateManifest = new Path(TEMPLATE + "/30/plugin/" + Constants.BUNDLE_FILENAME_DESCRIPTOR); //$NON-NLS-1$
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
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourcePluginDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_writingFile, templateManifestURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}

		//Copy the plugin.xml
		try {
			InputStream pluginXML = BundleHelper.getDefault().getBundle().getEntry(TEMPLATE + "/30/plugin/plugin.xml").openStream(); //$NON-NLS-1$
			Utils.transferStreams(pluginXML, new FileOutputStream(sourcePluginDirURL.append(Constants.PLUGIN_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, TEMPLATE + "/30/plugin/plugin.xml"); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e1));
		}

		//Copy the other files
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplatePlugin", sourcePluginDir.getAbsolutePath()); //$NON-NLS-1$
		if (copiedFiles.contains(Constants.BUNDLE_FILENAME_DESCRIPTOR)) {
			//make sure the manifest.mf has the version we want
			replaceManifestValue(sourcePluginDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString(), org.osgi.framework.Constants.BUNDLE_VERSION, result.getPluginVersion()); //$NON-NLS-1$
		}

		//	If a build.properties file already exist then we use it supposing it is correct.
		File buildProperty = sourcePluginDirURL.append(PROPERTIES_FILE).toFile();
		if (!buildProperty.exists()) {
			copiedFiles.add(Constants.PLUGIN_FILENAME_DESCRIPTOR); //Because the plugin.xml is not copied, we need to add it to the file
			copiedFiles.add("src/**/*.zip"); //$NON-NLS-1$
			copiedFiles.add(Constants.BUNDLE_FILENAME_DESCRIPTOR);//Because the manifest.mf is not copied, we need to add it to the file
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

		PDEState state = getSite(false).getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(result.getPluginIdentifier());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourcePluginDir);

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
		Path templatePluginXML = new Path(TEMPLATE + "/21/plugin/" + Constants.PLUGIN_FILENAME_DESCRIPTOR); //$NON-NLS-1$
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
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourcePluginDirURL.append(Constants.PLUGIN_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_writingFile, templatePluginURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplatePlugin", sourcePluginDir.getAbsolutePath()); //$NON-NLS-1$
		if (copiedFiles.contains(Constants.PLUGIN_FILENAME_DESCRIPTOR)) {
			replaceXMLAttribute(sourcePluginDirURL.append(Constants.PLUGIN_FILENAME_DESCRIPTOR).toOSString(), PLUGIN_START_TAG, VERSION, result.getPluginVersion());
		}
		//	If a build.properties file already exist then we use it supposing it is correct.
		File buildProperty = sourcePluginDirURL.append(PROPERTIES_FILE).toFile();
		if (!buildProperty.exists()) {
			copiedFiles.add(Constants.PLUGIN_FILENAME_DESCRIPTOR); //Because the plugin.xml is not copied, we need to add it to the file
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
		PDEState state = getSite(false).getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(result.getPluginIdentifier());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourcePluginDir);
		return result;
	}

	private void create30SourceFragment(PluginEntry fragment, PluginEntry plugin) throws CoreException {
		// create the directory for the plugin
		Path sourceFragmentDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(fragment, false));
		File sourceFragmentDir = new File(sourceFragmentDirURL.toOSString());
		new File(sourceFragmentDir, "META-INF").mkdirs(); //$NON-NLS-1$
		try {
			// read the content of the template file
			Path fragmentPath = new Path(TEMPLATE + "/30/fragment/" + Constants.BUNDLE_FILENAME_DESCRIPTOR);//$NON-NLS-1$
			URL templateLocation = BundleHelper.getDefault().find(fragmentPath);
			if (templateLocation == null) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, fragmentPath), null);
				BundleHelper.getDefault().getLog().log(status);
				return;
			}

			//Copy the fragment.xml
			try {
				InputStream fragmentXML = BundleHelper.getDefault().getBundle().getEntry(TEMPLATE + "/30/fragment/fragment.xml").openStream(); //$NON-NLS-1$
				Utils.transferStreams(fragmentXML, new FileOutputStream(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString()));
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
			// Set the platform filter of the fragment
			beginId = scan(buffer, beginId, REPLACED_PLATFORM_FILTER);
			buffer.replace(beginId, beginId + REPLACED_PLATFORM_FILTER.length(), "(& (osgi.ws=" + fragment.getWS() + ") (osgi.os=" + fragment.getOS() +  ") (osgi.arch=" + fragment.getOSArch() + "))");
			
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourceFragmentDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString()));
			Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFragment", sourceFragmentDir.getAbsolutePath()); //$NON-NLS-1$
			if (copiedFiles.contains(Constants.BUNDLE_FILENAME_DESCRIPTOR)) {
				//make sure the manifest.mf has the versions we want
				replaceManifestValue(sourceFragmentDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString(), org.osgi.framework.Constants.BUNDLE_VERSION, fragment.getPluginVersion());
				String host = plugin.getPluginIdentifier() + ';' + org.osgi.framework.Constants.BUNDLE_VERSION + '=' + effectivePlugin.getVersion().toString();
				replaceManifestValue(sourceFragmentDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString(), org.osgi.framework.Constants.FRAGMENT_HOST, host);
			}
			File buildProperty = sourceFragmentDirURL.append(PROPERTIES_FILE).toFile();
			if (!buildProperty.exists()) { //If a build.properties file already exist  then we don't override it.
				copiedFiles.add(Constants.FRAGMENT_FILENAME_DESCRIPTOR); //Because the fragment.xml is not copied, we need to add it to the file
				copiedFiles.add("src/**"); //$NON-NLS-1$
				copiedFiles.add(Constants.BUNDLE_FILENAME_DESCRIPTOR);
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
		PDEState state = getSite(false).getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(fragment.getPluginIdentifier());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourceFragmentDir);
	}

	private void createSourceFragment(PluginEntry fragment, PluginEntry plugin) throws CoreException {
		// create the directory for the plugin
		Path sourceFragmentDirURL = new Path(workingDirectory + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(fragment, false));
		File sourceFragmentDir = new File(sourceFragmentDirURL.toOSString());
		sourceFragmentDir.mkdirs();
		try {
			// read the content of the template file
			Path fragmentPath = new Path(TEMPLATE + "/21/fragment/" + Constants.FRAGMENT_FILENAME_DESCRIPTOR);//$NON-NLS-1$
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
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString()));
			Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFragment", sourceFragmentDir.getAbsolutePath()); //$NON-NLS-1$
			if (copiedFiles.contains(Constants.FRAGMENT_FILENAME_DESCRIPTOR)) {
				replaceXMLAttribute(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString(), FRAGMENT_START_TAG, VERSION, fragment.getPluginVersion());
				replaceXMLAttribute(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString(), FRAGMENT_START_TAG, PLUGIN_VERSION, plugin.getPluginVersion());
			}
			File buildProperty = sourceFragmentDirURL.append(PROPERTIES_FILE).toFile();
			if (!buildProperty.exists()) { //If a build.properties file already exist  then we don't override it.
				copiedFiles.add(Constants.FRAGMENT_FILENAME_DESCRIPTOR); //Because the fragment.xml is not copied, we need to add it to the file
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
		PDEState state = getSite(false).getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(fragment.getPluginIdentifier());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourceFragmentDir);
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

	/**
	 * Set the location of the .product file
	 * @param product the location of the .product file
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	protected void collectElementToAssemble(IPluginEntry entryToCollect) throws CoreException {
		if (assemblyData == null || sourceFeatureGeneration)
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
		File file = new File(sourceFeatureDir + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR);
		try {
			SourceFeatureWriter writer = new SourceFeatureWriter(new BufferedOutputStream(new FileOutputStream(file)), sourceFeature, this);
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
		if (copiedFiles.contains(Constants.FEATURE_FILENAME_DESCRIPTOR)) {
			//we overwrote our feature.xml with a template, replace the version
			replaceXMLAttribute(sourceFeatureDir + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR, FEATURE_START_TAG, VERSION, sourceFeature.getFeatureVersion());
		}
		File buildProperty = new File(sourceFeatureDir + '/' + PROPERTIES_FILE);
		if (buildProperty.exists()) {//If a build.properties file already exist then we don't override it.
			getSite(false).addFeatureReferenceModel(sourceDir);
			return;
		}
		copiedFiles.add(Constants.FEATURE_FILENAME_DESCRIPTOR); //Because the feature.xml is not copied, we need to add it to the file
		Properties sourceBuildProperties = new Properties();
		sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
		OutputStream output = null;
		try {
			output = new BufferedOutputStream(new FileOutputStream(buildProperty));
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

	private void replaceManifestValue(String location, String attribute, String newVersion) {
		Manifest manifest = null;
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(location));
			try {
				manifest = new Manifest(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			return;
		}

		manifest.getMainAttributes().put(new Attributes.Name(attribute), newVersion);

		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(location));
			try {
				manifest.write(os);
			} finally {
				os.close();
			}
		} catch (IOException e1) {
			//ignore
		}
	}

	private void replaceXMLAttribute(String location, String tag, String attr, String newValue) {
		File featureFile = new File(location);
		if (!featureFile.exists())
			return;

		StringBuffer buffer = null;
		try {
			buffer = readFile(featureFile);
		} catch (IOException e) {
			return;
		}

		int startComment = scan(buffer, 0, COMMENT_START_TAG);
		int endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
		int startTag = scan(buffer, 0, tag);
		while (startComment != -1 && startTag > startComment && startTag < endComment) {
			startTag = scan(buffer, endComment, tag);
			startComment = scan(buffer, endComment, COMMENT_START_TAG);
			endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
		}
		if (startTag == -1)
			return;
		int endTag = scan(buffer, startTag, ">"); //$NON-NLS-1$
		boolean attrFound = false;
		while (!attrFound) {
			int startAttributeWord = scan(buffer, startTag, attr);
			if (startAttributeWord == -1 || startAttributeWord > endTag)
				return;
			if (!Character.isWhitespace(buffer.charAt(startAttributeWord - 1))) {
				startTag = startAttributeWord + attr.length();
				continue;
			}
			//Verify that the word found is the actual attribute
			int endAttributeWord = startAttributeWord + attr.length();
			while (Character.isWhitespace(buffer.charAt(endAttributeWord)) && endAttributeWord < endTag) {
				endAttributeWord++;
			}
			if (endAttributeWord > endTag) { //attribute  has not been found
				return;
			}

			if (buffer.charAt(endAttributeWord) != '=') {
				startTag = endAttributeWord;
				continue;
			}

			int startVersionId = scan(buffer, startAttributeWord + 1, "\""); //$NON-NLS-1$
			int endVersionId = scan(buffer, startVersionId + 1, "\""); //$NON-NLS-1$
			buffer.replace(startVersionId + 1, endVersionId, newValue);
			attrFound = true;
		}
		if (attrFound) {
			try {
				Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(featureFile));
			} catch (IOException e) {
				//ignore
			}
		}
	}

}