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
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.update.core.VersionedIdentifier;

/**
 *
 */
public abstract class AbstractBuildScriptGeneratorTemp extends AbstractScriptGenerator {

	/**
	 * Map of build.properties from the existing plugin models or features.
	 */
	protected Map buildProperties;

	/** build script name */
	protected String buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected String[] devEntries;

	protected class JAR {
		private String name;
		private String[] source;
		
		protected JAR(String name, String[] source) {
			this.name = name;
			this.source = source;
		}
		protected String getName() {
			return name;
		}
		protected String[] getSource() {
			return source;
		}
	}

public AbstractBuildScriptGeneratorTemp() {
	buildProperties = new HashMap();
}

protected String getClasspath(PluginModel model, JAR jar) throws CoreException {
	Set classpath = new HashSet(20);
	String location = getLocation(model);
	// always add boot and runtime
	PluginModel boot = getPlugin(PI_BOOT, null);
	addLibraries(boot, classpath, location);
	addFragmentsLibraries(boot, classpath, location);
	addDevEntries(boot, location, classpath);
	PluginModel runtime = getPlugin(PI_RUNTIME, null);
	addLibraries(runtime, classpath, location);
	addFragmentsLibraries(runtime, classpath, location);
	addDevEntries(runtime, location, classpath);
	// add libraries from pre-requisite plug-ins
	PluginPrerequisiteModel[] requires = model.getRequires();
	if (requires != null) {
		for (int i = 0; i < requires.length; i++) {
			PluginModel prerequisite = getPlugin(requires[i].getPlugin(), requires[i].getVersion());
			addPrerequisiteLibraries(prerequisite, classpath, location);
			addFragmentsLibraries(prerequisite, classpath, location);
			addDevEntries(prerequisite, location, classpath);
		}
	}
	// add libraries from this plug-in
	String jarOrder = (String) getBuildProperties(model).get(PROPERTY_JAR_ORDER);
	if (jarOrder == null) {
		// if no jar order was specified in build.properties, we add all the libraries but the current one
		JAR[] jars = extractJars(getBuildProperties(model));
		for (int i = 0; i < jars.length; i++) {
			if (jar.getName().equals(jars[i].getName()))
				continue;
			classpath.add(jars[i].getName());
		}
		// Add the plug-in libraries that were not declared in build.properties .
		// It usually happens when the library is provided already built.
		LibraryModel[] libraries = model.getRuntime();
		for (int i = 0; i < libraries.length; i++) {
			boolean found = false;
			for (int j = 0; j < jars.length; j++) {
				if (jars[j].getName().equals(libraries[i].getName())) {
					found = true;
					break;
				}
			}
			if (found)
				continue;
			classpath.add(libraries[i].getName());
		}
	} else {
		// otherwise we add all the predecessor jars
		String[] order = Utils.getArrayFromString(jarOrder);
		for (int i = 0; i < order.length; i++) {
			if (order[i].equals(jar.getName()))
				break;
			classpath.add(order[i]);
		}
	}
	return replaceVariables(Utils.getStringFromCollection(classpath, ";"));
}

protected PluginModel getPlugin(String id, String version) throws CoreException {
	PluginModel plugin = getRegistry().getPlugin(id, version);
	if (plugin == null) {
		String pluginName = (version == null) ? id : id + "_" + version;
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", pluginName), null));
	}
	return plugin;
}

protected void addDevEntries(PluginModel model, String baseLocation, Set classpath) throws CoreException {
	if (devEntries == null)
		return;
	IPath root = Utils.makeRelative(new Path(getLocation(model)), new Path(baseLocation));
	for (int i = 0; i < devEntries.length; i++)
		classpath.add(root.append(devEntries[i]));
}

abstract protected PluginRegistryModel getRegistry() throws CoreException;

protected String getLocation(PluginModel model) throws CoreException {
	try {
		return new URL(model.getLocation()).getFile();
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url"), e));
	}
}

protected void addLibraries(PluginModel model, Set classpath, String baseLocation) throws CoreException {
	LibraryModel[] libraries = model.getRuntime();
	if (libraries == null)
		return;
	String root = getLocation(model);
	IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
	for (int i = 0; i < libraries.length; i++) {
		String library = base.append(libraries[i].getName()).toString();
		classpath.add(library);
	}
}



protected void addFragmentsLibraries(PluginModel plugin, Set classpath, String baseLocation) throws CoreException {
	PluginFragmentModel[] fragments = getRegistry().getFragments();
	for (int i = 0; i < fragments.length; i++) {
		if (fragments[i].getPlugin().equals(plugin.getId())) {
			addLibraries(fragments[i], classpath, baseLocation);
			addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath, baseLocation);
		}
	}
}

