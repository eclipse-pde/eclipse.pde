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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.internal.runtime.PlatformURLFragmentConnection;
import org.eclipse.core.internal.runtime.PlatformURLPluginConnection;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.update.core.VersionedIdentifier;

/**
 *
 */
public abstract class AbstractBuildScriptGenerator extends AbstractScriptGenerator implements IPDEBuildConstants {

	/**
	 * Map of build.properties from the existing plugin models or features.
	 */
	protected Map buildProperties;

	/** build script name */
	protected String buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;
	
	/**
	 * Where to put the generated script.
	 */
	protected String scriptTargetLocation;

	/**
	 * Location of the plug-ins and fragments.
	 */
	private URL[] pluginPath;

	/**
	 * Plug-in registry for the elements. Should only be accessed by getRegistry().
	 */
	private PluginRegistryModel registry;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected String[] devEntries;

	protected class JAR {
		private String name;
		private String[] source;
		private String[] output;
		private String[] extraClasspath;
		private String resolvedName;
		
		protected JAR(String name, String[] source, String[] output, String[] extraClasspath) {
			this.name = name;
			this.source = source;
			this.output = output;
			this.extraClasspath = extraClasspath;
		}
		protected String getName(boolean resolved) {
			if (! resolved) 
				return name;
			
			if (resolvedName==null)
				resolvedName = replaceVariables(name);
				
			return resolvedName;
		}
		protected String[] getSource() {
			return source;
		}
		public String[] getOutput() {
			return output;
		}
		public String[] getExtraClasspath() {
			return extraClasspath;
		}
	}
	

	/** The plugin for which the classpath is being computed */ 
	private PluginModel currentModel;
	
	/** constants */
	protected static final String BASEDIR = getPropertyFormat(PROPERTY_BASEDIR);
	protected static final String BUILD_RESULT_FOLDER = getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER);
	protected static final String TEMP_FOLDER = getPropertyFormat(PROPERTY_TEMP_FOLDER);

/**
 * Default constructor for the class.
 */
public AbstractBuildScriptGenerator() {
	buildProperties = new HashMap();
}

/**
 * Compute the classpath for the given jar.
 * The path returned conforms to Parent / Self / Prerequisite
 * @param model : the plugin containing the jar compiled
 * @param jar : the jar for which the classpath is being compiled
 * @return String : the classpath
 * @throws CoreException
 */
protected String getClasspath(PluginModel model, JAR jar) throws CoreException {
	currentModel = model;
	List classpath = new ArrayList(20);
	List pluginChain = new ArrayList(10);
	String location = getLocation(model);

	//PARENT  
	addPlugin(getPlugin(PI_BOOT, null), classpath, location);

	//SELF
	addSelf(model, jar, classpath, location, pluginChain);
	
	//PREREQUISITE
	addPrerequisites(model, classpath, location, pluginChain);
		
	return Utils.getStringFromCollection(classpath, ";"); //$NON-NLS-1$

}


