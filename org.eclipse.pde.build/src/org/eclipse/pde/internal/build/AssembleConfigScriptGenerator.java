/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
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
	protected IFeature[] features;
	protected BundleDescription[] plugins;
	protected String filename;
	protected boolean copyRootFile;
	protected Properties pluginsPostProcessingSteps;
	protected Properties featuresPostProcessingSteps;

	private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$
	private static final String PROPERTY_ELEMENT_NAME = "elementName"; //$NON-NLS-1$

	private static final String UPDATEJAR = "updateJar"; //$NON-NLS-1$
	private static final String FLAT = "flat"; //$NON-NLS-1$

	private static final byte BUNDLE = 0;
	private static final byte FEATURE = 1;

	private static final byte FOLDER = 0;
	private static final byte FILE = 1;
	private String PROPERTY_ECLIPSE_PLUGINS = "eclipse.plugins"; //$NON-NLS-1$
	private String PROPERTY_ECLIPSE_FEATURES = "eclipse.features"; //$NON-NLS-1$

	public AssembleConfigScriptGenerator() {
		super();
	}

	public void initialize(String directoryName, String scriptName, String feature, Config configurationInformation, Collection elementList, Collection featureList, boolean rootFileCopy) throws CoreException {
		this.directory = directoryName;
		this.featureId = feature;
		this.configInfo = configurationInformation;
		this.copyRootFile = rootFileCopy;

		this.features = new IFeature[featureList.size()];
		featureList.toArray(this.features);

		this.plugins = new BundleDescription[elementList.size()];
		this.plugins = (BundleDescription[]) elementList.toArray(this.plugins);

		filename = directory + '/' + (scriptName != null ? scriptName : getFilename()); //$NON-NLS-1$
		try {
			script = new AntScript(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			//TODO Log an error
			// a file doesn't exist so we will create a new one
		} catch (IOException e) {
			String message = Policy.bind("exception.writingFile", filename); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
		loadPostProcessingSteps();
	}

	private void loadPostProcessingSteps() throws CoreException {
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
		if (!outputFormat.equalsIgnoreCase("folder")) { //$NON-NLS-1$
			if (configInfo.getOs().equalsIgnoreCase("macosx")) { //$NON-NLS-1$
				generateTarTarget();
				generateGZipTarget();
			} else {
				if (outputFormat.equalsIgnoreCase("zip")) //$NON-NLS-1$
					generateZipTarget();
				else
					generateAntZipTarget();
			}
		}
		generateEpilogue();
	}

	/**
	 * 
	 */
	private void generateGatherSourceCalls() throws CoreException {
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

	private void generatePackagingTargets() {
		String fileName = getPropertyFormat(PROPERTY_SOURCE) + '/' + getPropertyFormat(PROPERTY_ELEMENT_NAME);
		String fileExists = getPropertyFormat(PROPERTY_SOURCE) + '/' + getPropertyFormat(PROPERTY_ELEMENT_NAME) + "_exists"; //$NON-NLS-1$

		script.printComment("Beginning of the jarUp task"); //$NON-NLS-1$
		script.printTargetDeclaration(TARGET_JARUP, null, null, null, Policy.bind("assemble.jarUp")); //$NON-NLS-1$
		script.printAvailableTask(fileExists, fileName);
		Map params = new HashMap(2);
		params.put(PROPERTY_SOURCE, getPropertyFormat(PROPERTY_SOURCE));
		params.put(PROPERTY_ELEMENT_NAME, getPropertyFormat(PROPERTY_ELEMENT_NAME));
		script.printAntCallTask(TARGET_JARING, null, params);
		script.printTargetEnd();

		script.printTargetDeclaration(TARGET_JARING, null, fileExists, null, null);
		script.printZipTask(fileName + ".jar", fileName, false, false, null); //$NON-NLS-1$
		script.printDeleteTask(fileName, null, null);
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

	private void generatePrologue() {
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

		if (!"folder".equalsIgnoreCase(outputFormat)) //$NON-NLS-1$
			script.printDeleteTask(getPropertyFormat(PROPERTY_ASSEMBLY_TMP), null, null);
		script.printMkdirTask(getPropertyFormat(PROPERTY_ASSEMBLY_TMP));
		script.printMkdirTask(getPropertyFormat(PROPERTY_BUILD_LABEL));
	}

	private void generatePostProcessingSteps() throws CoreException {
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			if (forceUpdateJarFormat) //Force the updateJar if it is asked as an output format
				pluginsPostProcessingSteps.put(plugin.getSymbolicName(), UPDATEJAR);
			generatePostProcessingSteps(plugin.getSymbolicName(), plugin.getVersion().toString(), BUNDLE);
		}

		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			if (forceUpdateJarFormat) //Force the updateJar if it is asked as an output format
				featuresPostProcessingSteps.put(feature.getVersionedIdentifier().getIdentifier(), UPDATEJAR);
			generatePostProcessingSteps(feature.getVersionedIdentifier().getIdentifier(), feature.getVersionedIdentifier().getVersion().toString(), FEATURE);
		}
	}

	private void generateGatherBinPartsCalls() throws CoreException {
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
	}

	//generate the appropriate postProcessingCall
	private void generatePostProcessingSteps(String name, String version, byte type) {
		String style = getPluginUnpackClause(name, version);
		Properties currentProperties = type == BUNDLE ? pluginsPostProcessingSteps : featuresPostProcessingSteps;
		String styleFromFile = currentProperties.getProperty(name);
		if (styleFromFile != null) //The info from the file override the one from the feaature
			style = styleFromFile;

		if (FLAT.equalsIgnoreCase(style)) {
			//do nothing
			return;
		}
		if (UPDATEJAR.equalsIgnoreCase(style)) {
			generateJarUpCall(name, version, type);
			return;
		}
	}

	//Get the unpack clause from the feature.xml
	private String getPluginUnpackClause(String name, String version) {
		for (int i = 0; i < features.length; i++) {
			IPluginEntry[] entries = features[i].getPluginEntries(); //Only plugin being built needs to be considered 
			for (int j = 0; j < entries.length; j++) {
				if (entries[j].getVersionedIdentifier().getIdentifier().equals(name))
					return ((org.eclipse.update.core.PluginEntry) entries[j]).isUnpack() ? FLAT : UPDATEJAR;
			}
		}
		return FLAT;
	}

	private Object[] getFinalShape(String name, String version, byte type) {
		String style = getPluginUnpackClause(name, version);
		Properties currentProperties = type == BUNDLE ? pluginsPostProcessingSteps : featuresPostProcessingSteps;
		String styleFromFile = currentProperties.getProperty(name);
		if (styleFromFile != null)
			style = styleFromFile;

		if (FLAT.equalsIgnoreCase(style)) {
			//do nothing
			return new Object[] {name + '_' + version, new Byte(FOLDER)};
		}
		if (UPDATEJAR.equalsIgnoreCase(style)) {
			return new Object[] {name + '_' + version + ".jar", new Byte(FILE)}; //$NON-NLS-1$
		}
		return new Object[] {name + '_' + version, new Byte(FOLDER)};
	}

	private void generateJarUpCall(String name, String version, byte type) {
		Map properties = new HashMap(2);
		properties.put(PROPERTY_SOURCE, type == BUNDLE ? getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : getPropertyFormat(PROPERTY_ECLIPSE_FEATURES));
		properties.put(PROPERTY_ELEMENT_NAME, name + '_' + version);
		script.printAntCallTask(TARGET_JARUP, null, properties);
	}

	private void generateEpilogue() {
		script.printTargetEnd();
		script.printProjectEnd();
		script.close();
	}

	public String getFilename() {
		return getTargetName() + ".xml"; //$NON-NLS-1$
	}

	public String getTargetName() {
		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	private void generateZipTarget() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) getFinalShape(plugins[i].getSymbolicName(), plugins[i].getVersion().toString(), BUNDLE)[0]);
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
			parameters.add(getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) getFinalShape(features[i].getVersionedIdentifier().getIdentifier(), features[i].getVersionedIdentifier().getVersion().toString(), FEATURE)[0]);
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
		if (!copyRootFile)
			return;

		List parameters = new ArrayList(1);
		parameters.add("-r -q ${zipargs} " + getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + " . "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printExecTask("zip", getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	}

	private void createZipExecCommand(List parameters) {
		parameters.add(0, "-r -q " + getPropertyFormat(PROPERTY_ZIP_ARGS) + " " + getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("zip", getPropertyFormat(PROPERTY_ASSEMBLY_TMP), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected String computeArchiveName() {
		return featureId + "-" + getPropertyFormat(PROPERTY_BUILD_ID_PARAM) + (configInfo.equals(Config.genericConfig()) ? "" : ("-" + configInfo.toStringReplacingAny(".", ANY_STRING))) + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public void generateTarTarget() {
		//This task only support creation of archive with eclipse at the root 
		//Need to do the copy using cp because of the link
		List parameters = new ArrayList(2);
		parameters.add("-r " + getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + ' ' + getPropertyFormat(PROPERTY_ASSEMBLY_TMP)); //$NON-NLS-1$ //$NON-NLS-2$  
		script.printExecTask("cp", getPropertyFormat(PROPERTY_BASEDIR), parameters, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$

		parameters.clear();
		parameters.add("-rf " + getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("rm", getPropertyFormat(PROPERTY_BASEDIR), parameters, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$

		parameters.clear();
		parameters.add("-cvf " + getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + ' ' + getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + ' '); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("tar", getPropertyFormat(PROPERTY_ASSEMBLY_TMP), parameters, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void generateAntZipTarget() {
		FileSet[] filesPlugins = new FileSet[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			Object[] shape = getFinalShape(plugins[i].getSymbolicName(), plugins[i].getVersion().toString(), BUNDLE);
			filesPlugins[i] = new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1].equals(new Byte(FILE)), null, null, null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], null);
		}
		script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesPlugins);

		FileSet[] filesFeatures = new FileSet[features.length];
		for (int i = 0; i < features.length; i++) {
			Object[] shape = getFinalShape(features[i].getVersionedIdentifier().getIdentifier(), features[i].getVersionedIdentifier().getVersion().toString(), FEATURE);
			filesFeatures[i] = new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1].equals(new Byte(FILE)), null, null, null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], null);
		}
		script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesFeatures);

		if (!copyRootFile)
			return;

		FileSet[] rootFiles = new FileSet[1];
		rootFiles[0] = new ZipFileSet(getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), false, null, "**/**", null, null, null, getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null); //$NON-NLS-1$//$NON-NLS-2$
		script.printZipTask(getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, rootFiles);
	}
}