/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
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

	/** constants */
	protected static final String FULL_NAME = getPropertyFormat(PROPERTY_FULL_NAME);
	protected static final String PLUGIN_DESTINATION = getPropertyFormat(PROPERTY_PLUGIN_DESTINATION);
	protected static final String PLUGIN_ZIP_DESTINATION = PLUGIN_DESTINATION + "/" + FULL_NAME + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
	protected static final String PLUGIN_UPDATE_JAR_DESTINATION = PLUGIN_DESTINATION + "/" + FULL_NAME + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$


/**
 * @see AbstractScriptGenerator#generate()
 */
public void generate() throws CoreException {
	if (model == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null)); //$NON-NLS-1$

	try {
		// if the model defines its own custom script, we do not generate a new one
		// but we do try to update the version number
		String custom = (String) getBuildProperties(model).get(PROPERTY_CUSTOM);
		if (custom != null && custom.equalsIgnoreCase("true")) { //$NON-NLS-1$
			String root = getLocation(model);
			File buildFile = new File(root, buildScriptName);
			updateVersion(buildFile, PROPERTY_VERSION_SUFFIX, model.getVersion());
			return;
		}
		String targetLocation = getScriptTargetLocation();
		if (targetLocation==null) targetLocation = getLocation(model);
		File root = new File(targetLocation);
		File target = new File(root, buildScriptName);
		AntScript script = new AntScript(new FileOutputStream(target));
		try {
			generateBuildScript(script);
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e)); //$NON-NLS-1$
	}
}

/**
 * Main call for generating the script.
 * 
 * @param script the script to generate
 * @throws CoreException
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
	generateRefreshTarget(script);
	generateZipPluginTarget(script);
	generateZipFolderTarget(script);
	generateEpilogue(script);
}

/**
 * @param script
 */
private void generateZipFolderTarget(AntScript script) {
	script.printTargetDeclaration(1, TARGET_ZIP_FOLDER, TARGET_INIT, null, null, null);
	script.printZipTask(2, PLUGIN_ZIP_DESTINATION, TEMP_FOLDER, true, "**/*.bin.log", false, null); //$NON-NLS-1$
	script.printTargetEnd(1);
}

/**
 * Add the <code>clean</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @throws CoreException
 */
protected void generateCleanTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	script.printTargetDeclaration(tab++, TARGET_CLEAN, TARGET_INIT, null, null, Policy.bind("build.plugin.clean", model.getId()));  //$NON-NLS-1$
	for (int i = 0; i < availableJars.length; i++) {
		String jarName = availableJars[i].getName(true);
		script.printDeleteTask(tab, null, getJARLocation(jarName), null);
		script.printDeleteTask(tab, null, getSRCLocation(jarName), null);
	}
	script.printDeleteTask(tab, null, PLUGIN_UPDATE_JAR_DESTINATION, null);
	script.printDeleteTask(tab, null, PLUGIN_ZIP_DESTINATION, null);
	script.printDeleteTask(tab, TEMP_FOLDER, null, null);
	script.printTargetEnd(--tab);
}

/**
 * Add the <code>gather.logs</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @throws CoreException
 */
protected void generateGatherLogTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_LOGS, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
	baseDestination = baseDestination.append(FULL_NAME);
	List destinations = new ArrayList(5);
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	for (int i = 0; i < availableJars.length; i++) {
		String name = availableJars[i].getName(true);
		IPath destination = baseDestination.append(name).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(destination)) {
			script.printMkdirTask(tab, destination.toString());
			destinations.add(destination);
		}
		script.printCopyTask(tab, getTempJARFolderLocation(name) + ".log", destination.toString(), null); //$NON-NLS-1$
	}
	script.printTargetEnd(--tab);
}

/**
 * 
 * @param script
 * @param zipName
 * @param source
 * @throws CoreException
 */
protected void generateZipIndividualTarget(AntScript script, String zipName, String source) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, zipName, TARGET_INIT, null, null, null);
	IPath root = new Path(BASEDIR);
	script.printZipTask(tab, root.append(zipName).toString(), root.append(source).toString(), false, null, false, null);
	script.printTargetEnd(--tab);
}