protected void addSelf(PluginModel model, JAR jar, List classpath, String location, List pluginChain) throws CoreException {
	// If model is a fragment, we need to add in the classpath the plugin to which it is related
	if (model instanceof PluginFragmentModel) {
		PluginModel plugin = getRegistry().getPlugin(((PluginFragmentModel) model).getPlugin());
		addPluginAndPrerequisites(plugin, classpath, location, pluginChain);
	}	
	
	// Add the libraries
	Properties modelProperties = getBuildProperties(model);
	String jarOrder = (String) modelProperties.get(PROPERTY_JAR_ORDER);
	if (jarOrder == null) {
		// if no jar order was specified in build.properties, we add all the libraries but the current one
		// based on the order specified by the plugin.xml. Both library that we compile and .jar provided are processed
		LibraryModel[] libraries = model.getRuntime();
		if (libraries != null) {
			for (int i = 0; i < libraries.length; i++) {
				String libraryName = libraries[i].getName();
				if (jar.getName(false).equals(libraryName))
					continue;
	
				boolean isSource = (modelProperties.getProperty(PROPERTY_SOURCE_PREFIX + libraryName) != null);
				if (isSource) {
					addDevEntries(model, location, classpath, (String[]) Utils.getArrayFromString(modelProperties.getProperty(PROPERTY_OUTPUT_PREFIX + libraryName)));
				}
				//Potential pb: here there maybe a nasty case where the libraries variable may refer to something which is part of the base
				//but $xx$ will replace it by the $xx instead of $basexx. The solution is for the user to use the explicitly set the content
				// of its build.property file
				addPathAndCheck(libraryName, classpath);
			}
		}
	} else {
		// otherwise we add all the predecessor jars
		String[] order = Utils.getArrayFromString(jarOrder);
		for (int i = 0; i < order.length; i++) {
			if (order[i].equals(jar.getName(false)))
				break;
			addDevEntries(model, location, classpath, (String[]) Utils.getArrayFromString((String) modelProperties.get(PROPERTY_OUTPUT_PREFIX + order[i])));
			addPathAndCheck(order[i], classpath);
		}
		// Then we add all the "pure libraries" (the one that does not contain source)
		LibraryModel[] libraries = model.getRuntime();
		for (int i = 0; i < libraries.length; i++) {
			String libraryName = libraries[i].getName();
			if (modelProperties.get(PROPERTY_SOURCE_PREFIX + libraryName) == null) {
				//Potential pb: if the pure library is something that is being compiled (which is supposetly not the case, but who knows...)
				//the user will get $basexx instead of $ws 
				addPathAndCheck(libraryName, classpath);
			}
		}
	}

	// add extra classpath if it exists. this code is kept for backward compatibility
	String extraClasspath = (String) modelProperties.get(PROPERTY_JAR_EXTRA_CLASSPATH);
	if (extraClasspath != null) {
		String[] extra = Utils.getArrayFromString(extraClasspath, ";,"); //$NON-NLS-1$
		
		for (int i = 0; i < extra.length; i++) {
			//Potential pb: if the path refers to something that is being compiled (which is supposetly not the case, but who knows...)
			//the user will get $basexx instead of $ws 
			addPathAndCheck(computeExtraPath(extra[i], location), classpath);
		}	 
	}

	//	add extra classpath if it is specified for the given jar
	String[] jarSpecificExtraClasspath = (String[]) jar.getExtraClasspath();
	for (int i = 0; i < jarSpecificExtraClasspath.length; i++) {
		//Potential pb: if the path refers to something that is being compiled (which is supposetly not the case, but who knows...)
		//the user will get $basexx instead of $ws 
		addPathAndCheck(computeExtraPath(jarSpecificExtraClasspath[i], location), classpath); 
	}
}

/** 
 * Convenience method that compute the relative classpath of extra.classpath entries  
 * @param url : a url
 * @param location : location used as a base location to compute the relative path 
 * @return String : the relative path 
 * @throws CoreException
 */
