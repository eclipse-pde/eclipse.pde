package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.core.runtime.model.PluginPrerequisiteModel;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates build.xml script for features.
 */
public class FeatureBuildScriptGenerator extends AbstractBuildScriptGenerator {

	/**
	 * Indicates whether scripts for this feature's children should be generated.
	 */
	protected boolean generateChildrenScript = true;

	/**
	 * 
	 */
	protected String featureID;

	/**
	 * Where to get the feature description from.
	 */
	protected String featureRootLocation;

	/**
	 * Target feature.
	 */
	protected Feature feature;

/**
 * Returns a list of PluginModel objects representing the elements. The boolean
 * argument indicates whether the list should consist of plug-ins or fragments.
 */
protected List computeElements(boolean fragments) throws CoreException {
	List result = new ArrayList(5);
	IPluginEntry[] pluginList = feature.getPluginEntries();
	for (int i = 0; i < pluginList.length; i++) {
		IPluginEntry entry = pluginList[i];
		if (fragments == entry.isFragment()) {
			VersionedIdentifier identifier = entry.getVersionedIdentifier();
			PluginModel model = getRegistry().getPlugin(identifier.getIdentifier(), identifier.getVersion().toString());
			if (model == null)
				throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", entry.getVersionedIdentifier().toString()), null));
			else
				result.add(model);
		}
	}
	return result;
}

public void setGenerateChildrenScript(boolean generate) {
	generateChildrenScript = generate;
}


public void generate() throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null));
	if (installLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_INSTALL_LOCATION_MISSING, Policy.bind("error.missingInstallLocation"), null));

	String custom = getBuildProperty(PROPERTY_CUSTOM);
	if (custom != null && custom.equalsIgnoreCase("true"))
		return;
	readFeature();

	if (generateChildrenScript)
		generateChildrenScripts();

	try {
		File root = new File(getFeatureRootLocation());
		File target = new File(root, DEFAULT_BUILD_SCRIPT_FILENAME);
		PrintWriter output = new PrintWriter(new FileOutputStream(target));
		try {
			generateBuildScript(output);
		} finally {
			output.flush();
			output.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
}

/**
 * Main call for generating the script.
 */
protected void generateBuildScript(PrintWriter output) throws CoreException {
	generatePrologue(output);

	generateAllPluginsTarget(output);
	generateAllFragmentsTarget(output);
	generateAllChildrenTarget(output);
	generateChildrenTarget(output);
	generateBuildJarsTarget(output);

	generateUpdateJarTarget(output);
	generateGatherBinPartsTarget(output);

	generateZipDistributionWholeTarget(output);

	generateBuildSourcesTarget(output);
	generateZipSourcesTarget(output);
	generateGatherSourcesTarget(output);

	generateGatherLogTarget(output);
	generateZipLogsTarget(output);
	generateCleanTarget(output);

	generateEpilogue(output);
}

/**
 * FIXME: add comments
 */
protected void generateBuildJarsTarget(PrintWriter output) throws CoreException {
	StringBuffer jars = new StringBuffer();
	Properties props = getBuildProperties();
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			jars.append(',');
			jars.append(jarName);
			generateJarIndividualTarget(output, jarName, (String) entry.getValue());
		}
	}
	output.println();
	int tab = 1;
	printTargetDeclaration(output, tab, TARGET_BUILD_JARS, TARGET_INIT + jars.toString(), null, null, null);
	tab++;
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_JARS);
	printAntCallTask(output, tab, TARGET_ALL_CHILDREN, null, params);
	tab--;
	printString(output, tab, "</target>");
}

/**
 * FIXME: add comments
 */
protected void generateBuildSourcesTarget(PrintWriter output) throws CoreException {
	StringBuffer sources = new StringBuffer();
	Properties props = getBuildProperties();
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			// zip name is jar name without the ".jar" but with "src.zip" appended
			String sourceName = jarName.substring(0, jarName.length() - 4) + "src.zip";
			sources.append(',');
			sources.append(sourceName);
			generateSourceIndividualTarget(output, sourceName, (String) entry.getValue());
		}
	}
	output.println();
	int tab = 1;
	printTargetDeclaration(output, tab, TARGET_BUILD_SOURCES, TARGET_INIT + sources.toString(), null, null, null);
	tab++;
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
	printAntCallTask(output, tab, TARGET_ALL_CHILDREN, null, params);
	tab--;
	printString(output, tab, "</target>");
}


/**
 * FIXME: add comments
 */
