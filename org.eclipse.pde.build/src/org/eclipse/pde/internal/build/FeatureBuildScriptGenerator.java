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
import org.eclipse.pde.internal.build.ant.*;
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
		if (fragments == entry.isFragment()) { // filter the plugins or fragments
			VersionedIdentifier identifier = entry.getVersionedIdentifier();
			PluginModel model;
			if (fragments)
				model = getRegistry().getFragment(identifier.getIdentifier(), identifier.getVersion().toString());
			else
				model = getRegistry().getPlugin(identifier.getIdentifier(), identifier.getVersion().toString());
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
	pluginLocations = new HashMap(20);

	if (generateChildrenScript)
		generateChildrenScripts();

	try {
		File root = new File(getFeatureRootLocation());
		File target = new File(root, buildScriptName);
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
	generateAllPluginsTarget();
	generateAllFragmentsTarget();
	generateAllChildrenTarget();
	generateChildrenTarget();
	generateBuildJarsTarget();
	generateBuildZipsTarget();
	generateBuildUpdateJarTarget();
	generateGatherBinPartsTarget();
	generateZipDistributionWholeTarget();
	generateBuildSourcesTarget();
	generateZipSourcesTarget();
	generateGatherSourcesTarget();
	generateGatherLogTarget();
	generateZipLogsTarget();
	generateCleanTarget();
	generatePropertiesTarget();
	generateEpilogue();
}
/**
 * FIXME: add comments
 */
protected void generateBuildJarsTarget() throws CoreException {
	StringBuffer jars = new StringBuffer();
	Properties props = getBuildProperties();
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_JAR_SUFFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			jars.append(',');
			jars.append(jarName);
			generateJarIndividualTarget(jarName, (String) entry.getValue());
		}
	}
	script.println();
	int tab = 1;
	script.printTargetDeclaration(tab, TARGET_BUILD_JARS, TARGET_INIT + jars.toString(), null, null, null);
	tab++;
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_JARS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * FIXME: add comments
 */
protected void generateBuildZipsTarget() throws CoreException {
	StringBuffer zips = new StringBuffer();
	Properties props = getBuildProperties();
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
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_ZIPS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printString(--tab, "</target>");
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
/**
 * FIXME: add comments
 */
protected void generateBuildSourcesTarget() throws CoreException {
	StringBuffer sources = new StringBuffer();
	Properties props = getBuildProperties();
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_JAR_SUFFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			// zip name is jar name without the ".jar" but with "src.zip" appended
			String sourceName = jarName.substring(0, jarName.length() - 4) + "src.zip";
			sources.append(',');
			sources.append(sourceName);
			generateSourceIndividualTarget(sourceName, (String) entry.getValue());
		}
	}
	script.println();
	int tab = 1;
	script.printTargetDeclaration(tab, TARGET_BUILD_SOURCES, TARGET_INIT + sources.toString(), null, null, null);
	tab++;
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * FIXME: add comments
 */
