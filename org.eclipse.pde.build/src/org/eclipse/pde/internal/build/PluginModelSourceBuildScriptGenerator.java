/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.build.AbstractBuildScriptGeneratorTemp.JAR;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;

/**
 * Given a set of plug-ins and fragments, generate their build scripts.
 */
public class PluginModelSourceBuildScriptGenerator extends AbstractBuildScriptGeneratorTemp {

	protected PluginRegistryModel registry;
	protected String sourceLocation;

public PluginModelSourceBuildScriptGenerator() {
	super();
}

/**
 * @see AbstractScriptGenerator#generate()
 */
public void generate() throws CoreException {
	PluginModel[] plugins = getRegistry().getPlugins();
	generate(plugins);
	PluginModel[] fragments = getRegistry().getFragments();
	generate(fragments);
	generateMainScript(plugins, fragments);
}

protected void generate(PluginModel[] models) throws CoreException {
	for (int i = 0; i < models.length; i++)
		generate(models[i]);
}

protected void generateMainScript(PluginModel[] plugins, PluginModel[] fragments) throws CoreException {
	String location = getMainScriptLocation();
	try {
		FileOutputStream output = new FileOutputStream(location);
		AntScript script = new AntScript(output);
		try {
			generateMainScript(script, plugins, fragments);
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, Policy.bind("exception.writingFile", location), e));
	}
}

protected void generateMainScript(AntScript script, PluginModel[] plugins, PluginModel[] fragments) throws CoreException {
	script.printProjectDeclaration("Source Build", TARGET_MAIN, ".");
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_MAIN, null, null, null, null);
	script.printProperty(tab, PROPERTY_TARGET, TARGET_BUILD_JARS);
	script.printProperty(tab, PROPERTY_INSTALL_LOCATION, "${basedir}/../install");
	script.println();
	String target = getPropertyFormat(PROPERTY_TARGET);
	PluginModel[] models = getCompileOrder(plugins, fragments);
	for (int i = 0; i < models.length; i++) {
		String location = getLocation(models[i]);
		script.printEchoTask(tab, "===========  " + models[i].getId() + "  ===========");
		script.printAntTask(tab, buildScriptName, location, target, null, null, null);
	}
	script.printEndTag(--tab, TARGET_TARGET);
	script.printEndTag(--tab, "project");
}

protected PluginModel[] getCompileOrder(PluginModel[] plugins, PluginModel[] fragments) throws CoreException {
	List result = new ArrayList(50);
	String[][] sortedPlugins = Utils.computePrerequisiteOrder(plugins);
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			String pluginId = sortedPlugins[list][i];
			result.add(getRegistry().getPlugin(pluginId));
			for (int j = 0; j < fragments.length; j++) {
				if (fragments[j].getPluginId().equals(pluginId))
					result.add(fragments[j]);
			}
		}
	}
	return (PluginModel[]) result.toArray(new PluginModel[result.size()]);
}



protected void generate(PluginModel model) throws CoreException {
	String custom = (String) getBuildProperties(model).get(PROPERTY_CUSTOM);
	if (custom != null && custom.equalsIgnoreCase("true"))
		return;
	String location = getScriptLocation(model);
	try {
		FileOutputStream output = new FileOutputStream(location);
		AntScript script = new AntScript(output);
		try {
			generate(script, model);
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, Policy.bind("exception.writingFile", location), e));
	}
}



/**
 * Main call for generating the script.
 */
protected void generate(AntScript script, PluginModel model) throws CoreException {
	generatePrologue(script, model);
//	generateBuildUpdateJarTarget();
	generateBuildJarsTarget(script, model);
	generateInstallTarget(script, model);
//	generateBuildZipsTarget();
//	generateGatherSourcesTarget();
//	generateGatherLogTarget();
	generateCleanTarget(script, model);
//	generatePropertiesTarget();
	generateEpilogue(script);
}

protected void generateCleanTarget(AntScript script, PluginModel model) throws CoreException {
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	script.println();
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_CLEAN, null, null, null, null);
	for (int i = 0; i < availableJars.length; i++) {
		String name = getJARLocation(availableJars[i].getName());
		script.printDeleteTask(tab, null, name, null);
		script.printDeleteTask(tab, null, getSRCName(name), null);
		script.printDeleteTask(tab, getTempJARFolderLocation(name), null, null);
	}
	script.printEndTag(--tab, "target");
}

protected String getInstallFolderLocation(PluginModel model) {
	IPath destination = new Path(getPropertyFormat(PROPERTY_INSTALL_LOCATION));
	destination = destination.append("plugins");
	destination = destination.append(model.getId() + "_" + model.getVersion());
	return destination.toString();
}

/**
 * Just ends the script.
 */
protected void generateEpilogue(AntScript script) {
	script.println();
	script.printEndTag(0, "project");
}

/**
 * Defines, the XML declaration, Ant project and targets init and initTemplate.
 */
protected void generatePrologue(AntScript script, PluginModel model) {
	script.printProjectDeclaration(model.getId(), TARGET_BUILD_JARS, ".");
}


protected String getScriptLocation(PluginModel model) throws CoreException {
	File file = new File(getLocation(model), buildScriptName);
	return file.getAbsolutePath();
}

protected PluginRegistryModel getRegistry() throws CoreException {
	if (registry == null) {
		URL[] pluginPath = getPluginPath();
		MultiStatus problems = new MultiStatus(PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null);
		Factory factory = new Factory(problems);
		registry = Platform.parsePlugins(pluginPath, factory);
		IStatus status = factory.getStatus();
		if (Utils.contains(status, IStatus.ERROR))
			throw new CoreException(status);
	}
	return registry;
}

protected URL[] getPluginPath() throws CoreException {
	if (sourceLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_SOURCE_LOCATION_MISSING, Policy.bind("error.missingSourceLocation"), null));
	try {
		File file = new File(sourceLocation, "plugins/");
		return new URL[] {
			file.toURL()
		};
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url"), null));
	}
}


protected String getMainScriptLocation() throws CoreException {
	if (sourceLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_SOURCE_LOCATION_MISSING, Policy.bind("error.missingSourceLocation"), null));
	File file = new File(sourceLocation, buildScriptName);
	return file.getAbsolutePath();
}
	
public void setSourceLocation(String location) {
	this.sourceLocation = location;
}

protected void generateInstallTarget(AntScript script, PluginModel model) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INSTALL, null, null, null, null);
	String root = getInstallFolderLocation(model);
	script.printMkdirTask(tab, root);
	String include = (String) getBuildProperties(model).get(PROPERTY_BIN_INCLUDES);
	if (include != null) {
		FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASEDIR), null, include, null, null, null, null);
		script.printCopyTask(tab, null, root, new FileSet[]{ fileSet });
	}
	script.printEndTag(--tab, "target");
}

}