/**
 * Add the <code>gather.sources</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @throws CoreException
 */
protected void generateGatherSourcesTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
	baseDestination = baseDestination.append(FULL_NAME);
	List destinations = new ArrayList(5);
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	for (int i = 0; i < availableJars.length; i++) {
		String jar = availableJars[i].getName(true);
		IPath destination = baseDestination.append(jar).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(destination)) {
			script.printMkdirTask(tab, destination.toString());
			destinations.add(destination);
		}
		script.printCopyTask(tab, getSRCLocation(jar), destination.toString(), null);
	}
	String include = (String) getBuildProperties(model).get(PROPERTY_SRC_INCLUDES);
	String exclude = (String) getBuildProperties(model).get(PROPERTY_SRC_EXCLUDES);
	if (include != null || exclude != null) {
		FileSet fileSet = new FileSet(BASEDIR, null, include, null, exclude, null, null);
		script.printCopyTask(tab, null, baseDestination.toString(), new FileSet[]{ fileSet });
	}
	script.printTargetEnd(--tab);
}

/**
 * Add the <code>gather.bin.parts</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @throws CoreException
 */
protected void generateGatherBinPartsTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
	IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
	destination = destination.append(FULL_NAME);
	String root = destination.toString();
	script.printMkdirTask(tab, root);
	List destinations = new ArrayList(5);
	destinations.add(destination);
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	for (int i = 0; i < availableJars.length; i++) {
		String jar = availableJars[i].getName(true);
		IPath dest = destination.append(jar).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(dest)) {
			script.printMkdirTask(tab, dest.toString());
			destinations.add(dest);
		}
		script.printCopyTask(tab, getJARLocation(jar), dest.toString(), null);
	}
	String include = (String) getBuildProperties(model).get(PROPERTY_BIN_INCLUDES);
	String exclude = (String) getBuildProperties(model).get(PROPERTY_BIN_EXCLUDES);
	if (include != null || exclude != null) {
		FileSet fileSet = new FileSet(BASEDIR, null, include, null, exclude, null, null);
		script.printCopyTask(tab, null, root, new FileSet[]{ fileSet });
	}
	script.printTargetEnd(--tab);
}

/**
 * Add the <code>zip.plugin</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @throws CoreException
 */
protected void generateZipPluginTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_PLUGIN, TARGET_INIT, null, null, Policy.bind("build.plugin.zipPlugin", model.getId())); //$NON-NLS-1$ 
	script.printDeleteTask(tab, TEMP_FOLDER, null, null);
	script.printMkdirTask(tab, TEMP_FOLDER);
	script.printAntCallTask(tab, TARGET_BUILD_JARS, null, null);
	script.printAntCallTask(tab, TARGET_BUILD_SOURCES, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION_TEMP_FOLDER, TEMP_FOLDER + "/"); //$NON-NLS-1$
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printAntCallTask(tab, TARGET_GATHER_SOURCES, null, params);
	FileSet fileSet = new FileSet(TEMP_FOLDER, null, "**/*.bin.log", null, null, null, null); //$NON-NLS-1$
	script.printDeleteTask(tab, null, null, new FileSet[] {fileSet});
	script.printAntCallTask(tab, TARGET_ZIP_FOLDER,null, null);
	script.printDeleteTask(tab, TEMP_FOLDER, null, null);
	script.printTargetEnd(--tab);
}

/**
 * Add the <code>build.update.jar</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 */
protected void generateBuildUpdateJarTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, Policy.bind("build.plugin.buildUpdateJar", model.getId())); //$NON-NLS-1$
	script.printDeleteTask(tab, TEMP_FOLDER, null, null);
	script.printMkdirTask(tab, TEMP_FOLDER);
	script.printAntCallTask(tab, TARGET_BUILD_JARS, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION_TEMP_FOLDER, TEMP_FOLDER + "/"); //$NON-NLS-1$
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printZipTask(tab, PLUGIN_UPDATE_JAR_DESTINATION, TEMP_FOLDER + "/" + FULL_NAME, false, null, false, null); //$NON-NLS-1$
	script.printDeleteTask(tab, TEMP_FOLDER, null, null);
	script.printTargetEnd(--tab);
}