protected void generateJarIndividualTarget(PrintWriter output, String jarName, String jarSource) throws CoreException {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, jarName, TARGET_INIT, null, null, null);
	tab++;
	Map properties = new HashMap(1);
	properties.put("mapping", jarSource);
	properties.put("includes", jarSource);
	properties.put("excludes", ""); // FIXME: why empty??? should we bother leaving it here??
	IPath destination = new Path(getPropertyFormat(PROPERTY_INSTALL));
	destination = destination.append(DEFAULT_FEATURE_LOCATION);
	destination = destination.append(getPropertyFormat(PROPERTY_FEATURE));
	destination = destination.append(jarName);
	properties.put("dest", destination.toString());
	properties.put("compilePath", ""); // FIXME: why empty??? should we bother leaving it here??
	printAntTask(output, tab, "${template}", null, TARGET_JAR, null, null, properties);
	tab--;
	printString(output, tab, "</target>");
}


protected void generateCleanTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_CLEAN, TARGET_INIT, null, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put("target", TARGET_CLEAN);
	printAntCallTask(output, tab, TARGET_ALL_CHILDREN, null, params);
	FileSet[] fileSet = new FileSet[3];
	fileSet [0] = new FileSet(".", null, "*.pdetemp", null, null, null, null);
	fileSet [1] = new FileSet(".", null, "${feature}*.jar", null, null, null, null);
	fileSet [2] = new FileSet(".", null, "${feature}*.zip", null, null, null, null);
	printDeleteTask(output, tab, null, null, fileSet);
	tab--;
	printString(output, tab, "</target>");
}

protected void generateZipLogsTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_ZIP_LOGS, TARGET_INIT, null, null, null);
	tab++;
	IPath base = getRelativeInstallLocation();
	base = base.append("_temp_");
	printProperty(output, tab, PROPERTY_BASE, base.toString());
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_LOG);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_BASE));
	printAntCallTask(output, tab, TARGET_ALL_CHILDREN, "false", params);
	printAntCallTask(output, tab, TARGET_GATHER_LOG, "false", params);
	IPath destination = getRelativeInstallLocation().append("${feature}.log.zip");
	printZipTask(output, tab, destination.toString(), getPropertyFormat(PROPERTY_BASE));
	printDeleteTask(output, tab, getPropertyFormat(PROPERTY_BASE), null, null);
	tab--;
	printString(output, tab, "</target>");
}

protected void generateGatherLogTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_GATHER_LOG, TARGET_INIT, null, null, null);
	tab++;
	IPath base = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	base = base.append(DEFAULT_FEATURE_LOCATION);
	base = base.append(getPropertyFormat(PROPERTY_FEATURE));
	printProperty(output, tab, PROPERTY_BASE, base.toString());
	printMkdirTask(output, tab, getPropertyFormat(PROPERTY_BASE));
	FileSet fileSet = new FileSet(getRelativeInstallLocation().toString(), null, "*.log", null, null, null, null);
	printCopyTask(output, tab, null, getPropertyFormat(PROPERTY_BASE), new FileSet[] {fileSet});
	tab--;
	printString(output, tab, "</target>");
}


protected void generateGatherSourcesTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	tab++;
	IPath destination = getRelativeInstallLocation();
	String dest = destination.toString();
	printMkdirTask(output, tab, dest);
	Properties props = getBuildProperties();
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			// zip name is jar name without the ".jar" but with "src.zip" appended
			String zip = jarName.substring(0, jarName.length() - 4) + "src.zip";
			printCopyTask(output, tab, destination.append(zip).toString(), dest, null);
		}
	}
	tab--;
	printString(output, tab, "</target>");
}

protected void generateZipSourcesTarget(PrintWriter output) {
	String featurebase = getPropertyFormat(PROPERTY_FEATURE_BASE);
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = getRelativeInstallLocation();
	printProperty(output, tab, PROPERTY_FEATURE_BASE, destination.append("zip.sources.pdetemp").toString());
	printDeleteTask(output, tab, featurebase, null, null);
	printMkdirTask(output, tab, featurebase);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION, featurebase);
	printAntCallTask(output, tab, TARGET_GATHER_SOURCES, null, params);
	params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
	printAntCallTask(output, tab, TARGET_ALL_CHILDREN, null, params);
	printZipTask(output, tab, destination.append("${feature}_src_${featureVersion}.zip").toString(), "${feature.base}");
	printDeleteTask(output, tab, featurebase, null, null);
	tab--;
	printString(output, tab, "</target>");
}

