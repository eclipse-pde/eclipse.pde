package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.pde.core.internal.ant.*;
/**
 * Generic class for generating scripts for plug-ins and fragments.
 */
public abstract class ModelBuildScriptGenerator extends AbstractBuildScriptGenerator {

	/**
	 * PluginModel to generate script from.
	 */
	protected PluginModel model;
	
	/**
	 * FIXME: add comment
	 */
	protected Map devJars;

	/**
	 * FIXME: add comment
	 */
	protected Map jarsCache;

	/**
	 * FIXME: add comment
	 */
	protected List requiredJars;

	/**
	 * FIXME: add comment
	 */
	protected Map trimmedDevJars;

	/**
	 * FIXME: add comment
	 */
	protected List jarOrder;


/**
 * @see AbstractScriptGenerator#generate()
 */
public void generate() throws CoreException {
	if (model == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));

	// if the model defines its own custom script, we just skip from generating it
	String custom = getBuildProperty(PROPERTY_CUSTOM);
	if (custom != null && custom.equalsIgnoreCase("true"))
		return;

	try {
		File root = new File(getModelLocation(model));
		File target = new File(root, DEFAULT_BUILD_SCRIPT_FILENAME);
		script = new BuildAntScript(new FileOutputStream(target));
		setUpAntBuildScript();
		try {
			generateBuildScript();
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
}

/**
 * Main call for generating the script.
 */
protected void generateBuildScript() throws CoreException {
	generatePrologue();

	generatePropertiesTarget();
	generateUpdateJarTarget();
	generateGatherBinPartsTarget();
	generateBuildJarsTarget();

	generateGatherSourcesTarget();
	generateBuildSourcesTarget();

	generateGatherLogTarget();

	generateCleanTarget();
	generateEpilogue();
}

protected void generateCleanTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_CLEAN, TARGET_INIT, null, null, null);
	tab++;
	ArrayList jars = new ArrayList(9);
	ArrayList zips = new ArrayList(9);
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		String jar = (String) i.next();
		jars.add(jar);
		zips.add(jar.substring(0, jar.length() - 4) + "src.zip");
	}
	String compiledJars = getStringFromCollection(jars, "", "", ",");
	String sourceZips = getStringFromCollection(zips, "", "", ",");
	String basedir = getRelativeInstallLocation().toString();
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

protected void generateGatherLogTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_LOG, TARGET_INIT, null, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	baseDestination = baseDestination.append(getDirectoryName());
	List destinations = new ArrayList(5);
	IPath baseSource = new Path(getPropertyFormat(PROPERTY_INSTALL));
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		String jar = (String) i.next();
		IPath destination = baseDestination.append(jar).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(destination)) {
			script.printMkdirTask(tab, destination.toString());
			destinations.add(destination);
		}
		script.printCopyTask(tab, baseSource.append(jar + ".bin.log").toString(), destination.toString(), null);
	}
	script.printEndTag(--tab, TARGET_TARGET);
}


protected void generateBuildSourcesTarget() throws CoreException {
	StringBuffer jars = new StringBuffer();
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		jars.append(",");
		// zip name is jar name without the ".jar" but with "src.zip" appended
		String jar = (String) i.next();
		String zip = jar.substring(0, jar.length() - 4) + "src.zip";
		jars.append(zip);
		generateSourceIndividualTarget(jar, zip);
	}
	script.println();
	script.printTargetDeclaration(1, TARGET_BUILD_SOURCES, TARGET_INIT + jars.toString(), null, null, null);
	script.printString(1, "</target>");
}

