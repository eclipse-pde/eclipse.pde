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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.model.LibraryModel;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.JavacTask;
import org.eclipse.update.core.VersionedIdentifier;

/**
 * Given a set of plug-ins and fragments, generate their build scripts.
 */
public class PluginModelSourceBuildScriptGenerator extends AbstractScriptGenerator {

	protected PluginRegistryModel registry;
	protected Map buildProperties;
	protected String scriptName = DEFAULT_BUILD_SCRIPT_FILENAME;
	protected String sourceLocation;

	protected class JAR {
		private String name;
		private String[] source;
		
		JAR(String name, String[] source) {
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

public PluginModelSourceBuildScriptGenerator() {
	buildProperties = new HashMap();
}

/**
 * @see AbstractScriptGenerator#generate()
 */
public void generate() throws CoreException {
	PluginModel[] plugins = getRegistry().getPlugins();
	generate(plugins);
	PluginModel[] fragments = getRegistry().getFragments();
	generate(fragments);
	generateLibrary();
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
	PluginModel[] models = getCompileOrder(plugins, fragments);
	for (int i = 0; i < models.length; i++) {
		String location = getLocation(models[i]);
		script.printAntTask(tab, scriptName, location, null, null, null, null);
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

protected String makeRelative(String location, IPath base) {
	IPath path = new Path(location);
	if (path.getDevice() != null && !path.getDevice().equalsIgnoreCase(base.getDevice()))
		return location.toString();
	int baseCount = base.segmentCount();
	int count = base.matchingFirstSegments(path);
	if (count > 0) {
		String temp = "";
		for (int j = 0; j < baseCount - count; j++)
			temp += "../";
		path = new Path(temp).append(path.removeFirstSegments(count));
	}
	return path.toString();
}


protected void generate(PluginModel model) throws CoreException {
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

protected void generateLibrary() throws CoreException {
	String location = getLibraryLocation();
	try {
		URL library = new URL(Platform.getPlugin(PI_PDEBUILD).getDescriptor().getInstallURL(), LIBRARY_FILE);
		InputStream input = library.openStream();
		FileOutputStream output = new FileOutputStream(location);
		transferStreams(input, output);
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, Policy.bind("exception.writingFile", location), e));
	}
}

/**
 * Transfers all available bytes from the given input stream to the given output stream. 
 * Regardless of failure, this method closes both streams.
 */
protected void transferStreams(InputStream source, OutputStream destination)	throws IOException {
	try {
		byte[] buffer = new byte[8192];
		while (true) {
			int bytesRead = -1;
			if ((bytesRead = source.read(buffer)) == -1)
				break;
			destination.write(buffer, 0, bytesRead);
		}
	} finally {
		try {
			source.close();
		} catch (IOException e) {
		}
		try {
			destination.close();
		} catch (IOException e) {
		}
	}
}

/**
 * Main call for generating the script.
 */
protected void generate(AntScript script, PluginModel model) throws CoreException {
	generatePrologue(script, model);
//	generateBuildUpdateJarTarget();
//	generateGatherBinPartsTarget();
	generateBuildJarsTarget(script, model);
//	generateBuildZipsTarget();
//	generateGatherSourcesTarget();
//	generateBuildSourcesTarget();
//	generateGatherLogTarget();
//	generateCleanTarget();
//	generatePropertiesTarget();
	generateEpilogue(script);
}

protected void generateBuildJarsTarget(AntScript script, PluginModel model) throws CoreException {
	Properties properties = getBuildProperties(model);
	JAR[] jars = extractJars(properties);
	List jarNames = new ArrayList(jars.length);
	// FIXME: have to make sure the ordering is correct
	// maybe a new build.properties entry:
	// build.jar.order = jar1.jar, jar2.jar, jar3.jar
	for (int i = 0; i < jars.length; i++) {
		String name = jars[i].name;
		jarNames.add(name);
		generateJARTarget(script, model, jars[i]);
	}
	script.println();
	script.printTargetDeclaration(1, TARGET_BUILD_JARS, Utils.getStringFromCollection(jarNames, ","), null, null, null);
	script.printEndTag(1, "target");
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

protected void generateJARTarget(AntScript script, PluginModel model, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName();
	script.printTargetDeclaration(tab++, name, null, null, null, null);
	String basedir = getPropertyFormat(PROPERTY_BASEDIR);
	IPath destination = new Path(basedir);
	destination = destination.append(name + ".bin");
	String destdir = destination.toString();
	script.printProperty(tab, "destdir", destdir);
	script.printDeleteTask(tab, destdir, null, null);
	script.printMkdirTask(tab, destdir);
	script.printComment(tab, "compiles the source code");
	JavacTask javac = new JavacTask();
	javac.setClasspath(getClasspath(model, jar));
	javac.setDestdir(destdir);
	javac.setFailOnError("no");
	javac.setSrcdir(jar.getSource());
	script.print(tab, javac);

//    <!-- copies the necessary resources -->
//    <copy todir="${destdir}">
//      <fileset dir="${srcdir}" includes="" excludes="**/*.java"/>
//    </copy>

//    <copy todir="${basedir}">
//      <fileset dir="." includes="*.log" />
//    </copy>

	script.printJarTask(tab, name, basedir);
	script.printMkdirTask(tab, destdir);
	script.printEndTag(--tab, "target");
}

protected String getClasspath(PluginModel model, JAR jar) throws CoreException {
	Set classpath = new HashSet(20);
	// always add boot and runtime
	addLibraries(getRegistry().getPlugin(PI_BOOT), classpath);
	addFragmentsLibraries(getRegistry().getPlugin(PI_BOOT), classpath);
	addLibraries(getRegistry().getPlugin(PI_RUNTIME), classpath);
	addFragmentsLibraries(getRegistry().getPlugin(PI_RUNTIME), classpath);
	// add libraries from pre-requisite plug-ins
	PluginPrerequisiteModel[] requires = model.getRequires();
	if (requires != null) {
		for (int i = 0; i < requires.length; i++) {
			PluginModel prerequisite = getRegistry().getPlugin(requires[i].getPlugin(), requires[i].getVersion());
			addLibraries(prerequisite, classpath);
			addFragmentsLibraries(prerequisite, classpath);
		}
	}
	// add libraries from this plug-in
	String jarOrder = (String) getBuildProperties(model).get(PROPERTY_JAR_ORDER);
	String root = getLocation(model);
	if (jarOrder == null) {
		// if no jar order was specified in build.properties, we add all the libraries but the current one
		JAR[] jars = extractJars(getBuildProperties(model));
		for (int i = 0; i < jars.length; i++) {
			if (jar.getName().equals(jars[i].getName()))
				continue;
			File library = new File(root, jars[i].getName());
			classpath.add(library.getAbsolutePath());
		}
	} else {
		// otherwise we add all the predecessor jars
		String[] order = Utils.getArrayFromString(jarOrder);
		for (int i = 0; i < order.length; i++) {
			if (order[i].equals(jar.getName()))
				break;
			File library = new File(root, order[i]);
			classpath.add(library.getAbsolutePath());
		}
	}
	return Utils.getStringFromCollection(classpath, ";");
}

protected void addLibraries(PluginModel model, Set classpath) throws CoreException {
	LibraryModel[] libraries = model.getRuntime();
	String root = getLocation(model);
	for (int i = 0; i < libraries.length; i++) {
		File library = new File(root, libraries[i].getName());
		classpath.add(library.getAbsolutePath());
	}
}

protected void addFragmentsLibraries(PluginModel model, Set classpath) throws CoreException {
	PluginFragmentModel[] fragments = getRegistry().getFragments();
	for (int i = 0; i < fragments.length; i++) {
		if (fragments[i].getPlugin().equals(model.getId()))
			addLibraries(fragments[i], classpath);
	}
}




protected Properties getBuildProperties(PluginModel model) throws CoreException {
	VersionedIdentifier identifier = new VersionedIdentifier(model.getId(), model.getVersion());
	Properties result = (Properties) buildProperties.get(identifier);
	if (result == null) {
		result = readBuildProperties(model);
		buildProperties.put(identifier, result);
	}
	return result;
}

protected Properties readBuildProperties(PluginModel model) throws CoreException {
	Properties result = new Properties();
	File file = new File(getLocation(model), PROPERTIES_FILE);
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

protected String getLocation(PluginModel model) throws CoreException {
	try {
		return new URL(model.getLocation()).getFile();
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url"), e));
	}
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
	File file = new File(getLocation(model), scriptName);
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

protected String getLibraryLocation() throws CoreException {
	if (sourceLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_SOURCE_LOCATION_MISSING, Policy.bind("error.missingSourceLocation"), null));
	File file = new File(sourceLocation, LIBRARY_FILE);
	return file.getAbsolutePath();
}

protected String getMainScriptLocation() throws CoreException {
	if (sourceLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_SOURCE_LOCATION_MISSING, Policy.bind("error.missingSourceLocation"), null));
	File file = new File(sourceLocation, scriptName);
	return file.getAbsolutePath();
}
	
public void setSourceLocation(String location) {
	this.sourceLocation = location;
}

}