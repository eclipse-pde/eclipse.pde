/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
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
	
	/** constants */
	protected static final String BASEDIR = getPropertyFormat(PROPERTY_BASEDIR);
	protected static final String BUILD_RESULT_FOLDER = getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER);
	protected static final String TEMP_FOLDER = getPropertyFormat(PROPERTY_TEMP_FOLDER);

/**
 * Default constructor for the class. */
public AbstractBuildScriptGenerator() {
	buildProperties = new HashMap();
}

/**
 *  * @param model * @param jar * @return String * @throws CoreException */
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
	List pluginChain = new ArrayList(10);
	pluginChain.add(model);
	if (requires != null) {
		for (int i = 0; i < requires.length; i++) {
			PluginModel prerequisite = getPlugin(requires[i].getPlugin(), requires[i].getVersion());
			addPrerequisiteLibraries(prerequisite, classpath, location, pluginChain, true);
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
		if (libraries != null) {
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
	// if it is a fragment, add the plugin as prerequisite
	if (model instanceof PluginFragmentModel) {
		PluginModel plugin = getRegistry().getPlugin(((PluginFragmentModel) model).getPlugin());
		addPrerequisiteLibraries(plugin, classpath, location, pluginChain, false);
	}
	// add extra classpath if it exists
	String extraClasspath = (String) getBuildProperties(model).get(PROPERTY_JAR_EXTRA_CLASSPATH);
	if (extraClasspath != null) {
		String[] extra = Utils.getArrayFromString(extraClasspath, ";,"); //$NON-NLS-1$
		for (int i = 0; i < extra.length; i++)			
			classpath.add(extra[i]);
	}
	return replaceVariables(Utils.getStringFromCollection(classpath, ";")); //$NON-NLS-1$
}

/**
 * Return the plug-in model object from the plug-in registry for the given
 * plug-in identifier and version. If the plug-in is not in the registry then
 * throw an exception.
 *  * @param id the plug-in identifier * @param version the plug-in version * @return PluginModel * @throws CoreException if the specified plug-in version does not exist in the registry */
protected PluginModel getPlugin(String id, String version) throws CoreException {
	PluginModel plugin = getRegistry().getPlugin(id, version);
	if (plugin == null) {
		String pluginName = (version == null) ? id : id + "_" + version; //$NON-NLS-1$
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", pluginName), null)); //$NON-NLS-1$
	}
	return plugin;
}

/**
 *  * @param model * @param baseLocation * @param classpath * @throws CoreException */
protected void addDevEntries(PluginModel model, String baseLocation, Set classpath) throws CoreException {
	if (devEntries == null)
		return;
	IPath root = Utils.makeRelative(new Path(getLocation(model)), new Path(baseLocation));
	for (int i = 0; i < devEntries.length; i++)
		classpath.add(root.append(devEntries[i]));
}

/**
 * Return the file system location for the given plug-in model object.
 *  * @param model the plug-in * @return String * @throws CoreException if a valid file-system location could not be constructed */
protected String getLocation(PluginModel model) throws CoreException {
	try {
		return new URL(model.getLocation()).getFile();
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url"), e)); //$NON-NLS-1$
	}
}

/**
 *  * @param model * @param classpath * @param baseLocation * @throws CoreException */
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

/**
 *  * @param plugin * @param classpath * @param baseLocation * @throws CoreException */
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
 *  * @param plugin * @param fragment * @param classpath * @param baseLocation * @throws CoreException */
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

/**
 * The pluginChain parameter is used to keep track of possible cycles. If prerequisite is already
 * present in the chain it is not included in the classpath.
 *  * @param prerequisite * @param classpath * @param baseLocation * @param pluginChain * @param considerExport * @throws CoreException */
protected void addPrerequisiteLibraries(PluginModel prerequisite, Set classpath, String baseLocation, List pluginChain, boolean considerExport) throws CoreException {
	if (pluginChain.contains(prerequisite))
		throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_CLASSPATH_CYCLE, Policy.bind("error.pluginCycle"), null)); //$NON-NLS-1$
	addLibraries(prerequisite, classpath, baseLocation);
	// add libraries (if exported) from pre-requisite plug-ins
	PluginPrerequisiteModel[] requires = prerequisite.getRequires();
	if (requires == null)
		return;
	pluginChain.add(prerequisite);
	for (int i = 0; i < requires.length; i++) {
		if (considerExport && !requires[i].getExport())
			continue;
		PluginModel plugin = getPlugin(requires[i].getPlugin(), requires[i].getVersion());
		addPrerequisiteLibraries(plugin, classpath, baseLocation, pluginChain, considerExport);
		addFragmentsLibraries(plugin, classpath, baseLocation);
		addDevEntries(plugin, baseLocation, classpath);
	}
	pluginChain.remove(prerequisite);
}

