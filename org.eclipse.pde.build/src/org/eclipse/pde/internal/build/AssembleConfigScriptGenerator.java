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
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IPluginEntry;

/**
 * Generate an assemble script for a given feature and a given config. It
 * generates all the instruction to zip the listed plugins and features.
 */
public class AssembleConfigScriptGenerator extends AbstractScriptGenerator {
	protected String directory; // representing the directory where to generate the file
	protected String featureId;
	protected Config configInfo;
	protected IFeature[] features; // the features that will be assembled
	protected IFeature[] allFeatures; //the set of all the features that have been considered
	protected BundleDescription[] plugins;
	protected String filename;
	protected Collection rootFileProviders;
	protected Properties pluginsPostProcessingSteps;
	protected Properties featuresPostProcessingSteps;

	private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$
	private static final String PROPERTY_ELEMENT_NAME = "elementName"; //$NON-NLS-1$

	private static final String UPDATEJAR = "updateJar"; //$NON-NLS-1$
	private static final String FLAT = "flat"; //$NON-NLS-1$

	private static final byte BUNDLE = 0;
	private static final byte FEATURE = 1;

	private static final String FOLDER = "folder"; //$NON-NLS-1$
	private static final String FILE = "file"; //$NON-NLS-1$
	protected String PROPERTY_ECLIPSE_PLUGINS = "eclipse.plugins"; //$NON-NLS-1$
	protected String PROPERTY_ECLIPSE_FEATURES = "eclipse.features"; //$NON-NLS-1$
	private boolean signJars;
	private boolean generateJnlp;

	public AssembleConfigScriptGenerator() {
		super();
	}

	public void initialize(String directoryName, String feature, Config configurationInformation, Collection elementList, Collection featureList, Collection allFeaturesList, Collection rootProviders) throws CoreException {
		this.directory = directoryName;
		this.featureId = feature;
		this.configInfo = configurationInformation;
		this.rootFileProviders = rootProviders != null ? rootProviders : new ArrayList(0);

		this.features = new IFeature[featureList.size()];
		featureList.toArray(this.features);

		this.allFeatures = new IFeature[allFeaturesList.size()];
		allFeaturesList.toArray(this.allFeatures);
		
		this.plugins = new BundleDescription[elementList.size()];
		this.plugins = (BundleDescription[]) elementList.toArray(this.plugins);

		openScript(directoryName, getTargetName() + ".xml");
		loadPostProcessingSteps();
	}

	private void loadPostProcessingSteps() {
		try {
			pluginsPostProcessingSteps = readProperties(AbstractScriptGenerator.getWorkingDirectory(), DEFAULT_PLUGINS_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR, IStatus.INFO);
			featuresPostProcessingSteps = readProperties(AbstractScriptGenerator.getWorkingDirectory(), DEFAULT_FEATURES_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR, IStatus.INFO);
		} catch (CoreException e) {
			//Ignore
		}
	}

	public void generate() throws CoreException {
		generatePrologue();
		generateInitializationSteps();
		generateGatherBinPartsCalls();
		if (embeddedSource)
			generateGatherSourceCalls();
		generatePostProcessingSteps();
		if (brandExecutable)
			generateBrandingCalls();
		generateArchivingSteps();
		generateEpilogue();
	}