protected void generateSourceIndividualTarget(String relativeJar, String target) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, target, TARGET_INIT, null, null, null);
	tab++;
	String fullJar = null;
	try {
		fullJar = new URL(model.getLocation() + relativeJar).getFile();
	} catch (MalformedURLException e) {
		// should not happen
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url") ,e));
	}
	Collection source = (Collection) getTrimmedDevJars().get(fullJar);
	String mapping = ""; 
	String src = ""; 
	if (source != null && !source.isEmpty()) {
		mapping = getStringFromCollection(source, "", "", ",");
		source = trimSlashes(source);
		src = getSourceList(source, "**/*.java");
	}
	if (src.length() != 0) {
		Map properties = new HashMap(1);
		properties.put("mapping", mapping);
		String inclusions = getBuildProperty(PROPERTY_SRC_INCLUDES);
		if (inclusions == null)
			inclusions = src;
		properties.put("includes", inclusions);
		String exclusions = getBuildProperty(PROPERTY_SRC_EXCLUDES);
		if (exclusions == null)
			exclusions = ""; // FIXME: why empty???
		properties.put("excludes", exclusions);
		IPath destination = getRelativeInstallLocation();
		destination = destination.append(target);
		properties.put("dest", destination.toString());
		script.printAntTask(tab, "${template}", null, TARGET_SRC, null, null, properties);
	}
	tab--;
	script.printString(tab, "</target>");
}

/**
 * FIXME: add comment
 */
protected String getSourceList (Collection source, String ending) {
	ArrayList srcList = new ArrayList(source.size());
	for (Iterator i = source.iterator(); i.hasNext();) {
		String entry = (String)i.next();
		srcList.add(entry.endsWith("/") ? entry + ending : entry);
	}
	return getStringFromCollection(srcList, "", "", ",");
}

protected void generateGatherSourcesTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	baseDestination = baseDestination.append(getDirectoryName());
	List destinations = new ArrayList(5);
	IPath baseSource = new Path(getPropertyFormat(PROPERTY_INSTALL));
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		String jar = (String) i.next();
		String zip = jar.substring(0, jar.length() - 4) + "src.zip";
		IPath destination = baseDestination.append(jar).removeLastSegments(1); // remove the jar name
		if (!destinations.contains(destination)) {
			script.printMkdirTask(tab, destination.toString());
			destinations.add(destination);
		}
		script.printCopyTask(tab, baseSource.append(zip).toString(), destination.toString(), null);
	}
	script.printString(--tab, "</target>");
}



protected void generateBuildJarsTarget() throws CoreException {
	StringBuffer jars = new StringBuffer();
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		jars.append(',');
		String currentJar = (String) i.next();
		jars.append(currentJar);
		generateJarIndividualTarget(currentJar);
	}
	script.println();
	script.printTargetDeclaration(1, TARGET_BUILD_JARS, TARGET_INIT + jars.toString(), null, null, null);
	script.printString(1, "</target>");
}


protected void generateJarIndividualTarget(String jarName) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, jarName, TARGET_INIT, null, null, null);
	tab++;
	String fullJar = null;
	try {
		fullJar = new URL(model.getLocation() + jarName).getFile();
	} catch (MalformedURLException e) {
		// should not happen
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_MALFORMED_URL, Policy.bind("exception.url") ,e));
	}
	Collection source = (Collection) getTrimmedDevJars().get(fullJar);
	String mapping = ""; 
	String src = ""; 
	if (source != null && !source.isEmpty()) {
		mapping = getStringFromCollection(source, "", "", ",");
		source = trimSlashes(source);
		src = getStringFromCollection(source, "", "", ",");
	}
	if (src.length() != 0) {
		String compilePath = computeCompilePathClause(fullJar);
		Map properties = new HashMap(1);
		properties.put("mapping", mapping);
		properties.put("includes", src);
		properties.put("excludes", ""); // FIXME: why empty??? should we bother leaving it here??
		IPath destination = getRelativeInstallLocation();
		destination = destination.append(jarName);
		properties.put("dest", destination.toString());
		properties.put("compilePath", compilePath);
		script.printAntTask(tab, "${template}", null, TARGET_JAR, null, null, properties);
	}
	tab--;
	script.printString(tab, "</target>");
}

protected IPath getRelativeInstallLocation() {
	IPath destination = new Path(getPropertyFormat(PROPERTY_INSTALL));
	destination = destination.append(getDirectoryName());
	return destination;
}

/**
 * FIXME: 
 *		+ add comments
 * 		+ figure out if this is the best way of using dev entries
 *			+ what if boot and runtime are not compiled?
 */