/**
 * There are cases where the plug-in only declares a library but the real JAR is under
 * a fragment location. This method gets all the plugin libraries and place them in the
 * possible fragment location.
 */
protected void addPluginLibrariesToFragmentLocations(PluginModel plugin, PluginFragmentModel fragment, Set classpath, String baseLocation) throws CoreException {
	LibraryModel[] libraries = plugin.getRuntime();
	if (libraries == null)
		return;
	String root = getLocation(fragment);
	IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
	for (int i = 0; i < libraries.length; i++) {
		String library = base.append(libraries[i].getName()).toString();
		classpath.add(library);
	}
}

protected void addPrerequisiteLibraries(PluginModel prerequisite, Set classpath, String baseLocation) throws CoreException {
	addLibraries(prerequisite, classpath, baseLocation);
	// add libraries (if exported) from pre-requisite plug-ins
	PluginPrerequisiteModel[] requires = prerequisite.getRequires();
	if (requires == null)
		return;
	for (int i = 0; i < requires.length; i++) {
		if (!requires[i].getExport())
			continue;
		PluginModel plugin = getPlugin(requires[i].getPlugin(), requires[i].getVersion());
		addLibraries(plugin, classpath, baseLocation);
		addFragmentsLibraries(plugin, classpath, baseLocation);
		addDevEntries(plugin, baseLocation, classpath);
	}
}



protected Properties getBuildProperties(PluginModel model) throws CoreException {
	VersionedIdentifier identifier = new VersionedIdentifier(model.getId(), model.getVersion());
	Properties result = (Properties) buildProperties.get(identifier);
	if (result == null) {
		result = readBuildProperties(getLocation(model));
		buildProperties.put(identifier, result);
	}
	return result;
}

protected Properties readBuildProperties(String rootLocation) throws CoreException {
	Properties result = new Properties();
	File file = new File(rootLocation, PROPERTIES_FILE);
	if (!file.exists())
		return result; // the file is not required to exist
	try {
		InputStream input = new FileInputStream(file);
		try {
			result.load(input);
		} finally {
			input.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, Policy.bind("exception.readingFile"), e));
	}
	return result;
}

protected JAR[] extractJars(Properties properties) {
	List result = new ArrayList(5);
	int n = PROPERTY_SOURCE_PREFIX.length();
	for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (!(key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_JAR_SUFFIX)))
			continue;
		key = key.substring(n);
		String[] source = Utils.getArrayFromString((String) entry.getValue());
		JAR jar = new JAR(key, source);
		result.add(jar);
	}
	return (JAR[]) result.toArray(new JAR[result.size()]);
}

protected void generateBuildJarsTarget(AntScript script, PluginModel model) throws CoreException {
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	List jarNames = new ArrayList(availableJars.length);
	List srcNames = new ArrayList(availableJars.length);
	Map jars = new HashMap(availableJars.length);
	for (int i = 0; i < availableJars.length; i++)
		jars.put(availableJars[i].getName(), availableJars[i]);
	String jarOrder = (String) getBuildProperties(model).get(PROPERTY_JAR_ORDER);
	if (jarOrder != null) {
		String[] order = Utils.getArrayFromString(jarOrder);
		for (int i = 0; i < order.length; i++) {
			JAR jar = (JAR) jars.get(order[i]);
			if (jar == null)
				continue;
			String name = jar.getName();
			jarNames.add(name);
			generateJARTarget(script, getClasspath(model, jar), jar);
			generateSRCTarget(script, jar);
			srcNames.add(getSRCName(name));
			jars.remove(order[i]);
		}
	}
	for (Iterator iterator = jars.values().iterator(); iterator.hasNext();) {
		JAR jar = (JAR) iterator.next();
		String name = jar.getName();
		jarNames.add(name);
		generateJARTarget(script, getClasspath(model, jar), jar);
		generateSRCTarget(script, jar);
		srcNames.add(getSRCName(name));
	}
	script.println();
	script.printTargetDeclaration(1, TARGET_BUILD_JARS, Utils.getStringFromCollection(jarNames, ","), null, null, null);
	script.printEndTag(1, "target");
	script.println();
	script.printTargetDeclaration(1, TARGET_BUILD_SOURCES, Utils.getStringFromCollection(srcNames, ","), null, null, null);
	script.printEndTag(1, "target");
}