/**
 * Add the <code>refresh</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 */
protected void generateRefreshTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_REFRESH, TARGET_INIT, PROPERTY_ECLIPSE_RUNNING, null, null);
	script.printConvertPathTask(tab, new Path(getLocation(model)).removeLastSegments(0).toOSString(), PROPERTY_RESOURCE_PATH, false);
	script.printRefreshLocalTask(tab, getPropertyFormat(PROPERTY_RESOURCE_PATH), "infinite"); //$NON-NLS-1$
	script.printTargetEnd(--tab);
}

/**
 * End the script by closing the project element.
 * 
 * @param script the script to end
 */
protected void generateEpilogue(AntScript script) {
	script.println();
	script.printProjectEnd();
}

/**
 * Defines, the XML declaration, Ant project and targets init and initTemplate.
 * 
 * @param script the script to begin
 */
protected void generatePrologue(AntScript script) {
	int tab = 1;
	script.printProjectDeclaration(model.getId(), TARGET_BUILD_JARS, "."); //$NON-NLS-1$
	script.println();
	script.printProperty(tab, PROPERTY_BOOTCLASSPATH, ""); //$NON-NLS-1$
	script.printProperty(tab, PROPERTY_WS, BootLoader.getWS());
	script.printProperty(tab, PROPERTY_OS, BootLoader.getOS());
	script.printProperty(tab, PROPERTY_ARCH, BootLoader.getOSArch());
	script.printProperty(tab, PROPERTY_JAVAC_FAIL_ON_ERROR, "false");	//$NON-NLS-1$
	script.printProperty(tab, PROPERTY_JAVAC_DEBUG_INFO, "on");	//$NON-NLS-1$
	script.printProperty(tab, PROPERTY_JAVAC_VERBOSE, "true");	//$NON-NLS-1$
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, TARGET_PROPERTIES, null, null, null);
	script.printProperty(tab, getModelTypeName(), model.getId());
	script.printProperty(tab, PROPERTY_VERSION_SUFFIX, "_" + model.getVersion()); //$NON-NLS-1$
	script.printProperty(tab, PROPERTY_FULL_NAME, getPropertyFormat(getModelTypeName()) + getPropertyFormat(PROPERTY_VERSION_SUFFIX));
	script.printProperty(tab, PROPERTY_TEMP_FOLDER, BASEDIR + "/" + PROPERTY_TEMP_FOLDER); //$NON-NLS-1$
	script.printProperty(tab, PROPERTY_PLUGIN_DESTINATION, BASEDIR);
	script.printProperty(tab, PROPERTY_BUILD_RESULT_FOLDER, BASEDIR);
	script.printTargetEnd(--tab);
	script.println();
	script.printTargetDeclaration(tab++, TARGET_PROPERTIES, null, PROPERTY_ECLIPSE_RUNNING, null, null);
	script.printProperty(tab, PROPERTY_BUILD_COMPILER, JDT_COMPILER_ADAPTER);
	script.printTargetEnd(--tab);
}

/**
 * 
 * @return String
 */
protected abstract String getModelTypeName();

/**
 * Sets the PluginModel to generate script from.
 * 
 * @param model
 * @throws CoreException
 */
public void setModel(PluginModel model) throws CoreException {
	if (model == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null)); //$NON-NLS-1$
	this.model = model;
}

/**
 * Sets model to generate scripts from.
 * 
 * @param modelId
 * @throws CoreException
 */
public void setModelId(String modelId) throws CoreException {
	PluginModel newModel = getModel(modelId);
	if (newModel == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("exception.missingElement", modelId), null)); //$NON-NLS-1$
	setModel(newModel);
}

/**
 * 
 * @param modelId
 * @return PluginModel
 * @throws CoreException
 */
protected abstract PluginModel getModel(String modelId) throws CoreException;

/**
 * Add the <code>build.zips</code> target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @throws CoreException
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
	script.printTargetDeclaration(2, TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
	script.printTargetEnd(2);
}

}