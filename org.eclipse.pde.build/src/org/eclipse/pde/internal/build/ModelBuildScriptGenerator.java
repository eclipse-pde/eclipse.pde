package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.build.ant.*;
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

	// if the model defines its own custom script, we just skip from generating it
	String custom = (String) getBuildProperties(model).get(PROPERTY_CUSTOM);
	if (custom != null && custom.equalsIgnoreCase("true"))
		return;

	try {
		File root = new File(getLocation(model));
		File target = new File(root, buildScriptName);
		script = new BuildAntScript(new FileOutputStream(target));
		setUpAntBuildScript();
		try {
			generateBuildScript();
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
protected void generateBuildScript() throws CoreException {
	generatePrologue();
	generateBuildUpdateJarTarget();
	generateGatherBinPartsTarget();
	generateBuildJarsTarget(script, model);
	generateBuildZipsTarget();
	generateGatherSourcesTarget();
	generateGatherLogTarget();
	generateCleanTarget();
	generateRefreshTarget(script, getPropertyFormat(getModelTypeName()));
	generateEpilogue();
}

protected void generateCleanTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_CLEAN, TARGET_INIT, null, null, null);
	ArrayList jars = new ArrayList(9);
	ArrayList zips = new ArrayList(9);
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	for (int i = 0; i < availableJars.length; i++) {
		String name = getJARLocation(availableJars[i].getName());
		jars.add(name);
		zips.add(getSRCName(name));
	}
	String compiledJars = Utils.getStringFromCollection(jars, ",");
	String sourceZips = Utils.getStringFromCollection(zips, ",");
	String basedir = getPropertyFormat(PROPERTY_BASEDIR);
	List fileSet = new ArrayList(5);
	if (compiledJars.length() > 0) {
		fileSet.add(new FileSet(basedir, null, "*.bin", null, null, null, null));
		fileSet.add(new FileSet(basedir, null, "**/*.log", null, null, null, null));
		fileSet.add(new FileSet(basedir, null, compiledJars, null, null, null, null));
		fileSet.add(new FileSet(basedir, null, sourceZips, null, null, null, null));
	}
	fileSet.add(new FileSet(basedir, null, "**/*.pdetemp", null, null, null, null));
	script.printDeleteTask(tab, null, null, (FileSet[]) fileSet.toArray(new FileSet[fileSet.size()]));
	script.printDeleteTask(tab, null, getModelFileBase() + ".jar", null);
	script.printDeleteTask(tab, null, getModelFileBase() + DEFAULT_FILENAME_SRC, null);
	script.printDeleteTask(tab, null, getModelFileBase() + DEFAULT_FILENAME_LOG, null);
	tab--;
	script.printString(tab, "</target>");
}

protected void generateGatherLogTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_LOGS, TARGET_INIT, null, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	baseDestination = baseDestination.append(getDirectoryName());
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
protected void generateZipIndividualTarget(String zipName, String source) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, zipName, TARGET_INIT, null, null, null);
	IPath root = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printZipTask(tab, root.append(zipName).toString(), root.append(source).toString());
	script.printString(--tab, "</target>");
}


protected void generateGatherSourcesTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	baseDestination = baseDestination.append(getDirectoryName());
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
















protected void generateGatherBinPartsTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	destination = destination.append(getDirectoryName());
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











protected void generateBuildUpdateJarTarget() {
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
	script.printZipTask(tab, destination.append(getModelFileBase() + ".jar").toString(), getPropertyFormat(PROPERTY_BASE) + "/" + getDirectoryName());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}

protected abstract String getDirectoryName();

/**
 * FIXME: there has to be a better name for this method. What does it mean?
 */
protected String getModelFileBase() {
	return "${" + getModelTypeName() + "}_${version}";
}

/**
 * Just ends the script.
 */
protected void generateEpilogue() {
	script.println();
	script.printString(0, "</project>");
}


/**
 * Defines, the XML declaration, Ant project and targets init and initTemplate.
 */
protected void generatePrologue() {
	int tab = 1;
	script.printProjectDeclaration(model.getId(), TARGET_BUILD_JARS, ".");
	script.println();
	script.printProperty(tab, PROPERTY_BUILD_COMPILER, JDT_COMPILER_ADAPTER);
	script.printProperty(tab, PROPERTY_BOOTCLASSPATH, "");
	script.printProperty(tab, PROPERTY_WS, BootLoader.getWS());
	script.printProperty(tab, PROPERTY_OS, BootLoader.getOS());
	script.printProperty(tab, PROPERTY_ARCH, BootLoader.getOSArch());
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, null, null, null, null);
	script.printProperty(tab, getModelTypeName(), model.getId());
	script.printProperty(tab, PROPERTY_VERSION, model.getVersion());
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
protected void generateBuildZipsTarget() throws CoreException {
	StringBuffer zips = new StringBuffer();
	Properties props = getBuildProperties(model);
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
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
	script.printString(--tab, "</target>");
}




/**
 * 
 */
protected void setUpAntBuildScript() throws CoreException {
	String external = (String) getBuildProperties(model).get(PROPERTY_ZIP_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		script.setZipExternal(true);

	external = (String) getBuildProperties(model).get(PROPERTY_JAR_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		script.setJarExternal(true);

	String executable = (String) getBuildProperties(model).get(PROPERTY_ZIP_PROGRAM);
	if (executable != null)
		script.setZipExecutable(executable);
	
	String arg = (String) getBuildProperties(model).get(PROPERTY_ZIP_ARGUMENT);
	if (arg != null)
		script.setZipArgument(arg);
}
}