protected void generateJARTarget(AntScript script, String classpath, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName();
	script.printTargetDeclaration(tab++, name, null, null, null, null);
	String destdir = getTempJARFolderLocation(name);
	script.printProperty(tab, "destdir", destdir);
	script.printDeleteTask(tab, destdir, null, null);
	script.printMkdirTask(tab, destdir);
	script.printComment(tab, "compile the source code");
	JavacTask javac = new JavacTask();
	javac.setClasspath(classpath);
	javac.setDestdir(destdir);
	javac.setFailOnError("false");
	javac.setDebug("on");
	javac.setVerbose("true");
	javac.setIncludeAntRuntime("no");
	String[] sources = jar.getSource();
	javac.setSrcdir(sources);
	script.print(tab, javac);
	script.printComment(tab, "copy necessary resources");
	FileSet[] fileSets = new FileSet[sources.length];
	for (int i = 0; i < sources.length; i++) {
		fileSets[i] = new FileSet(sources[i], null, null, null, "**/*.java", null, null);
	}
	script.printCopyTask(tab, null, destdir, fileSets);
	script.printJarTask(tab, getJARLocation(name), destdir);
	script.printDeleteTask(tab, destdir, null, null);
	script.printEndTag(--tab, "target");
}


protected void generateSRCTarget(AntScript script, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName();
	String zip = getSRCName(name);
	script.printTargetDeclaration(tab++, zip, null, null, null, null);
	String[] sources = jar.getSource();
	FileSet[] fileSets = new FileSet[sources.length];
	for (int i = 0; i < sources.length; i++) {
		fileSets[i] = new FileSet(sources[i], null, "**/*.java", null, null, null, null);
	}
	script.printZipTask(tab, zip, null, fileSets);
	script.printEndTag(--tab, "target");
}

protected String getSRCName(String jarName) {
	return jarName.substring(0, jarName.length() - 4) + "src.zip";
}

protected String getJARLocation(String jarName) {
	String basedir = getPropertyFormat(PROPERTY_BASEDIR);
	IPath destination = new Path(basedir);
	destination = destination.append(jarName);
	return destination.toString();
}

protected String getTempJARFolderLocation(String jarName) {
	String basedir = getPropertyFormat(PROPERTY_BASEDIR);
	IPath destination = new Path(basedir);
	destination = destination.append(jarName + ".bin");
	return destination.toString();
}

/**
 * Substitute the value of an element description variable (variables that
 * are found in files like plugin.xml, e.g. $ws$) by an Ant property.
 */
protected String replaceVariables(String sourceString) {
	int i = -1;
	String result = sourceString;
	while ((i = result.indexOf(DESCRIPTION_VARIABLE_WS)) >= 0)
		result = result.substring(0, i) + "ws/" + getPropertyFormat(PROPERTY_WS) + result.substring(i + DESCRIPTION_VARIABLE_WS.length());
	while ((i = result.indexOf(DESCRIPTION_VARIABLE_OS)) >= 0)
		result = result.substring(0, i) + "os/" + getPropertyFormat(PROPERTY_OS) + result.substring(i + DESCRIPTION_VARIABLE_OS.length());
	while ((i = result.indexOf(DESCRIPTION_VARIABLE_NL)) >= 0)
		result = result.substring(0, i) + "nl/" + getPropertyFormat(PROPERTY_NL) + result.substring(i + DESCRIPTION_VARIABLE_NL.length());
	return result;
}

public void setDevEntries(String[] entries) {
	this.devEntries = entries;
}

/**
 * Sets the buildScriptName.
 */
public void setBuildScriptName(String buildScriptName) {
	if (buildScriptName == null)
		this.buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;
	else
		this.buildScriptName = buildScriptName;
}

}