	/**
	 * 
	 */
	private void generateBrandingCalls() {
		String install = getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER); //$NON-NLS-1$
		script.printBrandTask(install, getPropertyFormat(PROPERTY_LAUNCHER_ICONS), getPropertyFormat(PROPERTY_LAUNCHER_NAME));
	}

	private void generateArchivingSteps() {
		if (outputFormat.equalsIgnoreCase("folder")) { //$NON-NLS-1$
			generateMoveRootFiles();
			return;
		}
		//Windows is archived as zip
		if (configInfo.getOs().equalsIgnoreCase(Constants.OS_WIN32) || configInfo.equals(Config.genericConfig())) {
			if (outputFormat.equalsIgnoreCase("zip")) //$NON-NLS-1$
				generateZipTarget();
			else
				generateAntZipTarget();
			return;
		}
		
		//Non-windows platform are archived as tar.gz
		if (!Platform.getOS().equals(Constants.OS_WIN32)) {
			generateTarTarget();
			generateGZipTarget();
		} else {
			generateAntTarTarget();
		}
	}

	private void generateMoveRootFiles() {
		if (rootFileProviders.size() == 0)
			return;
		FileSet[] rootFiles = new FileSet[1];
		rootFiles[0] = new FileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), null, "**/**", null, null, null, null); //$NON-NLS-1$//$NON-NLS-2$	
		script.printMoveTask(getPropertyFormat(PROPERTY_ECLIPSE_BASE), rootFiles,false);
		script.printDeleteTask(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING), null, null);
	}
	
	protected void generateGatherSourceCalls() {
		Map properties = new HashMap(1);
		properties.put(PROPERTY_DESTINATION_TEMP_FOLDER, getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS));
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			String placeToGather = getLocation(plugin);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_SOURCES, null, null, properties);
		}

		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_SOURCES, null, null, properties);
		}
	}

	protected void generatePackagingTargets() {
		String fileName = getPropertyFormat(PROPERTY_SOURCE) + '/' + getPropertyFormat(PROPERTY_ELEMENT_NAME);
		String fileExists = getPropertyFormat(PROPERTY_SOURCE) + '/' + getPropertyFormat(PROPERTY_ELEMENT_NAME) + "_exists"; //$NON-NLS-1$

		script.printComment("Beginning of the jarUp task"); //$NON-NLS-1$
		script.printTargetDeclaration(TARGET_JARUP, null, null, null, Messages.assemble_jarUp);
		script.printAvailableTask(fileExists, fileName);
		Map params = new HashMap(2);
		params.put(PROPERTY_SOURCE, getPropertyFormat(PROPERTY_SOURCE));
		params.put(PROPERTY_ELEMENT_NAME, getPropertyFormat(PROPERTY_ELEMENT_NAME));
		script.printAntCallTask(TARGET_JARING, null, params);
		script.printTargetEnd();

		script.printTargetDeclaration(TARGET_JARING, null, fileExists, null, null);
		script.printZipTask(fileName + ".jar", fileName, false, false, null); //$NON-NLS-1$
		script.printDeleteTask(fileName, null, null);
		if (signJars)
			script.println("<signjar jar=\"" + fileName + ".jar" + "\" alias=\"" + getPropertyFormat("sign.alias") + "\" keystore=\"" + getPropertyFormat("sign.keystore") + "\" storepass=\"" + getPropertyFormat("sign.storepass") + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ 

		script.printTargetEnd();
		script.printComment("End of the jarUp task"); //$NON-NLS-1$
	}

	private void generateGZipTarget() {
		script.println("<move file=\"" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + "\" tofile=\"" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + "/tmp.tar\"/>"); //$NON-NLS-1$
		script.printGZip(getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + "/tmp.tar", //$NON-NLS-1$ //$NON-NLS-2$
				getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH));
		List args = new ArrayList(2);
		args.add("-rf"); //$NON-NLS-1$
		args.add(getPropertyFormat(PROPERTY_ASSEMBLY_TMP));
		script.printExecTask("rm", null, args, null); //$NON-NLS-1$
	}

	protected void generatePrologue() {
		script.printProjectDeclaration("Assemble " + featureId, TARGET_MAIN, null); //$NON-NLS-1$  
		script.printProperty(PROPERTY_ARCHIVE_NAME, computeArchiveName());
		script.printProperty(PROPERTY_OS, configInfo.getOs());
		script.printProperty(PROPERTY_WS, configInfo.getWs());
		script.printProperty(PROPERTY_ARCH, configInfo.getArch());
		script.printProperty(PROPERTY_ASSEMBLY_TMP, getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/tmp"); //$NON-NLS-1$
		script.printProperty(PROPERTY_ECLIPSE_BASE, getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_ECLIPSE_PLUGINS, getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION);
		script.printProperty(PROPERTY_ECLIPSE_FEATURES, getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION);
		script.printProperty(PROPERTY_ARCHIVE_FULLPATH, getPropertyFormat(PROPERTY_BASEDIR) + '/' + getPropertyFormat(PROPERTY_BUILD_LABEL) + '/' + getPropertyFormat(PROPERTY_ARCHIVE_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_TAR_ARGS, ""); //$NON-NLS-1$
		generatePackagingTargets();
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateInitializationSteps() {
		if (BundleHelper.getDefault().isDebugging()) {
			script.printEchoTask("basedir : " + getPropertyFormat(PROPERTY_BASEDIR)); //$NON-NLS-1$
			script.printEchoTask("assemblyTempDir : " + getPropertyFormat(PROPERTY_ASSEMBLY_TMP)); //$NON-NLS-1$
			script.printEchoTask("eclipse.base : " + getPropertyFormat(PROPERTY_ECLIPSE_BASE)); //$NON-NLS-1$
			script.printEchoTask("collectingFolder : " + getPropertyFormat(PROPERTY_COLLECTING_FOLDER)); //$NON-NLS-1$
			script.printEchoTask("archivePrefix : " + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX)); //$NON-NLS-1$
		}

		script.println("<condition property=\"" + PROPERTY_PLUGIN_ARCHIVE_PREFIX + "\" value=\"" + DEFAULT_PLUGIN_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<equals arg1=\"" + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + "\"  arg2=\"\" trim=\"true\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_PLUGIN_ARCHIVE_PREFIX, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_PLUGIN_LOCATION);

		script.println();
		script.println("<condition property=\"" + PROPERTY_FEATURE_ARCHIVE_PREFIX + "\" value=\"" + DEFAULT_FEATURE_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<equals arg1=\"" + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + "\"  arg2=\"\" trim=\"true\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_FEATURE_ARCHIVE_PREFIX, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_FEATURE_LOCATION);

		script.println();

		script.printDirName(PROPERTY_ARCHIVE_PARENT, getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH));
		script.printMkdirTask(getPropertyFormat(PROPERTY_ARCHIVE_PARENT));
		script.printMkdirTask(getPropertyFormat(PROPERTY_ASSEMBLY_TMP));
		script.printMkdirTask(getPropertyFormat(PROPERTY_BUILD_LABEL));
	}

	protected void generatePostProcessingSteps() {
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			generatePostProcessingSteps(plugin.getSymbolicName(), plugin.getVersion().toString(), BUNDLE);
		}

		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			generatePostProcessingSteps(feature.getVersionedIdentifier().getIdentifier(), feature.getVersionedIdentifier().getVersion().toString(), FEATURE);
		}
	}

	protected void generateGatherBinPartsCalls() {
		Map properties = new HashMap(1);
		properties.put(PROPERTY_DESTINATION_TEMP_FOLDER, getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS));
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			String placeToGather = getLocation(plugin);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, properties);
		}

		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, properties);
		}

		//This will generate gather.bin.parts call to features that provides files for the root
		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			IFeature feature = (IFeature) iter.next();
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, properties);
		}
	}

	//generate the appropriate postProcessingCall
	private void generatePostProcessingSteps(String name, String version, byte type) {
		String style = (String) getFinalShape(name, version, type)[1];

		if (FOLDER.equalsIgnoreCase(style))
			return;
		if (FILE.equalsIgnoreCase(style)) {
			generateJarUpCall(name, version, type);
			String dir = type == BUNDLE ? getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : getPropertyFormat(PROPERTY_ECLIPSE_FEATURES);
			String location = dir + '/' + name + '_' + version + ".jar";  //$NON-NLS-1$
			if (type == FEATURE) 
				if (generateJnlp)
					script.println("<eclipse.jnlpGenerator feature=\"" + location + "\"  codebase=\"" + getPropertyFormat("jnlp.codebase") + "\" j2se=\"" + getPropertyFormat("jnlp.j2se") + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ 
			return;
		}
	}

	//Get the unpack clause from the feature.xml
	//TODO Need to improve the algorithm
	private String getPluginUnpackClause(String name, String version) {
		for (int i = 0; i < allFeatures.length; i++) {
			IPluginEntry[] entries = allFeatures[i].getPluginEntries(); //Only plugin being built needs to be considered 
			for (int j = 0; j < entries.length; j++) {
				if (entries[j].getVersionedIdentifier().getIdentifier().equals(name))
					return ((org.eclipse.update.core.PluginEntry) entries[j]).isUnpack() ? FLAT : UPDATEJAR;
			}
		}
		return FLAT;
	}

	protected Object[] getFinalShape(String name, String version, byte type) {
		String style = getPluginUnpackClause(name, version);
		Properties currentProperties = type == BUNDLE ? pluginsPostProcessingSteps : featuresPostProcessingSteps;
		if (currentProperties.size() > 0) {
			String styleFromFile = currentProperties.getProperty(name);
			if (styleFromFile == null)
				styleFromFile = currentProperties.getProperty(DEFAULT_FINAL_SHAPE);
			style = styleFromFile;
		}
		if (forceUpdateJarFormat)
			style = UPDATEJAR;
		
		if (FLAT.equalsIgnoreCase(style)) {
			//do nothing
			return new Object[] {name + '_' + version, FOLDER};
		}
		if (UPDATEJAR.equalsIgnoreCase(style)) {
			return new Object[] {name + '_' + version + ".jar", FILE}; //$NON-NLS-1$
		}
		return new Object[] {name + '_' + version, FOLDER};
	}

	private void generateJarUpCall(String name, String version, byte type) {
		Map properties = new HashMap(2);
		properties.put(PROPERTY_SOURCE, type == BUNDLE ? getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : getPropertyFormat(PROPERTY_ECLIPSE_FEATURES));
		properties.put(PROPERTY_ELEMENT_NAME, name + '_' + version);
		script.printAntCallTask(TARGET_JARUP, null, properties);
	}

	private void generateEpilogue() {
		if (!"folder".equalsIgnoreCase(outputFormat)) //$NON-NLS-1$
			script.printDeleteTask(getPropertyFormat(PROPERTY_ASSEMBLY_TMP), null, null);
		script.printTargetEnd();
		script.printProjectEnd();
		script.close();
		script = null;
	}

	public String getTargetName() {
		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	private void generateZipTarget() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) getFinalShape(plugins[i].getSymbolicName(), plugins[i].getVersion().toString(), BUNDLE)[0]);
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < features.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) getFinalShape(features[i].getVersionedIdentifier().getIdentifier(), features[i].getVersionedIdentifier().getVersion().toString(), FEATURE)[0]);
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		createZipRootFileCommand();
	}

	/**
	 *  Zip the root files
	 */
	private void createZipRootFileCommand() {
		if (rootFileProviders.size() == 0)
			return;

		List parameters = new ArrayList(1);
		parameters.add("-r -q ${zipargs} " + getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + " . "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printExecTask("zip", getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private void createZipExecCommand(List parameters) {
		parameters.add(0, "-r -q " + getPropertyFormat(PROPERTY_ZIP_ARGS) + ' ' + getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH)); //$NON-NLS-1$
		script.printExecTask("zip", getPropertyFormat(PROPERTY_ASSEMBLY_TMP), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected String computeArchiveName() {
		return featureId + "-" + getPropertyFormat(PROPERTY_BUILD_ID_PARAM) + (configInfo.equals(Config.genericConfig()) ? "" : ("-" + configInfo.toStringReplacingAny(".", ANY_STRING))) + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public void generateTarTarget() {
		//This task only support creation of archive with eclipse at the root 
		//Need to do the copy using cp because of the link
		List parameters = new ArrayList(2);
		if (rootFileProviders.size() > 0) {
			parameters.add("-r " + getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + ' ' + getPropertyFormat(PROPERTY_ASSEMBLY_TMP)); //$NON-NLS-1$ //$NON-NLS-2$  
			script.printExecTask("cp", getPropertyFormat(PROPERTY_BASEDIR), parameters, null); //$NON-NLS-1$

			parameters.clear();
			parameters.add("-rf " + getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING)); //$NON-NLS-1$ //$NON-NLS-2$
			script.printExecTask("rm", getPropertyFormat(PROPERTY_BASEDIR), parameters, null); //$NON-NLS-1$
		}
		parameters.clear();
		parameters.add("-cvf " + getPropertyFormat(PROPERTY_TAR_ARGS) + ' ' + getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + ' ' + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + ' '); //$NON-NLS-1$
		script.printExecTask("tar", getPropertyFormat(PROPERTY_ASSEMBLY_TMP), parameters, null); //$NON-NLS-1$ 
	}
	
	//TODO this code andn the generateAntTarTarget() should be refactored using a factory or something like that.
	protected void generateAntZipTarget() {
		FileSet[] filesPlugins = new FileSet[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			Object[] shape = getFinalShape(plugins[i].getSymbolicName(), plugins[i].getVersion().toString(), BUNDLE);
			filesPlugins[i] = new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (plugins.length != 0)
			script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesPlugins);

		FileSet[] filesFeatures = new FileSet[features.length];
		for (int i = 0; i < features.length; i++) {
			Object[] shape = getFinalShape(features[i].getVersionedIdentifier().getIdentifier(), features[i].getVersionedIdentifier().getVersion().toString(), FEATURE);
			filesFeatures[i] = new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (features.length != 0)
			script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesFeatures);

		if (rootFileProviders.size() == 0)
			return;

		FileSet[] permissionSets = generatePermissions(true);
		FileSet[] rootFiles = new FileSet[permissionSets.length + 1];
		System.arraycopy(permissionSets, 0, rootFiles, 1, permissionSets.length);
		rootFiles[0] = new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), false, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, null); //$NON-NLS-1$//$NON-NLS-2$
		script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, rootFiles);
	}
	
	private FileSet[] generatePermissions(boolean zip) {
		String configInfix = configInfo.toString("."); //$NON-NLS-1$
		String prefixPermissions = ROOT_PREFIX + configInfix + '.' + PERMISSIONS + '.';
		String commonPermissions = ROOT_PREFIX + PERMISSIONS + '.';
		ArrayList fileSets = new ArrayList();
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			Properties featureProperties = null;
			try {
				featureProperties = AbstractScriptGenerator.readProperties(new Path(((IFeature) iter.next()).getURL().getFile()).removeLastSegments(1).toOSString(), PROPERTIES_FILE, IStatus.OK);
			} catch(CoreException e) {
				return new FileSet[0];
			}
			
			for (Iterator iter2 = featureProperties.entrySet().iterator(); iter2.hasNext();) {
				Map.Entry permission = (Map.Entry) iter2.next();
				String instruction = (String) permission.getKey();
				String parameters = (String) permission.getValue();
				if (instruction.startsWith(prefixPermissions)) {
					if (zip)
						fileSets.add(new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + parameters, true, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, instruction.substring(prefixPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$
					else 
						fileSets.add(new TarFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + parameters, true, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, instruction.substring(prefixPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$
					continue;
				}
				if (instruction.startsWith(commonPermissions)) {
					if (zip)
						fileSets.add(new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + parameters, true, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, instruction.substring(commonPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$
					else 
						fileSets.add(new TarFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + parameters, true, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, instruction.substring(commonPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$
					continue;
				}
			}
		}
		return (FileSet[]) fileSets.toArray(new FileSet[fileSets.size()]);
	}
	
	
	//TODO this code andn the generateAntZipTarget() should be refactored using a factory or something like that.
	private void generateAntTarTarget() {
		FileSet[] filesPlugins = new FileSet[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			Object[] shape = getFinalShape(plugins[i].getSymbolicName(), plugins[i].getVersion().toString(), BUNDLE);
			filesPlugins[i] = new TarFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (plugins.length != 0)
			script.printTarTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesPlugins);

		FileSet[] filesFeatures = new FileSet[features.length];
		for (int i = 0; i < features.length; i++) {
			Object[] shape = getFinalShape(features[i].getVersionedIdentifier().getIdentifier(), features[i].getVersionedIdentifier().getVersion().toString(), FEATURE);
			filesFeatures[i] = new TarFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (features.length != 0)
			script.printTarTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesFeatures);

		if (rootFileProviders.size() == 0)
			return;

		FileSet[] permissionSets = generatePermissions(false);
		FileSet[] rootFiles = new FileSet[permissionSets.length + 1];
		System.arraycopy(permissionSets, 0, rootFiles, 1, permissionSets.length);
		rootFiles[0] = new TarFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), false, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, null); //$NON-NLS-1$//$NON-NLS-2$
		script.printTarTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, rootFiles);
	}

	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}

	public void setSignJars(boolean value) {
		signJars = value;
	}
}