protected void generateJarIndividualTarget(String jarName, String jarSource) throws CoreException {
	String basedir = getPropertyFormat(PROPERTY_BASEDIR);
	IPath destination = new Path(basedir);
	destination = destination.append(jarName);
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, jarName, TARGET_INIT, null, null, null);
	Map properties = new HashMap(1);
	properties.put("mapping", jarSource);
	properties.put("includes", jarSource);
	properties.put("excludes", ""); // FIXME: why empty??? should we bother leaving it here??
	properties.put("dest", destination.toString());
	properties.put("srcdir", basedir);
	properties.put("compilePath", ""); // FIXME: why empty??? should we bother leaving it here??
	script.printAntTask(tab, getPropertyFormat(PROPERTY_TEMPLATE), null, TARGET_JAR, null, null, properties);
	script.printString(--tab, "</target>");
}
protected void generateCleanTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_CLEAN, TARGET_INIT, null, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put("target", TARGET_CLEAN);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	FileSet[] fileSet = new FileSet[3];
	fileSet [0] = new FileSet(".", null, "*.pdetemp", null, null, null, null);
	fileSet [1] = new FileSet(".", null, "${feature}*.jar", null, null, null, null);
	fileSet [2] = new FileSet(".", null, "${feature}*.zip", null, null, null, null);
	script.printDeleteTask(tab, null, null, fileSet);
	tab--;
	script.printString(tab, "</target>");
}
protected void generateZipLogsTarget() {
	IPath base = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	base = base.append("_temp_");
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_LOGS, TARGET_INIT, null, null, null);
	script.printProperty(tab, PROPERTY_BASE, base.toString());
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_LOGS);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_BASE));
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, "false", params);
	script.printAntCallTask(tab, TARGET_GATHER_LOGS, "false", params);
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR)).append("${feature}.log.zip");
	script.printZipTask(tab, destination.toString(), getPropertyFormat(PROPERTY_BASE));
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_BASE), null, null);
	script.printString(--tab, "</target>");
}
protected void generateGatherLogTarget() {
	String source = new Path(getPropertyFormat(PROPERTY_BASEDIR)).toString();
	String destination = new Path(getPropertyFormat(PROPERTY_DESTINATION)).append(getDirectoryName()).toString();
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_LOGS, TARGET_INIT, null, null, null);
	script.printMkdirTask(tab, destination);
	FileSet fileSet = new FileSet(source, null, "*.log", null, null, null, null);
	script.printCopyTask(tab, null, destination, new FileSet[] {fileSet});
	script.printString(--tab, "</target>");
}
protected void generateGatherSourcesTarget() {
	IPath source = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	destination = destination.append(getDirectoryName());
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	script.printMkdirTask(tab, destination.toString());
	Properties props = getBuildProperties();
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_JAR_SUFFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			// zip name is jar name without the ".jar" but with "src.zip" appended
			String zip = jarName.substring(0, jarName.length() - 4) + "src.zip";
			script.printCopyTask(tab, source.append(zip).toString(), destination.toString(), null);
		}
	}
	script.printString(--tab, "</target>");
}
/**
 * 
 */
protected String getDirectoryName() {
	return "install/features/${feature}";
}
protected void generateZipSourcesTarget() {
	String featurebase = getPropertyFormat(PROPERTY_FEATURE_BASE);
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("zip.sources.pdetemp").toString());
	script.printDeleteTask(tab, featurebase, null, null);
	script.printMkdirTask(tab, featurebase);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION, featurebase);
	script.printAntCallTask(tab, TARGET_GATHER_SOURCES, null, params);
	params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printZipTask(tab, destination.append("${feature}_src_${featureVersion}.zip").toString(), "${feature.base}");
	script.printDeleteTask(tab, featurebase, null, null);
	tab--;
	script.printString(tab, "</target>");
}
protected void generateGatherBinPartsTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printAntCallTask(tab, TARGET_CHILDREN, null, params);
	String inclusions = getBuildProperty(PROPERTY_BIN_INCLUDES);
	if (inclusions == null)
		inclusions = "";
	String exclusions = getBuildProperty(PROPERTY_BIN_EXCLUDES);
	if (exclusions == null)
		exclusions = "";
	params.clear(); // they are properties, not params, but we'll reuse the variable
	params.put("includes", inclusions);
	params.put("excludes", exclusions);
	params.put("srcdir", getPropertyFormat(PROPERTY_BASEDIR));
	params.put("dest", "${feature.base}/install/features/${feature}");
	script.printAntTask(tab, getPropertyFormat(PROPERTY_TEMPLATE), null, "includesExcludesCopy", null, null, params);
	script.printString(--tab, "</target>");
}
protected void generatePropertiesTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_PROPERTIES, null, null, null, null);
	generateMandatoryProperties(tab);
	script.printEndTag(--tab, "target");
}
/**
 * 
 */
protected void generateMandatoryProperties(int tab) {
	script.printProperty(tab, PROPERTY_FEATURE, feature.getFeatureIdentifier());
	script.printProperty(tab, "featureVersion", feature.getFeatureVersion());
	for (Iterator iterator = pluginLocations.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		script.printPluginLocationDeclaration(tab, (String) entry.getKey(), (String) entry.getValue());
	}
}
protected void generateBuildUpdateJarTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printAntCallTask(tab, TARGET_BUILD_JARS, null, null);
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	// be sure to call the gather with children turned off.  The only way to do this is 
	// to clear all inherited values.  Must remember to setup anything that is really expected.
	params.clear();
	params.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, "false", params);
	script.printJarTask(tab, destination.append("${feature}_${featureVersion}.jar").toString(), "${feature.base}");
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * Zip up the whole feature.
 */