/**
 *  * @param model * @return Properties * @throws CoreException */
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
 *  * @param rootLocation the parent directory of the build.properties file * @return Properties * @throws CoreException if there was a problem reading the file */
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
 *  * @param properties * @return JAR[] */
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

/**
 * Add the "build.jars" target to the given Ant script using the specified plug-in model.
 *  * @param script the script to add the target to * @param model the plug-in model to reference * @throws CoreException */
protected void generateBuildJarsTarget(AntScript script, PluginModel model) throws CoreException {
	Properties properties = getBuildProperties(model);
	JAR[] availableJars = extractJars(properties);
	List jarNames = new ArrayList(availableJars.length);
	Map jars = new HashMap(availableJars.length);
	for (int i = 0; i < availableJars.length; i++)
		jars.put(availableJars[i].getName(), availableJars[i]);
	// try to put the jars in a correct compile order
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
			jars.remove(order[i]);
		}
	}
	for (Iterator iterator = jars.values().iterator(); iterator.hasNext();) {
		JAR jar = (JAR) iterator.next();
		String name = jar.getName();
		jarNames.add(name);
		generateJARTarget(script, getClasspath(model, jar), jar);
		generateSRCTarget(script, jar);
	}
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_JARS, TARGET_INIT, null, null, null);
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
		script.printAvailableTask(tab, srcName, getSRCLocation(jarName));
		script.printAntCallTask(tab, srcName, null, null);
	}
	script.printTargetEnd(--tab);
}

/**
 * Add the "jar" target to the given Ant script using the given classpath and
 * jar as parameters.
 *  * @param script the script to add the target to * @param classpath the classpath for the jar command * @param jar * @throws CoreException */
protected void generateJARTarget(AntScript script, String classpath, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName();
	script.printTargetDeclaration(tab++, name, TARGET_INIT, null, name, null);
	String destdir = getTempJARFolderLocation(name);
	script.printProperty(tab, "destdir", destdir); //$NON-NLS-1$
	script.printDeleteTask(tab, destdir, null, null);
	script.printMkdirTask(tab, destdir);
	script.printComment(tab, "compile the source code"); //$NON-NLS-1$
	JavacTask javac = new JavacTask();
	javac.setClasspath(classpath);
	javac.setBootClasspath(getPropertyFormat(PROPERTY_BOOTCLASSPATH));
	javac.setDestdir(destdir);
	javac.setFailOnError("false"); //$NON-NLS-1$
	javac.setDebug("on"); //$NON-NLS-1$
	javac.setVerbose("true"); //$NON-NLS-1$
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
	String jarLocation = getJARLocation(name);
	script.printMkdirTask(tab, new Path(jarLocation).removeLastSegments(1).toString());
	script.printJarTask(tab, jarLocation, destdir);
	script.printDeleteTask(tab, destdir, null, null);
	script.printTargetEnd(--tab);
}

/**
 * Add the "src" target to the given Ant script.
 *  * @param script the script to add the target to * @param jar * @throws CoreException */