protected String computeCompilePathClause(String fullJar) throws CoreException {
	List jars = new ArrayList(9);
	PluginModel runtime = getRegistry().getPlugin(PI_RUNTIME);
	if (runtime == null)
		throw new CoreException(new Status(IStatus.WARNING, PI_PDECORE, EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", PI_RUNTIME), null));
	else {
		String runtimeLocation = getModelLocation(runtime);
		if (devEntries != null)
			for (Iterator i = devEntries.iterator(); i.hasNext();) 
				addEntry(jars, runtimeLocation + i.next());
		addEntry(jars, runtimeLocation + PI_RUNTIME_JAR_NAME);
		// The boot jar must be located relative to the runtime jar.
		// This reflects the actual runtime requirements.
		String pluginsLocation = new Path(runtimeLocation).removeLastSegments(1).toString();
		if (devEntries != null)
			for (Iterator i = devEntries.iterator(); i.hasNext();) 
				addEntry(jars, pluginsLocation + PI_BOOT + "/" +  i.next());
		addEntry(jars, pluginsLocation + PI_BOOT + "/" + PI_BOOT_JAR_NAME);
	}

	for (Iterator i = getRequiredJars().iterator(); i.hasNext();) 
		addEntry(jars, i.next());
	// see if the relative jar is in variable form (e.g., {ws/win32})
	String[] var = extractVars(fullJar);
	// add the dev jars which match the ws of the relative jar
	for (Iterator i = devJars.keySet().iterator(); i.hasNext();) {
		String jar = (String)i.next();
		String[] jarVar = extractVars(jar);
		if (jarVar[0] != null && jarVar[0].equals(var[0]) && jarVar[1].equals(var[1]))
			addEntry(jars, jar);
	}
	jars.remove(fullJar);
	if (var[0] != null) {
		int start = fullJar.indexOf('{');
		int end = fullJar.indexOf('}');
		String resolvedFullJar = fullJar; 
		resolvedFullJar = fullJar.substring(0, start);
		resolvedFullJar += fullJar.substring(start + 1, end);
		resolvedFullJar += fullJar.substring(end + 1);
		jars.remove(resolvedFullJar);
		resolvedFullJar = fullJar.substring(0, start);
		resolvedFullJar += "$" + var[0] + "$";
		resolvedFullJar += fullJar.substring(end + 1);
		jars.remove(resolvedFullJar);
	}
	if (devEntries != null)
		for (Iterator i = devEntries.iterator(); i.hasNext();) 
			jars.remove(getModelLocation(model) + i.next());
	
	List relativeJars = makeRelative(jars, installLocation);
	String result = getStringFromCollection(relativeJars, "", "", ";");
	result = replaceVariables(result);
	return result;
}

/**
 * Makes the list of jars relative to the base property.
 */
protected List makeRelative(List jars, String baseLocation) {
	List result = new ArrayList(jars.size());
	for (Iterator i = jars.iterator(); i.hasNext();)
		addEntry(result, makeRelative(getPropertyFormat(PROPERTY_INSTALL), (String) i.next(), baseLocation));
	return result;
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

protected String[] extractVars(String entry) {
	// see if the relative jar is in variable form (e.g., {ws/win32})
	String[] result = new String[2];
	int start = entry.indexOf('{');
	if (start > -1) {
		result[0] = entry.substring(start + 1, start + 3);
		int end = entry.indexOf('}', start);
		result[1] = entry.substring(start + 4, end);
	}
	return result;
}

/**
 * FIXME: add comments
 */
protected List getJars(PluginModel descriptor) throws CoreException {
	List result = (List) jarsCache.get(descriptor.getId());
	if (result != null)
		return result;
	result = new ArrayList(9);
	LibraryModel[] libs = descriptor.getRuntime();
	
	if (libs != null) {
		if (devEntries != null)
			for (Iterator i = devEntries.iterator(); i.hasNext();) 
				addEntry(result, getModelLocation(descriptor) + i.next());
		for (int i = 0; i < libs.length; i++)
			addEntry(result, getModelLocation(descriptor) + libs[i].getName());
	}
	
	PluginPrerequisiteModel[] prereqs = descriptor.getRequires();
	if (prereqs != null) {
		for (int i = 0; i < prereqs.length; i++) {
			PluginModel prereq = getRegistry().getPlugin(prereqs[i].getPlugin());
			if (prereq != null) {
				List prereqJars = getJars(prereq);
				for (Iterator j = prereqJars.iterator(); j.hasNext();) 
					addEntry(result, j.next());
			}
		}
	}
	
	jarsCache.put(descriptor.getId(), result);
	return result;
}

protected void addEntry(List list, Object entry) {
	if (list.contains(entry))
		return;
	list.add(entry);
}

/**
 * FIXME: add comment
 */
protected Collection trimSlashes(Collection original) {
	ArrayList result = new ArrayList(original.size());
	for (Iterator i = original.iterator(); i.hasNext();) {
		String entry = (String)i.next();
		if (entry.charAt(0) == '/')
			entry = entry.substring(1);
		result.add(entry);
	}
	return result;
}

/**
 * FIXME: add comments
 */
protected Hashtable trimDevJars(Map devJars) throws CoreException {
	Hashtable result = new Hashtable(9);
	for (Iterator it = devJars.keySet().iterator(); it.hasNext();) {
		String key = (String) it.next();
		Collection list = (Collection) devJars.get(key);			// projects
		File base = null;
		try {
			base = new File(new URL(model.getLocation()).getFile());
		} catch (MalformedURLException e) {
			continue;
		}
		boolean found = false;
		for (Iterator i = list.iterator(); i.hasNext();) {
			String src = (String) i.next();
			File entry = new File(base, src).getAbsoluteFile();
			if (!entry.exists())
				throw new CoreException(new Status(IStatus.WARNING, PI_PDECORE, WARNING_MISSING_SOURCE, Policy.bind("warning.cannotLocateSource", entry.getPath()), null));
			else
				found = true;;
		}
		if (found)
			result.put(key, list);
	}
	return result;
}

protected void generateGatherBinPartsTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	tab++;
	Map properties = new HashMap(1);
	properties.put("includes", getPropertyFormat(PROPERTY_BIN_INCLUDES));
	properties.put("excludes", getPropertyFormat(PROPERTY_BIN_EXCLUDES));
	IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	destination = destination.append(getDirectoryName());
	properties.put("dest", destination.toString());
	script.printAntTask(tab, "${template}", null, "includesExcludesCopy", null, null, properties);
	tab--;
	script.printString(tab, "</target>");
}

protected void generatePropertiesTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_PROPERTIES, null, null, null, null);
	tab++;
	generateMandatoryProperties(tab);
	generateConditionalProperties(tab);
	tab--;
	script.printEndTag(tab, "target");
}