protected void generateZipDistributionWholeTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	Map params = new HashMap(1);
	params.put(PROPERTY_INCLUDE_CHILDREN, "true");
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printZipTask(tab, destination.append("${feature}_${featureVersion}.bin.dist.zip").toString(), getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * Executes a given target in all children's script files.
 */
protected void generateAllChildrenTarget() {
	StringBuffer depends = new StringBuffer();
	depends.append(TARGET_INIT);
	depends.append(",");
	depends.append(TARGET_ALL_PLUGINS);
	depends.append(",");
	depends.append(TARGET_ALL_FRAGMENTS);
	
	script.println();
	script.printTargetDeclaration(1, TARGET_ALL_CHILDREN, depends.toString(), null, null, null);
	script.printString(1, "</target>");
}
protected void generateSourceIndividualTarget(String name, String source) throws CoreException {
	String basedir = getPropertyFormat(PROPERTY_BASEDIR);
	IPath destination = new Path(basedir);
	destination = destination.append(name);
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, name, TARGET_INIT, null, null, null);
	tab++;
	Map properties = new HashMap(1);
	properties.put("mapping", source);
	properties.put("includes", source);
	properties.put("excludes", ""); // FIXME: why empty??? should we bother leaving it here??
	properties.put("dest", destination.toString());
	properties.put("srcdir", basedir);
	script.printAntTask(tab, getPropertyFormat(PROPERTY_TEMPLATE), null, TARGET_SRC, null, null, properties);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * Target responsible for delegating target calls to plug-in's build.xml scripts.
 */
protected void generateAllPluginsTarget() throws CoreException {
	int tab = 1;
	List plugins = computeElements(false);
	String[][] sortedPlugins = computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]));
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			PluginModel plugin = getRegistry().getPlugin(sortedPlugins[list][i]);
			String location = getPluginLocationProperty(plugin.getId(), false);
			script.printAntTask(tab, buildScriptName, location, getPropertyFormat(PROPERTY_TARGET), null, null, null);
		}
	}
	script.printString(--tab, "</target>");
}
/**
 * Target responsible for delegating target calls to fragments's build.xml scripts.
 */
protected void generateAllFragmentsTarget() throws CoreException {
	int tab = 1;
	List fragments = computeElements(true);
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_FRAGMENTS, TARGET_INIT, null, null, null);
	for (Iterator iterator = fragments.iterator(); iterator.hasNext();) {
		PluginModel fragment = (PluginModel) iterator.next();
		String location = getPluginLocationProperty(fragment.getId(), true);
		script.printAntTask(tab, buildScriptName, location, getPropertyFormat(PROPERTY_TARGET), null, null, null);
	}
	script.printString(--tab, "</target>");
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
protected void generateEpilogue() {
	script.println();
	script.printString(0, "</project>");
}
/**
 * Defines, the XML declaration, Ant project and init target.
 */
protected void generatePrologue() {
	int tab = 1;
	script.printProjectDeclaration(feature.getFeatureIdentifier(), TARGET_BUILD_JARS, ".");
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, "initTemplate, " + TARGET_PROPERTIES, null, null, null);
	script.printString(--tab, "</target>");
	script.println();
	script.printTargetDeclaration(tab++, "initTemplate", null, null, PROPERTY_TEMPLATE, null);
	script.printString(tab, "<initTemplate/>");
	script.printString(--tab, "</target>");
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
	generator.setPluginPath(getPluginPath());
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
 *
 */
public void setFeatureRootLocation(String location) {
	this.featureRootLocation = location;
}
/**
 * Delegates some target call to all-template only if the property
 * includeChildren is set.
 */
protected void generateChildrenTarget() {
	script.println();
	script.printTargetDeclaration(1, TARGET_CHILDREN, null, PROPERTY_INCLUDE_CHILDREN, null, null);
	script.printAntCallTask(2, TARGET_ALL_CHILDREN, null, null);
	script.printString(1, "</target>");
}
}