private String computeExtraPath(String url, String location) throws CoreException {
	String relativePath = null;
	
	String[] urlfragments = Utils.getArrayFromString(url, "/");	 //$NON-NLS-1$
	
	// A valid platform url for a plugin has a leat 3 segments.
	if (urlfragments.length>2 && urlfragments[0].equals(PlatformURLHandler.PROTOCOL+PlatformURLHandler.PROTOCOL_SEPARATOR)){
		String modelLocation = null;
		if (urlfragments[1].equalsIgnoreCase(PlatformURLPluginConnection.PLUGIN))
			modelLocation = getLocation(getRegistry().getPlugin(urlfragments[2]));
		
		if (urlfragments[1].equalsIgnoreCase(PlatformURLFragmentConnection.FRAGMENT))
			modelLocation = getLocation(getRegistry().getFragment(urlfragments[2]));
		
		if (urlfragments[1].equalsIgnoreCase("resource")) { 	//TODO I did not find the declaration of the constant 
			String message = Policy.bind("exception.url", PROPERTIES_FILE + "::"+url);  //$NON-NLS-1$  //$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR,PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_MALFORMED_URL, message,null));
		}		
		if (modelLocation != null) {
			for (int i = 3; i < urlfragments.length; i++) {
				if (i==3)
					modelLocation += urlfragments[i];
				else	
					modelLocation += "/"+urlfragments[i];	//$NON-NLS-1$
			}			
			return relativePath = Utils.makeRelative(new Path(modelLocation), new Path(location)).toOSString();
		}
	}
	
	// Then it's just a regular URL, or just something that will be added at the end of the classpath for backward compatibility.......
	try {
		URL extraURL = new URL(url);
		try {
			relativePath = Utils.makeRelative(new Path(Platform.resolve(extraURL).getFile()), new Path(location)).toOSString();
		} catch (IOException e) {
			String message = Policy.bind("exception.url", PROPERTIES_FILE + "::"+url);  //$NON-NLS-1$  //$NON-NLS-2$
			throw new CoreException(new Status(IStatus.ERROR,PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_MALFORMED_URL, message,e));
		}
	} catch (MalformedURLException e) {
		relativePath = url;
		//TODO remove this backward compatibility support for as soon as we go to 2.2 and put back the exception
		//		String message = Policy.bind("exception.url", PROPERTIES_FILE + "::"+url); //$NON-NLS-1$  //$NON-NLS-2$
		//		throw new CoreException(new Status(IStatus.ERROR,PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_MALFORMED_URL, message,e));
	}		 
	return relativePath;
}

/**
 * Return the plug-in model object from the plug-in registry for the given
 * plug-in identifier and version. If the plug-in is not in the registry then
 * throw an exception.
 * 
 * @param id the plug-in identifier
 * @param version the plug-in version
 * @return PluginModel
 * @throws CoreException if the specified plug-in version does not exist in the registry
 */
protected PluginModel getPlugin(String id, String version) throws CoreException {
	PluginModel plugin = getRegistry().getPlugin(id, version);
	if (plugin == null) {
		String pluginName = (version == null) ? id : id + "_" + version; //$NON-NLS-1$
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", pluginName), null)); //$NON-NLS-1$
	}
	return plugin;
}

/**
 * 
 * @param model
 * @param baseLocation
 * @param classpath
 * @throws CoreException
 */
protected void addDevEntries(PluginModel model, String baseLocation, List classpath, String[] jarSpecificEntries) throws CoreException {
	// first we verify is the addition of dev entries is required
	if (devEntries != null && devEntries.length == 0)
		return;

	if (devEntries == null && jarSpecificEntries == null)
		return;

	String[] entries;
	// if jarSpecificEntries is given, then it overrides devEntries 
	if (jarSpecificEntries != null && jarSpecificEntries.length > 0)
		entries = jarSpecificEntries;
	else
		entries = devEntries;

	IPath root = Utils.makeRelative(new Path(getLocation(model)), new Path(baseLocation));
	String path;
	for (int i = 0; i < entries.length; i++) {
		path = root.append(entries[i]).toString();
		addPathAndCheck(path, classpath);
	}
}

// Add a path into the classpath for a given model
// path : The path to add
// classpath : The classpath in which we want to add this path 
private void addPathAndCheck(String path, List classpath) {
	path = replaceVariables(path);
	if (!classpath.contains(path))
		classpath.add(path);
}

/**
 * Return the file system location for the given plug-in model object.
 * 
 * @param model the plug-in
 * @return String
 * @throws CoreException if a valid file-system location could not be constructed
 */
protected String getLocation(PluginModel model) throws CoreException {
	try {
		return new URL(model.getLocation()).getFile();
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url"), e)); //$NON-NLS-1$
	}
}

public String getScriptTargetLocation() {
	return scriptTargetLocation;
}

