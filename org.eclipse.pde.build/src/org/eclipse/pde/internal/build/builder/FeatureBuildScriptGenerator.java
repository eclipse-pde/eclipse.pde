package org.eclipse.pde.internal.build.builder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;
import org.osgi.framework.Version;

public class FeatureBuildScriptGenerator extends AbstractScriptGenerator {

	protected BuildTimeFeature feature;
	protected Properties buildProperties;
	//protected boolean analyseIncludedFeatures = false;
//	private boolean generateJnlp = false;
//	private boolean signJars = false;
//	private String product = null;
	private BuildDirector director;
	private boolean generateProductFiles = true;
	
	protected String featureFolderName;
	protected String featureTempFolder;
	protected String featureFullName;
	protected String featureRootLocation;
	protected String sourceFeatureFullNameVersioned;
	
	private String customFeatureCallbacks = null;
	private String customCallbacksBuildpath = null;
	private String customCallbacksFailOnError = null;
	private String customCallbacksInheritAll = null;

	public FeatureBuildScriptGenerator(BuildTimeFeature feature) {
		this.feature = feature;
		
		if (featureRootLocation == null) {
			featureRootLocation = feature.getURL().getPath();
			int i = featureRootLocation.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (i != -1)
				featureRootLocation = featureRootLocation.substring(0, i);
		}
	}
	
	public void setDirector(BuildDirector director) {
		this.director = director;
	}
	
	private void initializeVariables() throws CoreException {
		featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
		featureFolderName = DEFAULT_FEATURE_LOCATION + '/' + featureFullName;
		featureTempFolder = Utils.getPropertyFormat(PROPERTY_FEATURE_TEMP_FOLDER);

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

	public void generate() throws CoreException {
		if (feature.isBinary())
			return;
				
		if (getBuildProperties() == MissingProperties.getInstance() || AbstractScriptGenerator.getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE)) {
			feature.setBinary(true);
			return;
		}

		initializeVariables();
		
		// if the feature defines its own custom script, we do not generate a
		// new one but we do try to update the version number
		boolean custom = TRUE.equalsIgnoreCase((String) getBuildProperties().get(PROPERTY_CUSTOM));
		File customBuildFile = null;
		if (custom) {
			customBuildFile = new File(featureRootLocation, DEFAULT_BUILD_SCRIPT_FILENAME);
			if (!customBuildFile.exists()) {
				String message = NLS.bind(Messages.error_missingCustomBuildFile, customBuildFile);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, null));
			}
			/* need to do root files here because we won't be doing the gatherBinParts where it normally happens */
			List configs = getConfigInfos();
			for (Iterator iter = configs.iterator(); iter.hasNext();) {
				director.getAssemblyData().addRootFileProvider((Config) iter.next(), feature);
			}

			//Feature had a custom build script, we need to update the version in it.
			//Do it here after generateChildrenScripts since there may have been a suffix generated.
			try {
				Utils.updateVersion(customBuildFile, PROPERTY_FEATURE_VERSION_SUFFIX, feature.getVersion());
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writeScript, customBuildFile);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, e));
			}
		} else {
			openScript(featureRootLocation, DEFAULT_BUILD_SCRIPT_FILENAME);
			try {
				generateBuildScript();
			} finally {
				closeScript();
			}
		}
	}
	
	private void absorbExceptionIfOptionalFeature(FeatureEntry entry, CoreException toAbsorb) throws CoreException {
		if (toAbsorb.getStatus().getCode() != EXCEPTION_FEATURE_MISSING || (toAbsorb.getStatus().getCode() == EXCEPTION_FEATURE_MISSING && !entry.isOptional()))
			throw toAbsorb;
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
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_FEATURE_TEMP_FOLDER) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + sourceFeatureFullNameVersioned + '/' + "src"); //$NON-NLS-1$
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, false, params);
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
		script.printTargetDeclaration(TARGET_CLEAN, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_clean, feature.getId()));
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
		script.printAntCallTask(TARGET_ALL_CHILDREN, false, params);
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
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, featureTempFolder + '/' + DEFAULT_PLUGIN_LOCATION + '/' + sourceFeatureFullNameVersioned + '/' + "src"); //$NON-NLS-1$
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
//			if (include != null || exclude != null) {
				FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
				script.printCopyTask(null, root, new FileSet[] {fileSet}, true, false);