protected void generateConditionalProperties(int tab) {
	for (Iterator i = getConditionalProperties().entrySet().iterator(); i.hasNext();) {
		Map.Entry entry = (Map.Entry) i.next();
		String realKey = (String) entry.getKey();
		StringBuffer realValue = new StringBuffer();
		realValue.append(getBuildProperty(realKey));
		Map variations = (Map) entry.getValue();
		int n = 0;
		for (Iterator j = variations.entrySet().iterator(); j.hasNext();) {
			Map.Entry variation = (Map.Entry) j.next();
			String key = (String) variation.getKey();
			String[] conditions = getConditions(key);
			Condition cond = new Condition(Condition.TYPE_AND);
			for (int k = 0; k < conditions.length; k++) {
				String[] condition = Utils.getArrayFromString(conditions[k], "/");
				String variable = condition[0];
				String value = condition[1];
				cond.addEquals(getPropertyFormat(variable), value);
			}
			String conditionKey = "condition" + (++n) + "." + realKey;
			script.printProperty(tab, conditionKey, "");
			ConditionTask task = new ConditionTask(conditionKey, "," + variation.getValue(), cond);
			script.print(tab, task);
			realValue.append(getPropertyFormat(conditionKey));
		}
		script.printProperty(tab, realKey, realValue.toString());
	}
}

/**
 * 
 */