/**
 * Add the runtime libraries for the specified plugin. 
 * @param model
 * @param classpath
 * @param baseLocation
 * @throws CoreException
 */
protected void addRuntimeLibraries(PluginModel model, List classpath, String baseLocation) throws CoreException {
	LibraryModel[] libraries = model.getRuntime();
	if (libraries == null)
		return;
	String root = getLocation(model);
	IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
	for (int i = 0; i < libraries.length; i++) {
		addDevEntries(model, baseLocation, classpath, Utils.getArrayFromString(getBuildProperties(model).getProperty(PROPERTY_OUTPUT_PREFIX + libraries[i].getName())));
		String library = base.append(libraries[i].getName()).toString();
		addPathAndCheck(library, classpath);
	}
}

/**
 * Add the specified plugin (including its jars) and its fragments 
 * @param model
 * @param classpath
 * @param location
 * @throws CoreException
 */
private void addPlugin(PluginModel plugin, List classpath, String location) throws CoreException {
	addRuntimeLibraries(plugin, classpath, location);
	addFragmentsLibraries(plugin, classpath, location);
}


/**
 * Add all fragments of the given plugin
 * @param plugin
 * @param classpath
 * @param baseLocation
 * @throws CoreException
 */
protected void addFragmentsLibraries(PluginModel plugin, List classpath, String baseLocation) throws CoreException {
	// if plugin is not a plugin, it's a fragment and there is no fragment for a fragment. So we return.
	if (!(plugin instanceof PluginDescriptorModel))
		return;

	PluginDescriptorModel pluginModel = (PluginDescriptorModel) plugin;
	PluginFragmentModel[] fragments = pluginModel.getFragments();
	if (fragments == null)
		return;

	for (int i = 0; i < fragments.length; i++) {
		if (fragments[i]==currentModel)
			continue;
		addPluginLibrariesToFragmentLocations(plugin, fragments[i], classpath, baseLocation);
		addRuntimeLibraries(fragments[i], classpath, baseLocation);
	}
}

/**
 * There are cases where the plug-in only declares a library but the real JAR is under
 * a fragment location. This method gets all the plugin libraries and place them in the
 * possible fragment location.
 * 
 * @param plugin
 * @param fragment
 * @param classpath
 * @param baseLocation
 * @throws CoreException
 */
protected void addPluginLibrariesToFragmentLocations(PluginModel plugin, PluginFragmentModel fragment, List classpath, String baseLocation) throws CoreException {
	LibraryModel[] libraries = plugin.getRuntime();
	if (libraries == null)
		return;
	String root = getLocation(fragment);
	IPath base = Utils.makeRelative(new Path(root), new Path(baseLocation));
	for (int i = 0; i < libraries.length; i++) {
		String libraryName = base.append(libraries[i].getName()).toString();
		addPathAndCheck(libraryName, classpath);
	}
}



/**
 * The pluginChain parameter is used to keep track of possible cycles. If prerequisite is already
 * present in the chain it is not included in the classpath.
 * 
 * @param target : the plugin for which we are going to introduce
 * @param classpath 
 * @param baseLocation
 * @param pluginChain
 * @param considerExport
 * @throws CoreException
 */
protected void addPluginAndPrerequisites(PluginModel target, List classpath, String baseLocation, List pluginChain) throws CoreException {
	addPlugin(target, classpath, baseLocation);
	addPrerequisites(target, classpath, baseLocation, pluginChain);
}