//			}
			// Generate the parameters for the Id Replacer.
			String featureVersionInfo = ""; //$NON-NLS-1$
			// Here we get all the included features (independently of the config being built so the version numbers in the feature can be replaced)
			FeatureEntry[] includedFeatures = feature.getRawIncludedFeatureReferences();
			for (int i = 0; i < includedFeatures.length; i++) {
				String versionRequested = includedFeatures[i].getVersion();
				BuildTimeFeature includedFeature = null;
				try {
					includedFeature = getSite(false).findFeature(includedFeatures[i].getId(), versionRequested, true);
				} catch (CoreException e) {
					absorbExceptionIfOptionalFeature(includedFeatures[i], e);
					continue;
				}
				//VersionedIdentifier includedFeatureVersionId = includedFeature.getVersionedIdentifier();
				if (needsReplacement(versionRequested))
					featureVersionInfo += (includedFeature.getId() + ':' + extract3Segments(versionRequested) + ',' + includedFeature.getVersion() + ',');
			}
			String pluginVersionInfo = ""; //$NON-NLS-1$
			// Here we get all the included plugins (independently of the config being built so the version numbers in the feature can be replaced)
			FeatureEntry[] pluginsIncluded = feature.getRawPluginEntries();
			for (int i = 0; i < pluginsIncluded.length; i++) {
				BundleDescription model;
				// If we ask for 0.0.0, the call to the registry must have null as a parameter
				String versionRequested = pluginsIncluded[i].getVersion();
				String entryIdentifier = pluginsIncluded[i].getId();
				model = getSite(false).getRegistry().getResolvedBundle(entryIdentifier, versionRequested);
				if (model != null) {
					if (needsReplacement(versionRequested))
						pluginVersionInfo += (entryIdentifier + ':' + extract3Segments(versionRequested) + ',' + model.getVersion() + ',');
				}
			}
			script.println("<eclipse.idReplacer featureFilePath=\"" + AntScript.getEscaped(root) + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR + "\"  selfVersion=\"" + feature.getVersion() + "\" featureIds=\"" + featureVersionInfo + "\" pluginIds=\"" + pluginVersionInfo + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
		Map param = new HashMap(1);
		param.put(TARGET_ROOT_TARGET, TARGET_ROOTFILES_PREFIX + Utils.getPropertyFormat(PROPERTY_OS) + '_' + Utils.getPropertyFormat(PROPERTY_WS) + '_' + Utils.getPropertyFormat(PROPERTY_ARCH));		
		script.printAntCallTask(TARGET_ROOTFILES_PREFIX, true, param);
	}

	/**
	 *  
	 */
	private void generateRootFilesAndPermissions() throws CoreException {
		String product = generateProductFiles ? director.getProduct() : null;
		if (product != null && !havePDEUIState()) {
			ProductGenerator generator = new ProductGenerator();
			generator.setProduct(product);
			generator.setBuildSiteFactory(siteFactory);
			generator.setBuildProperties(getBuildProperties());
			generator.setRoot(featureRootLocation);
			generator.setWorkingDirectory(getWorkingDirectory());
			generator.setDirector(director);
			try {
				generator.generate();
			} catch (CoreException e) {
				//problem with the .product file
				//TODO Log warning/error
			}
		}
		
		script.printTargetDeclaration(TARGET_ROOTFILES_PREFIX, null, null, PROPERTY_OMIT_ROOTFILES, null);
		script.printAntCallTask(Utils.getPropertyFormat(TARGET_ROOT_TARGET), true, null);
		script.printTargetEnd();
		script.println();
		
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
			script.printAntCallTask(TARGET_ROOTFILES_PREFIX + aConfig.toString("_"), true, null);//.getPropertyFormat(PROPERTY_OS) + '_' + Utils.getPropertyFormat(PROPERTY_WS) + '_' + Utils.getPropertyFormat(PROPERTY_ARCH)) //$NON-NLS-1$
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

		director.getAssemblyData().addRootFileProvider(aConfig, feature);

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
		script.printTargetDeclaration(TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_buildUpdateJar, feature.getId()));
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
		script.printProperty(PROPERTY_FEATURE_BASE, featureTempFolder);
		script.printDeleteTask(featureTempFolder, null, null);
		script.printMkdirTask(featureTempFolder);
		script.printMkdirTask(featureTempFolder + '/' + featureFolderName);
		params.clear();
		params.put(PROPERTY_FEATURE_BASE, featureTempFolder);
		params.put(PROPERTY_OS, feature.getOS() == null ? Config.ANY : feature.getOS());
		params.put(PROPERTY_WS, feature.getWS() == null ? Config.ANY : feature.getWS());
		params.put(PROPERTY_ARCH, feature.getArch() == null ? Config.ANY : feature.getArch());
		params.put(PROPERTY_NL, feature.getNL() == null ? Config.ANY : feature.getNL());
		params.put(PROPERTY_OMIT_ROOTFILES, "true"); //$NON-NLS-1$
		
		// Be sure to call the gather with children turned off. The only way to
		// do this is
		// to clear all inherited values. Must remember to setup anything that
		// is really expected.
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, false, params);
		String jar = Utils.getPropertyFormat(PROPERTY_FEATURE_DESTINATION) + '/' + featureFullName + ".jar"; //$NON-NLS-1$
		script.printJarTask(jar, featureTempFolder + '/' + featureFolderName, null);
		script.printDeleteTask(featureTempFolder, null, null);
		if (director.getGenerateJnlp())
			script.println("<eclipse.jnlpGenerator feature=\"" + AntScript.getEscaped(jar) + "\"  codebase=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_JNLP_CODEBASE) + "\" j2se=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_JNLP_J2SE) + "\" locale=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_JNLP_LOCALE) + "\" generateOfflineAllowed=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_GENOFFLINE) + "\" configInfo=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_CONFIGS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ ); 
		if (director.getSignJars()) {
			if (director.getGenerateJnlp()) {
				script.printProperty(PROPERTY_UNSIGN, "true"); //$NON-NLS-1$
			}
			script.println("<eclipse.jarProcessor sign=\"" + Utils.getPropertyFormat(PROPERTY_SIGN) + "\" pack=\"" + Utils.getPropertyFormat(PROPERTY_PACK) + "\" unsign=\"" + Utils.getPropertyFormat(PROPERTY_UNSIGN) + "\" jar=\"" + AntScript.getEscaped(jar) + "\" alias=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS) + "\" keystore=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE) + "\" storepass=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}
		script.printAntCallTask(TARGET_REFRESH, true, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>zip.distribution</code> target to the given Ant script.
	 * Zip up the whole feature.
	 */
	protected void generateZipDistributionWholeTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_zips, feature.getId()));
		script.printDeleteTask(featureTempFolder, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_FEATURE_BASE, featureTempFolder);
		params.put(PROPERTY_INCLUDE_CHILDREN, TRUE);
		params.put(PROPERTY_OS, feature.getOS() == null ? Config.ANY : feature.getOS());
		params.put(PROPERTY_WS, feature.getWS() == null ? Config.ANY : feature.getWS());
		params.put(PROPERTY_ARCH, feature.getArch() == null ? Config.ANY : feature.getArch());
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
			FeatureEntry[] entries = Utils.getPluginEntry(feature, current.getSymbolicName(), false); //TODO This can be improved to use the value from the user object in the bundleDescription
			for (int j = 0; j < entries.length; j++) {
				List list = director.selectConfigs(entries[j]);
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

	protected List computeElements() throws CoreException {
		List computedElements = new ArrayList(5);
		FeatureEntry[] pluginList = feature.getPluginEntries();
		for (int i = 0; i < pluginList.length; i++) {
			FeatureEntry entry = pluginList[i];
			BundleDescription model;
			if (director.selectConfigs(entry).size() == 0)
				continue;

			String versionRequested = entry.getVersion();
			model = getSite(false).getRegistry().getResolvedBundle(entry.getId(), versionRequested);
			if (model == null) {
				String message = NLS.bind(Messages.exception_missingPlugin, entry.getId() + "_" + entry.getVersion()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null));
			}

			computedElements.add(model);
		}
		return computedElements;
	}
	
	private void generateAllFeaturesTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_ALL_FEATURES, TARGET_INIT, null, null, null);
		//if (analyseIncludedFeatures) {
			FeatureEntry[] features = feature.getIncludedFeatureReferences();
			for (int i = 0; i < features.length; i++) {
				String featureId = features[i].getId();
				String versionId = features[i].getVersion();
				BuildTimeFeature includedFeature = getSite(false).findFeature(featureId, versionId, false);
				if (includedFeature == null) {
					if (features[i].isOptional())
						continue;
					String message = NLS.bind(Messages.exception_missingFeature, featureId + ' ' + versionId);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
				}

				if (includedFeature.isBinary())
					continue;

				String includedFeatureDirectory = includedFeature.getURL().getPath();
				int j = includedFeatureDirectory.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
				if (j != -1)
					includedFeatureDirectory = includedFeatureDirectory.substring(0, j);
				IPath location;
				location = Utils.makeRelative(new Path(includedFeatureDirectory), new Path(featureRootLocation));
				script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toString(), Utils.getPropertyFormat(PROPERTY_TARGET), null, null, null);
			}
		//}
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
		script.printProjectDeclaration(feature.getId(), TARGET_BUILD_UPDATE_JAR, "."); //$NON-NLS-1$
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
			buildProperties = readProperties(featureRootLocation, PROPERTIES_FILE, director.isIgnoreMissingPropertiesFile() ? IStatus.OK : IStatus.WARNING);
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
		script.printTargetDeclaration(TARGET_BUILD_JARS, TARGET_INIT, null, null, NLS.bind(Messages.build_feature_buildJars, feature.getId()));
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
		script.printTargetDeclaration(TARGET_REFRESH, TARGET_INIT, PROPERTY_ECLIPSE_RUNNING, null, NLS.bind(Messages.build_feature_refresh, feature.getId()));
		script.printConvertPathTask(new Path(featureRootLocation).removeLastSegments(0).toOSString().replace('\\', '/'), PROPERTY_RESOURCE_PATH, false);
		script.printRefreshLocalTask(Utils.getPropertyFormat(PROPERTY_RESOURCE_PATH), "infinite"); //$NON-NLS-1$
		Map params = new HashMap(2);
		params.put(PROPERTY_TARGET, TARGET_REFRESH);
		script.printAntCallTask(TARGET_ALL_CHILDREN, true, params);
		script.printTargetEnd();
	}

	public void setGenerateProductFiles(boolean generateProductFiles) {
		this.generateProductFiles = generateProductFiles;
	}
}