protected void generateSRCTarget(AntScript script, JAR jar) throws CoreException {
	int tab = 1;
	script.println();
	String name = jar.getName();
	String srcName = getSRCName(name);
	script.printTargetDeclaration(tab++, srcName, TARGET_INIT, null, srcName, null);
	String[] sources = jar.getSource();
	FileSet[] fileSets = new FileSet[sources.length];
	for (int i = 0; i < sources.length; i++) {
		fileSets[i] = new FileSet(sources[i], null, "**/*.java", null, null, null, null); //$NON-NLS-1$
	}
	String srcLocation = getSRCLocation(name);
	script.printMkdirTask(tab, new Path(srcLocation).removeLastSegments(1).toString());
	script.printZipTask(tab, srcLocation, null, false, fileSets);
	script.printTargetEnd(--tab);
}

/**
 * Return the name of the zip file for the source from the given jar name.
 *  * @param jarName the name of the jar file * @return String */
protected String getSRCName(String jarName) {
	return jarName.substring(0, jarName.length() - 4) + "src.zip"; //$NON-NLS-1$
}

/**
 * Return the full location of the jar file.
 *  * @param jarName the name of the jar file * @return String */
protected String getJARLocation(String jarName) {
	IPath destination = new Path(BUILD_RESULT_FOLDER);
	destination = destination.append(jarName);
	return destination.toString();
}

/**
 * Return the name of the zip file for the source for the jar with
 * the given name.
 *  * @param jarName the name of the jar file * @return String */
protected String getSRCLocation(String jarName) {
	return getSRCName(getJARLocation(jarName));
}

/**
 * Return the location for a temporary file for the jar file with
 * the given name.
 *  * @param jarName the name of the jar file * @return String */
protected String getTempJARFolderLocation(String jarName) {
	IPath destination = new Path(TEMP_FOLDER);
	destination = destination.append(jarName + ".bin"); //$NON-NLS-1$
	return destination.toString();
}

/**
 * Substitute the value of an element description variable (variables that
 * are found in files like plugin.xml, e.g. $ws$) by an Ant property.
 *  * @param sourceString * @return String */
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
 *  * @param entries */
public void setDevEntries(String[] entries) {
	this.devEntries = entries;
}

/**
 * Sets the buildScriptName.
 *  * @param buildScriptName */
public void setBuildScriptName(String buildScriptName) {
	if (buildScriptName == null)
		this.buildScriptName = DEFAULT_BUILD_SCRIPT_FILENAME;
	else
		this.buildScriptName = buildScriptName;
}

/**
 * Set this object's install location variable.
 *  * @param location the install location */
public void setInstallLocation(String location) {
	this.installLocation = location;
}

/**
 * Return the plug-in registry. If this value isn't cached, then read
 * it from disk.
 *  * @return PluginRegistryModel * @throws CoreException */
protected PluginRegistryModel getRegistry() throws CoreException {
	if (registry == null) {
		URL[] pluginPath = getPluginPath();
		MultiStatus problems = new MultiStatus(PI_PDEBUILD, EXCEPTION_MODEL_PARSE, Policy.bind("exception.pluginParse"), null); //$NON-NLS-1$
		Factory factory = new Factory(problems);
		registry = Platform.parsePlugins(pluginPath, factory);
		IStatus status = factory.getStatus();
		if (Utils.contains(status, IStatus.ERROR))
			throw new CoreException(status);
	}
	return registry;
}

/**
 *  * @return URL[] */
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
 *  * @param pluginPath */
public void setPluginPath(URL[] pluginPath) {
	this.pluginPath = pluginPath;
}

/**
 *  * @param buf * @param start * @param target * @return int */
protected int scan(StringBuffer buf, int start, String target) {
	return scan(buf, start, new String[] {target});
}

/**
 *  * @param buf * @param start * @param targets * @return int */
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
 *  * @param target the file * @return StringBuffer * @throws IOException */
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
 *  * @param buildFile * @param propertyName * @param version * @throws CoreException * @throws IOException */
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