//Add the prerequisite of a given plugin (target)
protected void addPrerequisites(PluginModel target, List classpath, String baseLocation, List pluginChain) throws CoreException {

	if (pluginChain.contains(target)) {
		if (target==getPlugin(PI_RUNTIME,null))
			return;
		String message = Policy.bind("error.pluginCycle"); //$NON-NLS-1$
		throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_CLASSPATH_CYCLE, message, null));
	}
	
	//	The first prerequisite is ALWAYS runtime
	 if (target != getPlugin(PI_RUNTIME, null))
		 addPluginAndPrerequisites(getPlugin(PI_RUNTIME, null), classpath, baseLocation, pluginChain);
	
	 // add libraries from pre-requisite plug-ins.  Don't worry about the export flag
	 // as all required plugins may be required for compilation.
	PluginPrerequisiteModel[] requires = target.getRequires();
	if (requires != null) {
		pluginChain.add(target);
	 	for (int i = 0; i < requires.length; i++) {
			PluginModel plugin = getPlugin(requires[i].getPlugin(), requires[i].getVersion());
			if (plugin != null)
				addPluginAndPrerequisites(plugin, classpath, baseLocation, pluginChain);
	 	}
	 	pluginChain.remove(target);
	}
}

/**
 * 
 * @param model
 * @return Properties
 * @throws CoreException
 */
protected Properties getBuildProperties(PluginModel model) throws CoreException {
	VersionedIdentifier identifier = new VersionedIdentifier(model.getId(), model.getVersion());
	Properties result = (Properties) buildProperties.get(identifier);
	if (result == null) {
		result = readBuildProperties(getLocation(model));
		buildProperties.put(identifier, result);
	}
	return result;
}

/**
 * Read the "build.properties" file from the given location and return the associated
 * <code>java.io.Properties</code> object. Throw a <code>CoreException</code>
 * if there is a problem reading the file.
 * 
 * @param rootLocation the parent directory of the build.properties file
 * @return Properties
 * @throws CoreException if there was a problem reading the file
 */
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
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, Policy.bind("exception.readingFile"), e)); //$NON-NLS-1$
	}
	return result;
}

/**
 * 
 * @param properties
 * @return JAR[]
 */
protected JAR[] extractJars(Properties properties) {
	List result = new ArrayList(5);
	int prefixLength = PROPERTY_SOURCE_PREFIX.length();
	for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (!(key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_JAR_SUFFIX)))
			continue;
		key = key.substring(prefixLength);
		String[] source = Utils.getArrayFromString((String) entry.getValue());
		String[] output = Utils.getArrayFromString((String) properties.getProperty(PROPERTY_OUTPUT_PREFIX + key));
		String[] extraClasspath = Utils.getArrayFromString((String) properties.getProperty(PROPERTY_EXTRAPATH_PREFIX + key));
		JAR jar = new JAR(key, source, output, extraClasspath);
		result.add(jar);
	}
	return (JAR[]) result.toArray(new JAR[result.size()]);
}

/**
 * Add the "build.jars" target to the given Ant script using the specified plug-in model.
 * 
 * @param script the script to add the target to
 * @param model the plug-in model to reference
 * @throws CoreException
 */
protected void generateBuildJarsTarget(AntScript script, PluginModel model) throws CoreException {
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	List jarNames = new ArrayList(availableJars.length);
	Map jars = new HashMap(availableJars.length);
	for (int i = 0; i < availableJars.length; i++)
		jars.put(availableJars[i].getName(false), availableJars[i]);
	// try to put the jars in a correct compile order
	String jarOrder = (String) getBuildProperties(model).get(PROPERTY_JAR_ORDER);
	if (jarOrder != null) {
		String[] order = Utils.getArrayFromString(jarOrder);
		for (int i = 0; i < order.length; i++) {
			JAR jar = (JAR) jars.get(order[i]);
			if (jar == null)
				continue;
			String name = jar.getName(false);
			jarNames.add(name);
			generateJARTarget(script, getClasspath(model, jar), jar);
			generateSRCTarget(script, jar);
			jars.remove(order[i]);
		}
	}
	for (Iterator iterator = jars.values().iterator(); iterator.hasNext();) {
		JAR jar = (JAR) iterator.next();
		String name = jar.getName(false);
		jarNames.add(name);
		generateJARTarget(script, getClasspath(model, jar), jar);
		generateSRCTarget(script, jar);
	}
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_JARS, TARGET_INIT, null, null, Policy.bind("build.plugin.buildJars", model.getId())); //$NON-NLS-1$ 
	for (Iterator iter = jarNames.iterator(); iter.hasNext();) {
		String name = (String) iter.next();
		script.printAvailableTask(tab, name, getJARLocation(name));
		script.printAntCallTask(tab, name, null, null);
	}
	script.printTargetEnd(--tab);
	
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
	for (Iterator iter = jarNames.iterator(); iter.hasNext();) {
		String jarName = (String) iter.next();
		String srcName = getSRCName(jarName);
		script.printAvailableTask(tab, srcName, replaceVariables(getSRCLocation(jarName)));
		script.printAntCallTask(tab, srcName, null, null);
	}
	script.printTargetEnd(--tab);
}