protected IPath getRelativeInstallLocation() {
	IPath destination = new Path(getPropertyFormat(PROPERTY_INSTALL));
	destination = destination.append(DEFAULT_FEATURE_LOCATION);
	destination = destination.append(getPropertyFormat(PROPERTY_FEATURE));
	return destination;
}


protected void generateGatherBinPartsTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_FEATURE_BASE));
	printAntCallTask(output, tab, TARGET_CHILDREN, null, params);
	String inclusions = getBuildProperty(PROPERTY_BIN_INCLUDES);
	if (inclusions == null)
		inclusions = "";
	String exclusions = getBuildProperty(PROPERTY_BIN_EXCLUDES);
	if (exclusions == null)
		exclusions = "";
	params.clear(); // they are properties, not params, but we'll reuse the variable
	params.put("includes", inclusions);
	params.put("excludes", exclusions);
	params.put("dest", "${feature.base}/install/features/${feature}");
	printAntTask(output, tab, "${template}", null, "includesExcludesCopy", null, null, params);
	tab--;
	printString(output, tab, "</target>");
}


protected void generateUpdateJarTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
	printAntCallTask(output, tab, TARGET_ALL_CHILDREN, null, params);
	printAntCallTask(output, tab, TARGET_BUILD_JARS, null, null);
	IPath destination = getRelativeInstallLocation();
	printProperty(output, tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	printDeleteTask(output, tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	printMkdirTask(output, tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	// be sure to call the gather with children turned off.  The only way to do this is 
	// to clear all inherited values.  Must remember to setup anything that is really expected.
	params.clear();
	params.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_FEATURE_BASE));
	printAntCallTask(output, tab, TARGET_GATHER_BIN_PARTS, "false", params);
	printJarTask(output, tab, destination.append("${feature}_${featureVersion}.jar").toString(), "${feature.base}");
	printDeleteTask(output, tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	printString(output, tab, "</target>");
}



/**
 * Zip up the whole feature.
 */
protected void generateZipDistributionWholeTarget(PrintWriter output) {
	int tab = 1;
	output.println();
	// FIXME: should not have releng on the name
	printTargetDeclaration(output, tab, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = getRelativeInstallLocation();
	printProperty(output, tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	printDeleteTask(output, tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	printMkdirTask(output, tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	Map params = new HashMap(1);
	params.put(PROPERTY_INCLUDE_CHILDREN, "true");
	printAntCallTask(output, tab, TARGET_GATHER_BIN_PARTS, null, params);
	printZipTask(output, tab, destination.append("${feature}_${featureVersion}.bin.dist.zip").toString(), getPropertyFormat(PROPERTY_FEATURE_BASE));
	printDeleteTask(output, tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	printString(output, tab, "</target>");
}



/**
 * Executes a given target in all children's script files.
 */
protected void generateAllChildrenTarget(PrintWriter output) {
	StringBuffer depends = new StringBuffer();
	depends.append(TARGET_INIT);
	depends.append(",");
	depends.append(TARGET_ALL_PLUGINS);
	depends.append(",");
	depends.append(TARGET_ALL_FRAGMENTS);
	
	output.println();
	printTargetDeclaration(output, 1, TARGET_ALL_CHILDREN, depends.toString(), null, null, null);
	printString(output, 1, "</target>");
}

protected void generateSourceIndividualTarget(PrintWriter output, String name, String source) throws CoreException {
	int tab = 1;
	output.println();
	printTargetDeclaration(output, tab, name, TARGET_INIT, null, null, null);
	tab++;
	Map properties = new HashMap(1);
	properties.put("mapping", source);
	properties.put("includes", source);
	properties.put("excludes", ""); // FIXME: why empty??? should we bother leaving it here??
	IPath destination = new Path(getPropertyFormat(PROPERTY_INSTALL));
	destination = destination.append(DEFAULT_FEATURE_LOCATION);
	destination = destination.append(getPropertyFormat(PROPERTY_FEATURE));
	destination = destination.append(name);
	properties.put("dest", destination.toString());
	printAntTask(output, tab, "${template}", null, TARGET_SRC, null, null, properties);
	tab--;
	printString(output, tab, "</target>");
}

/**
 * Target responsible for delegating target calls to plug-in's build.xml scripts.
 */
protected void generateAllPluginsTarget(PrintWriter output) throws CoreException {
	int tab = 1;
	List plugins = computeElements(false);
	String[][] sortedPlugins = computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]));
	output.println();
	printTargetDeclaration(output, tab, TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
	tab++;
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			PluginModel plugin = getRegistry().getPlugin(sortedPlugins[list][i]);
			String location = makeRelative(getPropertyFormat(PROPERTY_INSTALL), getModelLocation(plugin), installLocation);
			printAntTask(output, tab, "build.xml", location, getPropertyFormat(PROPERTY_TARGET), null, null, null);
		}
	}
	tab--;
	printString(output, tab, "</target>");
}

/**
 * Target responsible for delegating target calls to fragments's build.xml scripts.
 */
protected void generateAllFragmentsTarget(PrintWriter output) throws CoreException {
	int tab = 1;
	List fragments = computeElements(true);
	output.println();
	printTargetDeclaration(output, tab, TARGET_ALL_FRAGMENTS, TARGET_INIT, null, null, null);
	tab++;
	for (Iterator iterator = fragments.iterator(); iterator.hasNext();) {
		PluginModel fragment = (PluginModel) iterator.next();
		String location = makeRelative(getPropertyFormat(PROPERTY_INSTALL), getModelLocation(fragment), installLocation);
		printAntTask(output, tab, "build.xml", location, getPropertyFormat(PROPERTY_TARGET), null, null, null);
	}
	tab--;
	printString(output, tab, "</target>");
}






/**
 * 
 */
protected String[][] computePrerequisiteOrder(PluginModel[] plugins) {
	List prereqs = new ArrayList(9);
	Set pluginList = new HashSet(plugins.length);
	for (int i = 0; i < plugins.length; i++) 
		pluginList.add(plugins[i].getId());
	// create a collection of directed edges from plugin to prereq
	for (int i = 0; i < plugins.length; i++) {
		boolean boot = false;
		boolean runtime = false;
		boolean found = false;
		PluginPrerequisiteModel[] prereqList = plugins[i].getRequires();
		if (prereqList != null) {
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				String prereq = prereqList[j].getPlugin();
				boot = boot || prereq.equals(BootLoader.PI_BOOT);
				runtime = runtime || prereq.equals(Platform.PI_RUNTIME);
				if (pluginList.contains(prereq)) {
					found = true;
					prereqs.add(new String[] { plugins[i].getId(), prereq });
				}
			}
		}
		// if we didn't find any prereqs for this plugin, add a null prereq 
		// to ensure the value is in the output
		if (!found)
			prereqs.add(new String[] { plugins[i].getId(), null });
		// if we didn't find the boot or runtime plugins as prereqs and they are in the list
		// of plugins to build, add prereq relations for them.  This is required since the 
		// boot and runtime are implicitly added to a plugin's requires list by the platform runtime.
		// Note that we should skip the xerces plugin as this would cause a circularity.
		if (plugins[i].getId().equals("org.apache.xerces"))
			continue;
		if (!boot && pluginList.contains(BootLoader.PI_BOOT) && !plugins[i].getId().equals(BootLoader.PI_BOOT))
			prereqs.add(new String[] { plugins[i].getId(), BootLoader.PI_BOOT});
		if (!runtime && pluginList.contains(Platform.PI_RUNTIME) && !plugins[i].getId().equals(Platform.PI_RUNTIME) && !plugins[i].getId().equals(BootLoader.PI_BOOT))
			prereqs.add(new String[] { plugins[i].getId(), Platform.PI_RUNTIME});
	}
	// do a topological sort and return the prereqs
	String[][] prereqArray = (String[][]) prereqs.toArray(new String[prereqs.size()][]);
	return computeNodeOrder(prereqArray);
}

/**
 * 
 */
protected String[][] computeNodeOrder(String[][] specs) {
	HashMap counts = computeCounts(specs);
	List nodes = new ArrayList(counts.size());
	while (!counts.isEmpty()) {
		List roots = findRootNodes(counts);
		if (roots.isEmpty())
			break;
		for (Iterator i = roots.iterator(); i.hasNext();)
			counts.remove(i.next());
		nodes.addAll(roots);
		removeArcs(specs, roots, counts);
	}
	String[][] result = new String[2][];
	result[0] = (String[]) nodes.toArray(new String[nodes.size()]);
	result[1] = (String[]) counts.keySet().toArray(new String[counts.size()]);
	return result;
}

/**
 * 
 */
protected static void removeArcs(String[][] mappings, List roots, HashMap counts) {
	for (Iterator j = roots.iterator(); j.hasNext();) {
		String root = (String) j.next();
		for (int i = 0; i < mappings.length; i++) {
			if (root.equals(mappings[i][1])) {
				String input = mappings[i][0];
				Integer count = (Integer) counts.get(input);
				if (count != null)
					counts.put(input, new Integer(count.intValue() - 1));
			}
		}
	}
}

/**
 * 
 */
protected List findRootNodes(HashMap counts) {
	List result = new ArrayList(5);
	for (Iterator i = counts.keySet().iterator(); i.hasNext();) {
		String node = (String) i.next();
		int count = ((Integer) counts.get(node)).intValue();
		if (count == 0)
			result.add(node);
	}
	return result;
}

/**
 * 
 */
protected HashMap computeCounts(String[][] mappings) {
	HashMap counts = new HashMap(5);
	for (int i = 0; i < mappings.length; i++) {
		String from = mappings[i][0];
		Integer fromCount = (Integer) counts.get(from);
		String to = mappings[i][1];
		if (to == null)
			counts.put(from, new Integer(0));
		else {
			if (((Integer) counts.get(to)) == null)
				counts.put(to, new Integer(0));
			fromCount = fromCount == null ? new Integer(1) : new Integer(fromCount.intValue() + 1);
			counts.put(from, fromCount);
		}
	}
	return counts;
}

/**
 * Just ends the script.
 */
protected void generateEpilogue(PrintWriter output) {
	output.println();
	output.println("</project>");
}

/**
 * Defines, the XML declaration, Ant project and init target.
 */
protected void generatePrologue(PrintWriter output) {
	int tab = 1;
	output.println(XML_PROLOG);
	printProjectDeclaration(output, feature.getFeatureIdentifier(), TARGET_INIT, ".");
	output.println();
	printTargetDeclaration(output, tab, TARGET_INIT, null, null, null, null);
	tab++;
	printString(output, tab, "<initTemplate/>");
	printProperty(output, tab, "feature", feature.getFeatureIdentifier());
	printProperty(output, tab, "featureVersion", feature.getFeatureVersion());
	tab--;
	printString(output, tab, "</target>");
}


protected void generateChildrenScripts() throws CoreException {
	generateModels(new PluginBuildScriptGenerator(), computeElements(false));
	generateModels(new PluginBuildScriptGenerator(), computeElements(true));
}

protected void generateModels(ModelBuildScriptGenerator generator, List models) throws CoreException {
	if (models.isEmpty())
		return;
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setBuildVariableARCH(buildVariableARCH);
	generator.setBuildVariableNL(buildVariableNL);
	generator.setBuildVariableOS(buildVariableOS);
	generator.setBuildVariableWS(buildVariableWS);
	for (Iterator iterator = models.iterator(); iterator.hasNext();) {
		PluginModel model = (PluginModel) iterator.next();
		// setModel has to be called before configurePersistentProperties
		// because it reads the model's properties
		generator.setModel(model);
		configurePersistentProperties(generator);
		generator.generate();
	}
}

/**
 * Propagates properties that are set for this feature but should
 * overwrite any values set for the children.
 */
protected void configurePersistentProperties(AbstractBuildScriptGenerator generator) {
	for (int i = 0; i < PERSISTENT_PROPERTIES.length; i++) {
		String key = PERSISTENT_PROPERTIES[i];
		String value = getBuildProperty(key);
		if (value == null)
			continue;
		generator.setBuildProperty(key, value);
	}
}





public void setFeature(String featureID) throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null));
	this.featureID = featureID;
	readProperties(getFeatureRootLocation());
}

/**
 * Reads the target feature from the specified location.
 */
protected void readFeature() throws CoreException {
	String location = getFeatureRootLocation();
	if (location == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureLocation"), null));
	
	FeatureExecutableFactory factory = new FeatureExecutableFactory();
	File file = new File(location);
	try {
		feature = (Feature) factory.createFeature(file.toURL(), null);
		if (feature == null)
			throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), null));	
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), e));
	}
}

/**
 * If the feature location was not specified, use a default one.
 */
protected String getFeatureRootLocation() {
	if (featureRootLocation == null) {
		IPath location = new Path(installLocation);
		location = location.append(DEFAULT_FEATURE_LOCATION);
		location = location.append(featureID);
		featureRootLocation = location.addTrailingSeparator().toOSString();
	}
	return featureRootLocation;
}

/**
 * Delegates some target call to all-template only if the property
 * includeChildren is set.
 */
protected void generateChildrenTarget(PrintWriter output) {
	output.println();
	printTargetDeclaration(output, 1, TARGET_CHILDREN, null, PROPERTY_INCLUDE_CHILDREN, null, null);
	printAntCallTask(output, 2, TARGET_ALL_CHILDREN, null, null);
	printString(output, 1, "</target>");
}

}
