/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.core.runtime.model.PluginFragmentModel;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.update.core.IFeature;

//FIXME This whole hierarchy of assembler needs to be polished... creation of an interface, etc...
/**
 * Generate an assemble script for a given feature and a given config. It
 * generates all the instruction to zip the listed plugins and features.
 */
public class AssembleConfigScriptGenerator extends AbstractScriptGenerator {
	protected String directory; // representing the directory where to generate the file
	protected String featureId;
	protected Config configInfo;
	protected IFeature[] features;
	protected PluginModel[] plugins;
	protected PluginFragmentModel[] fragments;
	protected String filename;

	public AssembleConfigScriptGenerator() {
	}

	public void initialize(String directoryName, String scriptName, String feature, Config configurationInformation, Collection pluginList, Collection fragmentList, Collection featureList) throws CoreException {
		this.directory = directoryName;
		this.featureId = feature;
		this.configInfo = configurationInformation;

		this.features = new IFeature[featureList.size()];
		featureList.toArray(this.features);

		this.plugins = new PluginModel[pluginList.size()];
		pluginList.toArray(this.plugins);

		this.fragments = new PluginFragmentModel[fragmentList.size()];
		fragmentList.toArray(this.fragments);

		filename = directory + "/" + (scriptName != null ? scriptName : getFilename()); //$NON-NLS-1$
		try {
			script = new AntScript(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			// a file doesn't exist so we will create a new one
		} catch (IOException e) {
			String message = Policy.bind("exception.writingFile", filename); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}

	public void generate() throws CoreException {
		generatePrologue();
		generateInitializationSteps();
		generateGatherBinPartsCalls();
		if (configInfo.getOs().equalsIgnoreCase("macosx")) { //$NON-NLS-1$
			generateTarTarget();
			generateGZipTarget();
		} else {
			generateZipTarget();
		}
		generateEpilogue();
	}

	/**
	 * 
	 */
	private void generateGZipTarget() {
		script.println(
			"<move file=\"" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_BASEDIR)
				+ "/" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_BUILD_LABEL)
				+ "/" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_COLLECTING_BASE)
				+ "/" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_ARCHIVE_NAME)
				+ "\" tofile=\"" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_BASEDIR)
				+ "/" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_BUILD_LABEL)
				+ "/" //$NON-NLS-1$
				+ getPropertyFormat(PROPERTY_COLLECTING_BASE)
				+ "/tmp.tar\"/>"); //$NON-NLS-1$
		script.printGZip(
			getPropertyFormat(PROPERTY_BASEDIR) + "/" + getPropertyFormat(PROPERTY_BUILD_LABEL) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE) + "/tmp.tar", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			getPropertyFormat(PROPERTY_BASEDIR) + "/" + getPropertyFormat(PROPERTY_BUILD_LABEL) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE) + "/" + getPropertyFormat(PROPERTY_ARCHIVE_NAME)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		List args = new ArrayList(2);
		args.add("-rf"); //$NON-NLS-1$
		args.add(getPropertyFormat(PROPERTY_FEATURE_BASE));
		script.printExecTask("rm", null, args, null); //$NON-NLS-1$
		script.printDeleteTask(null, getPropertyFormat(PROPERTY_BASEDIR) + "/" + getPropertyFormat(PROPERTY_BUILD_LABEL) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE) + "/tmp.tar", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void generatePrologue() {
		script.printProjectDeclaration("Assemble " + featureId, TARGET_MAIN, null); //$NON-NLS-1$
		script.printProperty(PROPERTY_ARCHIVE_NAME, computeArchiveName());
		script.printProperty(PROPERTY_OS, configInfo.getOs());
		script.printProperty(PROPERTY_WS, configInfo.getWs());
		script.printProperty(PROPERTY_ARCH, configInfo.getArch());
		script.printProperty(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_BASEDIR) + "/" + getPropertyFormat(PROPERTY_BUILD_LABEL) + "/" + getPropertyFormat(PROPERTY_COLLECTING_PLACE)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_DESTINATION_TEMP_FOLDER, getPropertyFormat(PROPERTY_FEATURE_BASE) + "/" + DEFAULT_PLUGIN_LOCATION); //$NON-NLS-1$

		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateInitializationSteps() {
		script.printDeleteTask(getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
		script.printMkdirTask(getPropertyFormat(PROPERTY_BUILD_LABEL));
	}

	private void generateGatherBinPartsCalls() throws CoreException {
		for (int i = 0; i < plugins.length; i++) {
			PluginModel plugin = plugins[i];
			String placeToGather = getLocation(plugin);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, null);
		}
		for (int i = 0; i < fragments.length; i++) {
			PluginModel fragment = fragments[i];
			String placeToGather = getLocation(fragment);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, null);
		}
		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, null);
		}
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
		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ("." + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ("." + configInfo.toStringReplacingAny(".", ANY_STRING))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	private void generateZipTarget() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_COLLECTING_PLACE) + "/" + DEFAULT_PLUGIN_LOCATION + "/" + plugins[i].getPluginId() + "_" + plugins[i].getVersion()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < fragments.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_COLLECTING_PLACE) + "/" + DEFAULT_PLUGIN_LOCATION + "/" + fragments[i].getId() + "_" + fragments[i].getVersion()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < features.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_COLLECTING_PLACE) + "/" + DEFAULT_FEATURE_LOCATION + "/" + features[i].getVersionedIdentifier().toString()); //$NON-NLS-1$ //$NON-NLS-2$
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		// Zip the root files
		parameters.clear();
		parameters.add("-r -q ${zipargs} " + " ../../" + getPropertyFormat(PROPERTY_ARCHIVE_NAME) + " . "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printExecTask("zip", getPropertyFormat(PROPERTY_FEATURE_BASE) + "/" + configInfo.toStringReplacingAny(".", ANY_STRING) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private void createZipExecCommand(List parameters) {
		parameters.add(0, "-r -q " + getPropertyFormat(PROPERTY_ZIP_ARGS) + " " + getPropertyFormat(PROPERTY_ARCHIVE_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("zip", getPropertyFormat(PROPERTY_BASEDIR) + "/" + getPropertyFormat(PROPERTY_BUILD_LABEL) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected String computeArchiveName() {
		return featureId + "-" + getPropertyFormat(PROPERTY_BUILD_ID_PARAM) + (configInfo.equals(Config.genericConfig()) ? "" : ("-" + configInfo.toStringReplacingAny(".", ANY_STRING))) + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	public void generateTarTarget() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_COLLECTING_PLACE) + "/" + DEFAULT_PLUGIN_LOCATION + "/" + plugins[i].getPluginId() + "_" + plugins[i].getVersion()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (i % parameterSize == 0) {
				createTarExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createTarExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < fragments.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_COLLECTING_PLACE) + "/" + DEFAULT_PLUGIN_LOCATION + "/" + fragments[i].getId() + "_" + fragments[i].getVersion()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (i % parameterSize == 0) {
				createTarExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createTarExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < features.length; i++) {
			parameters.add(getPropertyFormat(PROPERTY_COLLECTING_PLACE) + "/" + DEFAULT_FEATURE_LOCATION + "/" + features[i].getVersionedIdentifier().toString()); //$NON-NLS-1$ //$NON-NLS-2$
			if (i % parameterSize == 0) {
				createTarExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createTarExecCommand(parameters);
			parameters.clear();
		}

		parameters.clear();
		parameters.add("-rf" + " ../../" + getPropertyFormat(PROPERTY_ARCHIVE_NAME) + " . "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printExecTask("tar", getPropertyFormat(PROPERTY_FEATURE_BASE) + "/" + configInfo.toStringReplacingAny(".", ANY_STRING) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE), parameters, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	private void createTarExecCommand(List parameters) {
		parameters.add(0, "-rf " + getPropertyFormat(PROPERTY_ARCHIVE_NAME)); //$NON-NLS-1$
		script.printExecTask("tar", getPropertyFormat(PROPERTY_BASEDIR) + "/" + getPropertyFormat(PROPERTY_BUILD_LABEL) + "/" + getPropertyFormat(PROPERTY_COLLECTING_BASE), parameters, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