/**
 * Add the "jar" target to the given Ant script using the given classpath and
 * jar as parameters.
 * 
 * @param script the script to add the target to
 * @param classpath the classpath for the jar command
 * @param jar
 * @throws CoreException
 */
protected void generateJARTarget(AntScript script, String classpath, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName(false);
	script.printTargetDeclaration(tab++, name, TARGET_INIT, null, jar.getName(true), Policy.bind("build.plugin.jar", name));  //$NON-NLS-1$
	String destdir = getTempJARFolderLocation(jar.getName(true));
	script.printProperty(tab, "destdir", destdir); //$NON-NLS-1$
	script.printDeleteTask(tab, destdir, null, null);
	script.printMkdirTask(tab, destdir);
	script.printComment(tab, "compile the source code"); //$NON-NLS-1$
	JavacTask javac = new JavacTask();
	javac.setClasspath(classpath);
	javac.setBootClasspath(getPropertyFormat(PROPERTY_BOOTCLASSPATH));
	javac.setDestdir(destdir);
	javac.setFailOnError(getPropertyFormat(PROPERTY_JAVAC_FAIL_ON_ERROR));
	javac.setDebug(getPropertyFormat(PROPERTY_JAVAC_DEBUG_INFO));
	javac.setVerbose(getPropertyFormat(PROPERTY_JAVAC_VERBOSE));
	javac.setIncludeAntRuntime("no"); //$NON-NLS-1$
	String[] sources = jar.getSource();
	javac.setSrcdir(sources);
	script.print(tab, javac);
	script.printComment(tab, "copy necessary resources"); //$NON-NLS-1$
	FileSet[] fileSets = new FileSet[sources.length];
	for (int i = 0; i < sources.length; i++) {
		fileSets[i] = new FileSet(sources[i], null, null, null, "**/*.java", null, null); //$NON-NLS-1$
	}
	script.printCopyTask(tab, null, destdir, fileSets);
	String jarLocation = getJARLocation(jar.getName(true));
	script.printMkdirTask(tab, new Path(jarLocation).removeLastSegments(1).toString());
	script.printJarTask(tab, jarLocation, destdir);
	script.printDeleteTask(tab, destdir, null, null);
	script.printTargetEnd(--tab);
}

/**
 * Add the "src" target to the given Ant script.
 * 
 * @param script the script to add the target to
 * @param jar
 * @throws CoreException
 */
protected void generateSRCTarget(AntScript script, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName(false);
	String srcName = getSRCName(name);
	script.printTargetDeclaration(tab++, srcName, TARGET_INIT, null, replaceVariables(name), null);
	String[] sources = jar.getSource();
	FileSet[] fileSets = new FileSet[sources.length];
	for (int i = 0; i < sources.length; i++) {
		fileSets[i] = new FileSet(sources[i], null, "**/*.java", null, null, null, null); //$NON-NLS-1$
	}
	String srcLocation = replaceVariables(getSRCLocation(name));
	script.printMkdirTask(tab, new Path(srcLocation).removeLastSegments(1).toString());
	script.printZipTask(tab, srcLocation, null, false, null, false, fileSets);
	script.printTargetEnd(--tab);
}

