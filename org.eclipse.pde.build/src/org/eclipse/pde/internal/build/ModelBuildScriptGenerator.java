/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.AbstractBuildScriptGenerator.JAR;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;
/**
 * Generic class for generating scripts for plug-ins and fragments.
 */
public abstract class ModelBuildScriptGenerator extends AbstractBuildScriptGenerator {

	/**
	 * PluginModel to generate script from.
	 */
	protected PluginModel model;

/**
 * @see AbstractScriptGenerator#generate()
 */
public void generate() throws CoreException {
	if (model == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));

	try {
		// if the model defines its own custom script, we do not generate a new one
		// but we do try to update the version number
		String custom = (String) getBuildProperties(model).get(PROPERTY_CUSTOM);
		if (custom != null && custom.equalsIgnoreCase("true")) {
			String root = getLocation(model);
			File buildFile = new File(root, buildScriptName);
			updateVersion(buildFile, PROPERTY_VERSION_SUFFIX, model.getVersion());
			return;
		}

		File root = new File(getLocation(model));
		File target = new File(root, buildScriptName);
		AntScript script = new AntScript(new FileOutputStream(target));
		try {
			generateBuildScript(script);
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
}

/**
 * Main call for generating the script.
 */
protected void generateBuildScript(AntScript script) throws CoreException {
	generatePrologue(script);
	generateBuildUpdateJarTarget(script);
	generateGatherBinPartsTarget(script);
	generateBuildJarsTarget(script, model);
	generateBuildZipsTarget(script);
	generateGatherSourcesTarget(script);
	generateGatherLogTarget(script);
	generateCleanTarget(script);
	generateRefreshTarget(script, getPropertyFormat(getModelTypeName()));
	generateZipPluginTarget(script, model);
	generateEpilogue(script);
}

protected void generateCleanTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	IPath basedir = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	script.printTargetDeclaration(tab++, TARGET_CLEAN, TARGET_INIT, null, null, null);
	for (int i = 0; i < availableJars.length; i++) {
		String jarName = availableJars[i].getName();
		String name = getJARLocation(jarName);
		script.printDeleteTask(tab, null, name, null);
		script.printDeleteTask(tab, null, getLogName(name), null);
		script.printDeleteTask(tab, null, getSRCName(name), null);
		script.printDeleteTask(tab, getTempJARFolderLocation(jarName), null, null);
	}
	script.printDeleteTask(tab, null, basedir.append(getModelFullName() + ".jar").toString(), null);
	script.printDeleteTask(tab, null, basedir.append(getModelFullName() + ".zip").toString(), null);
	script.printDeleteTask(tab, null, basedir.append(getModelFullName() + DEFAULT_FILENAME_SRC).toString(), null);
	script.printDeleteTask(tab, null, basedir.append(getModelFullName() + DEFAULT_FILENAME_LOG).toString(), null);
	script.printString(--tab, "</target>");
}

protected void generateGatherLogTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_LOGS, TARGET_INIT, null, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	baseDestination = baseDestination.append(getModelFullName());
	List destinations = new ArrayList(5);
	IPath baseSource = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	for (int i = 0; i < availableJars.length; i++) {
		String name = availableJars[i].getName();
		IPath destination = baseDestination.append(name).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(destination)) {
			script.printMkdirTask(tab, destination.toString());
			destinations.add(destination);
		}
		script.printCopyTask(tab, baseSource.append(name + ".bin.log").toString(), destination.toString(), null);
	}
	script.printEndTag(--tab, TARGET_TARGET);
}




/**
 * FIXME: add comments
 */
protected void generateZipIndividualTarget(AntScript script, String zipName, String source) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, zipName, TARGET_INIT, null, null, null);
	IPath root = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printZipTask(tab, root.append(zipName).toString(), root.append(source).toString(), null);
	script.printString(--tab, "</target>");
}


protected void generateGatherSourcesTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	baseDestination = baseDestination.append(getModelFullName());
	List destinations = new ArrayList(5);
	IPath baseSource = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	for (int i = 0; i < availableJars.length; i++) {
		String jar = availableJars[i].getName();
		String zip = getSRCName(jar);
		IPath destination = baseDestination.append(jar).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(destination)) {
			script.printMkdirTask(tab, destination.toString());
			destinations.add(destination);
		}
		script.printCopyTask(tab, baseSource.append(zip).toString(), destination.toString(), null);
	}
	script.printString(--tab, "</target>");
}
