protected void generateMandatoryProperties(int tab) {
	if (!getConditionalProperties().containsKey(PROPERTY_BIN_INCLUDES)) {
		String value = getBuildProperty(PROPERTY_BIN_INCLUDES);
		if (value == null)
			value = "**";
		script.printProperty(tab, PROPERTY_BIN_INCLUDES, value);
	}
	if (!getConditionalProperties().containsKey(PROPERTY_BIN_EXCLUDES)) {
		String value = getBuildProperty(PROPERTY_BIN_EXCLUDES);
		if (value == null)
			value = computeCompleteSrc();
		script.printProperty(tab, PROPERTY_BIN_EXCLUDES, value);
	}
}


/**
 * FIXME: add comment
 */
protected String computeCompleteSrc() {
	Set jars = new HashSet(9);
	for (Iterator i = getDevJars().values().iterator(); i.hasNext();)
		jars.addAll((Collection)i.next());
	return getStringFromCollection(jars, "", "", ",");
}

protected Map getDevJars() {
	if (devJars == null)
		devJars = computeJarDefinitions();
	return devJars;
}

protected List getRequiredJars() throws CoreException {
	if (requiredJars == null)
		requiredJars = getJars(model);
	return requiredJars;
}

protected Map getTrimmedDevJars() throws CoreException {
	if (trimmedDevJars == null)
		trimmedDevJars = trimDevJars(getDevJars());
	return trimmedDevJars;
}

/**
 * FIXME: add comment
 */
protected String getStringFromCollection(Collection list, String prefix, String suffix, String separator) {
	StringBuffer result = new StringBuffer();
	boolean first = true;
	for (Iterator i = list.iterator(); i.hasNext();) {
		if (!first)
			result.append(separator);
		first = false;
		result.append(prefix);
		result.append((String) i.next());
		result.append(suffix);
	}
	return result.toString();
}


protected void generateUpdateJarTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = getRelativeInstallLocation();
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
	script.printProjectDeclaration(model.getId(), TARGET_INIT, ".");
	script.println();
	script.printTargetDeclaration(tab, "initTemplate", null, null, PROPERTY_TEMPLATE, null);
	tab++;
	script.printString(tab, "<initTemplate/>");
	tab--;
	script.printString(tab, "</target>");
	script.println();
	script.printTargetDeclaration(tab, TARGET_INIT, "initTemplate, " + TARGET_PROPERTIES, null, null, null);
	tab++;
	script.printProperty(tab, getModelTypeName(), model.getId());
	script.printProperty(tab, "version", model.getVersion());
	tab--;
	script.printString(tab, "</target>");
}

protected abstract String getModelTypeName();

/**
 * Sets the PluginModel to generate script from.
 */
public void setModel(PluginModel model) throws CoreException {
	if (model == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));
	this.model = model;
	devJars = null;
	jarOrder = null;
	jarsCache = new HashMap(5);
	trimmedDevJars = null;
	requiredJars = null;
	readProperties(getModelLocation(model));
}

/**
 * Sets model to generate scripts from.
 */
public void setModelId(String modelId) throws CoreException {
	PluginModel newModel = getModel(modelId);
	if (newModel == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ELEMENT_MISSING, Policy.bind("exception.missingElement", modelId), null));
	setModel(newModel);
}

protected abstract PluginModel getModel(String modelId) throws CoreException;

protected Map computeJarDefinitions() {
	jarOrder = new ArrayList();
	Map result = new HashMap(5);
	String base = getModelLocation(model);
	int n = PROPERTY_SOURCE_PREFIX.length();
	for (Iterator iterator = getBuildProperties().entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (!key.startsWith(PROPERTY_SOURCE_PREFIX))
			continue;
		key = key.substring(n);
		jarOrder.add(key);
		result.put(base + key, getListFromString((String) entry.getValue()));
	}
	return result;
}

protected List getListFromString(String prop) {
	if (prop == null || prop.trim().equals(""))
		return new ArrayList(0);
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(prop, ","); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return result;
}


protected String[] getConditions(String key) {
	int prefix = key.indexOf(PROPERTY_ASSIGNMENT_PREFIX);
	int suffix = key.indexOf(PROPERTY_ASSIGNMENT_SUFFIX);
	return Utils.getArrayFromString(key.substring(prefix + PROPERTY_ASSIGNMENT_PREFIX.length(), suffix));
}

}