/**
 * Return the name of the zip file for the source from the given jar name.
 * 
 * @param jarName the name of the jar file
 * @return String
 */
protected String getSRCName(String jarName) {
	return jarName.substring(0, jarName.length() - 4) + "src.zip"; //$NON-NLS-1$
}

/**
 * Return the full location of the jar file.
 * 
 * @param jarName the name of the jar file
 * @return String
 */
protected String getJARLocation(String jarName) {
	IPath destination = new Path(BUILD_RESULT_FOLDER);
	destination = destination.append(jarName);
	return destination.toString();
}

/**
 * Return the name of the zip file for the source for the jar with
 * the given name.
 * 
 * @param jarName the name of the jar file
 * @return String
 */
protected String getSRCLocation(String jarName) {
	return getSRCName(getJARLocation(jarName));
}

/**
 * Return the location for a temporary file for the jar file with
 * the given name.
 * 
 * @param jarName the name of the jar file
 * @return String
 */
protected String getTempJARFolderLocation(String jarName) {
	IPath destination = new Path(TEMP_FOLDER);
	destination = destination.append(jarName + ".bin"); //$NON-NLS-1$
	return destination.toString();
}

/**
 * Substitute the value of an element description variable (variables that
 * are found in files like plugin.xml, e.g. $ws$) by an Ant property.
 * 
 * @param sourceString
 * @return String
 */
protected String replaceVariables(String sourceString) {
	int i = -1;
	String result = sourceString;
	while ((i = result.indexOf(DESCRIPTION_VARIABLE_WS)) >= 0)
		result = result.substring(0, i) + "ws/" + getPropertyFormat(PROPERTY_WS) + result.substring(i + DESCRIPTION_VARIABLE_WS.length()); //$NON-NLS-1$
	while ((i = result.indexOf(DESCRIPTION_VARIABLE_OS)) >= 0)
		result = result.substring(0, i) + "os/" + getPropertyFormat(PROPERTY_OS) + result.substring(i + DESCRIPTION_VARIABLE_OS.length()); //$NON-NLS-1$
	while ((i = result.indexOf(DESCRIPTION_VARIABLE_NL)) >= 0)
		result = result.substring(0, i) + "nl/" + getPropertyFormat(PROPERTY_NL) + result.substring(i + DESCRIPTION_VARIABLE_NL.length()); //$NON-NLS-1$
	return result;
}

/**
 * 
 * @param entries
 */
public void setDevEntries(String[] entries) {
	this.devEntries = entries;
}

/**
 * Sets the buildScriptName.
 * 
 * @param buildScriptName
 */
public void setBuildScriptName(String buildScriptName) {
	if (buildScriptName == null)
		this.buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;
	else
		this.buildScriptName = buildScriptName;
}

/**
 * Sets the alternative location of the build script.
 */

public void setScriptTargetLocation(String location) {
	this.scriptTargetLocation = location;
}

/**
 * Set this object's install location variable.
 * 
 * @param location the install location
 */
public void setInstallLocation(String location) {
	this.installLocation = location;
}

/**
 * Return the plug-in registry. If this value isn't cached, then read
 * it from disk.
 * 
 * @return PluginRegistryModel
 * @throws CoreException
 */
protected PluginRegistryModel getRegistry() throws CoreException {
	if (registry == null) {
		URL[] pluginPath = getPluginPath();
		MultiStatus problems = new MultiStatus(PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null); //$NON-NLS-1$
		Factory factory = new Factory(problems);
		registry = Platform.parsePlugins(pluginPath, factory);
		setFragments();
		IStatus status = factory.getStatus();
		if (Utils.contains(status, IStatus.ERROR))
			throw new CoreException(status);
	}
	return registry;
}