protected void generateGatherBinPartsTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	destination = destination.append(getModelFullName());
	String root = destination.toString();
	script.printMkdirTask(tab, root);
	String include = (String) getBuildProperties(model).get(PROPERTY_BIN_INCLUDES);
	String exclude = (String) getBuildProperties(model).get(PROPERTY_BIN_EXCLUDES);
	if (include != null || exclude != null) {
		FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
		script.printCopyTask(tab, null, root, new FileSet[]{ fileSet });
	}
	script.printEndTag(--tab, "target");
}

protected void generateZipPluginTarget(AntScript script, PluginModel model) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_PLUGIN, TARGET_INIT, null, null, null);
	IPath basedir = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	IPath destination = basedir.append("bin.zip.pdetemp");
	script.printProperty(tab, PROPERTY_BASE, destination.toString());
	script.printDeleteTask(tab, destination.toString(), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_BASE));
	script.printAntCallTask(tab, TARGET_BUILD_JARS, null, null);
	script.printAntCallTask(tab, TARGET_BUILD_SOURCES, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_BASE) + "/");
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printAntCallTask(tab, TARGET_GATHER_SOURCES, null, params);
	FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASE), null, "**/*.bin.log", null, null, null, null);
	script.printDeleteTask(tab, null, null, new FileSet[] {fileSet});
	script.printZipTask(tab, basedir.append(getModelFullName() + ".zip").toString(), destination.toString(), null);
	script.printDeleteTask(tab, destination.toString(), null, null);
	script.printString(--tab, "</target>");
}











protected void generateBuildUpdateJarTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_BASE));
	script.printAntCallTask(tab, TARGET_BUILD_JARS, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_BASE) + "/");
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASE), null, "**/*.bin.log", null, null, null, null);
	script.printDeleteTask(tab, null, null, new FileSet[] {fileSet});
	script.printZipTask(tab, destination.append(getModelFullName() + ".jar").toString(), getPropertyFormat(PROPERTY_BASE) + "/" + getModelFullName(), null);
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}


protected String getModelFullName() {
	return getPropertyFormat(PROPERTY_FULL_NAME);
}

/**
 * Just ends the script.
 */
protected void generateEpilogue(AntScript script) {
	script.println();
	script.printString(0, "</project>");
}


/**
 * Defines, the XML declaration, Ant project and targets init and initTemplate.
 */
protected void generatePrologue(AntScript script) {
	int tab = 1;
	script.printProjectDeclaration(model.getId(), TARGET_BUILD_JARS, ".");
	script.println();
	script.printProperty(tab, PROPERTY_BOOTCLASSPATH, "");
	script.printProperty(tab, PROPERTY_WS, BootLoader.getWS());
	script.printProperty(tab, PROPERTY_OS, BootLoader.getOS());
	script.printProperty(tab, PROPERTY_ARCH, BootLoader.getOSArch());
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, TARGET_PROPERTIES, null, null, null);
	script.printProperty(tab, getModelTypeName(), model.getId());
	script.printProperty(tab, PROPERTY_VERSION_SUFFIX, "_" + model.getVersion());
	script.printProperty(tab, PROPERTY_FULL_NAME, getPropertyFormat(getModelTypeName()) + getPropertyFormat(PROPERTY_VERSION_SUFFIX));
	script.printString(--tab, "</target>");
	script.println();
	script.printTargetDeclaration(tab++, TARGET_PROPERTIES, null, PROPERTY_ECLIPSE_RUNNING, null, null);
	script.printProperty(tab, PROPERTY_BUILD_COMPILER, JDT_COMPILER_ADAPTER);
	script.printString(--tab, "</target>");
}

protected abstract String getModelTypeName();

/**
 * Sets the PluginModel to generate script from.
 */
public void setModel(PluginModel model) throws CoreException {
	if (model == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));
	this.model = model;
}

/**
 * Sets model to generate scripts from.
 */
public void setModelId(String modelId) throws CoreException {
	PluginModel newModel = getModel(modelId);
	if (newModel == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("exception.missingElement", modelId), null));
	setModel(newModel);
}

protected abstract PluginModel getModel(String modelId) throws CoreException;


/**
 * FIXME: add comments
 */
protected void generateBuildZipsTarget(AntScript script) throws CoreException {
	StringBuffer zips = new StringBuffer();
	Properties props = getBuildProperties(model);
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_ZIP_SUFFIX)) {
			String zipName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			zips.append(',');
			zips.append(zipName);
			generateZipIndividualTarget(script, zipName, (String) entry.getValue());
		}
	}
	script.println();
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
	script.printString(--tab, "</target>");
}




}