private void setFragments() {
	PluginFragmentModel[] fragments = registry.getFragments();
	for (int i = 0; i < fragments.length; i++) {
		String pluginId = fragments[i].getPluginId();
		PluginDescriptorModel plugin = registry.getPlugin(pluginId);
		PluginFragmentModel[] existingFragments = plugin.getFragments();
		if (existingFragments == null)
			plugin.setFragments(new PluginFragmentModel[] { fragments[i] });
		else {
			PluginFragmentModel[] newFragments = new PluginFragmentModel[existingFragments.length + 1];
			System.arraycopy(existingFragments, 0, newFragments, 0, existingFragments.length);
			newFragments[newFragments.length - 1] = fragments[i];
			plugin.setFragments(newFragments);
		}
	}
}

/**
 * 
 * @return URL[]
 */
protected URL[] getPluginPath() {
	// Get the plugin path if one was spec'd.
	if (pluginPath != null)
		return pluginPath;
	// Otherwise, if the install location was spec'd, compute the default path.
	if (installLocation != null) {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("file:"); //$NON-NLS-1$
			sb.append(installLocation);
			sb.append("/"); //$NON-NLS-1$
			sb.append(DEFAULT_PLUGIN_LOCATION);
			sb.append("/"); //$NON-NLS-1$
			return new URL[] { new URL(sb.toString()) };
		} catch (MalformedURLException e) {
			// Ignore because should never happen.
		}
	}
	return null;
}

/**
 * Sets the pluginPath.
 * 
 * @param pluginPath
 */
public void setPluginPath(URL[] pluginPath) {
	this.pluginPath = pluginPath;
}

/**
 * 
 * @param buf
 * @param start
 * @param target
 * @return int
 */
protected int scan(StringBuffer buf, int start, String target) {
	return scan(buf, start, new String[] {target});
}

/**
 * 
 * @param buf
 * @param start
 * @param targets
 * @return int
 */
protected int scan(StringBuffer buf, int start, String[] targets) {
	for (int i=start; i<buf.length(); i++) {
		for (int j=0; j<targets.length; j++) {
			if (i<buf.length()-targets[j].length()) {		
				String match = buf.substring(i, i+targets[j].length());
				if (targets[j].equals(match))
					return i;
			}
		}
	}
	return -1;
}

/**
 * Return a buffer containing the contents of the file at the specified location.
 * 
 * @param target the file
 * @return StringBuffer
 * @throws IOException
 */
protected StringBuffer readFile(File target) throws IOException {
	FileInputStream fis = new FileInputStream(target);
	InputStreamReader reader = new InputStreamReader(fis);
	StringBuffer result = new StringBuffer();
	char[] buf = new char[4096];
	int count;
	try {
		count = reader.read(buf, 0, buf.length);
		while (count != -1) {
			result.append(buf, 0, count);
			count = reader.read(buf, 0, buf.length);
		}
	} finally{
		try {
			fis.close();
			reader.close();
		} catch(IOException e) {
			// ignore exceptions here
		}
	}
	return result;
}

/**
 * Custom build scripts should have their version number matching the
 * version number defined by the feature/plugin/fragment descriptor.
 * This is a best effort job so do not worry if the expected tags were
 * not found and just return without modifying the file.
 * 
 * @param buildFile
 * @param propertyName
 * @param version
 * @throws CoreException
 * @throws IOException
 */
protected void updateVersion(File buildFile, String propertyName, String version) throws CoreException, IOException {
	StringBuffer buffer = readFile(buildFile);
	int pos = scan(buffer, 0, propertyName);
	if (pos == -1)
		return;
	pos = scan(buffer, pos, "value"); //$NON-NLS-1$
	if (pos == -1)
		return;
	int begin = scan(buffer, pos, "\""); //$NON-NLS-1$
	if (begin == -1)
		return;
	begin++;
	int end = scan(buffer, begin, "\""); //$NON-NLS-1$
	if (end == -1)
		return;
	String currentVersion = buffer.substring(begin, end);
	String newVersion = "_" + version; //$NON-NLS-1$
	if (currentVersion.equals(newVersion))
		return;
	buffer.replace(begin, end, newVersion);
	Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(buildFile));
}
}