/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
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
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;
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

	protected static final String FEATURE_DESTINATION = getPropertyFormat(PROPERTY_FEATURE_DESTINATION);
	protected static final String FEATURE_FULL_NAME = getPropertyFormat(PROPERTY_FEATURE_FULL_NAME);
	protected static final String FEATURE_FOLDER_NAME = "features/" + FEATURE_FULL_NAME; //$NON-NLS-1$
	protected static final String FEATURE_TEMP_FOLDER = getPropertyFormat(PROPERTY_FEATURE_TEMP_FOLDER);
	protected static final String SOURCE_FEATURE_FULL_NAME = getPropertyFormat(PROPERTY_FEATURE) + ".source" + getPropertyFormat(PROPERTY_FEATURE_VERSION_SUFFIX); //$NON-NLS-1$

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
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", entry.getVersionedIdentifier().toString()), null)); //$NON-NLS-1$
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
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null)); //$NON-NLS-1$
	if (installLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_INSTALL_LOCATION_MISSING, Policy.bind("error.missingInstallLocation"), null)); //$NON-NLS-1$

	readFeature();
	try {
		// if the feature defines its own custom script, we do not generate a new one
		// but we do try to update the version number
		String custom = (String) getBuildProperties(feature).get(PROPERTY_CUSTOM);
		if (custom != null && custom.equalsIgnoreCase("true")) { //$NON-NLS-1$
			File buildFile = new File(getFeatureRootLocation(), buildScriptName);
			updateVersion(buildFile, PROPERTY_FEATURE_VERSION_SUFFIX, feature.getFeatureVersion());
			return;
		}
	
		if (generateChildrenScript)
			generateChildrenScripts();

		File root = new File(getFeatureRootLocation());
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
 */
protected void generateBuildScript(AntScript script) throws CoreException {
	generatePrologue(script);
	generateAllPluginsTarget(script);
	generateAllFragmentsTarget(script);
	generateAllChildrenTarget(script);
	generateChildrenTarget(script);
	generateBuildJarsTarget(script);
	generateBuildZipsTarget(script);
	generateBuildUpdateJarTarget(script);
	generateGatherBinPartsTarget(script);
	generateZipDistributionWholeTarget(script);
	generateZipSourcesTarget(script);
	generateZipLogsTarget(script);
	generateCleanTarget(script);
	generateRefreshTarget(script);
	generateEpilogue(script);
}

protected void generateBuildZipsTarget(AntScript script) throws CoreException {
	StringBuffer zips = new StringBuffer();
	Properties props = getBuildProperties(feature);
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
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_ZIPS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
protected void generateZipIndividualTarget(AntScript script, String zipName, String source) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, zipName, TARGET_INIT, null, null, null);
	script.printZipTask(tab, FEATURE_DESTINATION + "/" + zipName, BASEDIR + "/" + source, false, null); //$NON-NLS-1$ //$NON-NLS-2$
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
protected void generateCleanTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	IPath basedir = new Path(FEATURE_DESTINATION);
	script.printTargetDeclaration(tab++, TARGET_CLEAN, TARGET_INIT, null, null, null);
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".jar").toString(), null); //$NON-NLS-1$
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".bin.dist.zip").toString(), null); //$NON-NLS-1$
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".log.zip").toString(), null); //$NON-NLS-1$
	script.printDeleteTask(tab, null, basedir.append(FEATURE_FULL_NAME + ".src.zip").toString(), null); //$NON-NLS-1$
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_CLEAN);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
protected void generateZipLogsTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_LOGS, TARGET_INIT, null, null, null);
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printMkdirTask(tab, FEATURE_TEMP_FOLDER);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_LOGS);
	params.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(FEATURE_TEMP_FOLDER).append("plugins").toString()); //$NON-NLS-1$
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, "false", params); //$NON-NLS-1$
	IPath destination = new Path(FEATURE_DESTINATION).append(FEATURE_FULL_NAME + ".log.zip"); //$NON-NLS-1$
	script.printZipTask(tab, destination.toString(), FEATURE_TEMP_FOLDER, true, null);
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}

protected void generateZipSourcesTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printMkdirTask(tab, FEATURE_TEMP_FOLDER);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
	params.put(PROPERTY_DESTINATION_TEMP_FOLDER, FEATURE_TEMP_FOLDER + "/" + "plugins" + "/" + SOURCE_FEATURE_FULL_NAME + "/" + "src"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printZipTask(tab, FEATURE_DESTINATION + "/" + FEATURE_FULL_NAME + ".src.zip", FEATURE_TEMP_FOLDER, true, null); //$NON-NLS-1$ //$NON-NLS-2$
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
protected void generateGatherBinPartsTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
	params.put(PROPERTY_DESTINATION_TEMP_FOLDER, new Path(getPropertyFormat(PROPERTY_FEATURE_BASE)).append("plugins").toString()); //$NON-NLS-1$
	script.printAntCallTask(tab, TARGET_CHILDREN, null, params);
	String include = (String) getBuildProperties(feature).get(PROPERTY_BIN_INCLUDES);
	String exclude = (String) getBuildProperties(feature).get(PROPERTY_BIN_EXCLUDES);
	String root = "${feature.base}/" + FEATURE_FOLDER_NAME; //$NON-NLS-1$
	script.printMkdirTask(tab, root);
	if (include != null || exclude != null) {
		FileSet fileSet = new FileSet(BASEDIR, null, include, null, exclude, null, null);
		script.printCopyTask(tab, null, root, new FileSet[]{ fileSet });
	}	
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
protected void generateBuildUpdateJarTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printProperty(tab, PROPERTY_FEATURE_BASE, FEATURE_TEMP_FOLDER);
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printMkdirTask(tab, FEATURE_TEMP_FOLDER);
	params.clear();
	params.put(PROPERTY_FEATURE_BASE, FEATURE_TEMP_FOLDER);
	// Be sure to call the gather with children turned off.  The only way to do this is 
	// to clear all inherited values.  Must remember to setup anything that is really expected.
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, "false", params); //$NON-NLS-1$
	script.printJarTask(tab, FEATURE_DESTINATION + "/" + FEATURE_FULL_NAME + ".jar", FEATURE_TEMP_FOLDER + "/" + FEATURE_FOLDER_NAME); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
/**
 * Zip up the whole feature.
 */
protected void generateZipDistributionWholeTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, null);
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printMkdirTask(tab, FEATURE_TEMP_FOLDER);
	Map params = new HashMap(1);
	params.put(PROPERTY_FEATURE_BASE, FEATURE_TEMP_FOLDER);
	params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printZipTask(tab, FEATURE_DESTINATION + "/" + FEATURE_FULL_NAME + ".bin.dist.zip", FEATURE_TEMP_FOLDER, false, null); //$NON-NLS-1$ //$NON-NLS-2$
	script.printDeleteTask(tab, FEATURE_TEMP_FOLDER, null, null);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
/**
 * Executes a given target in all children's script files.
 */
protected void generateAllChildrenTarget(AntScript script) {
	StringBuffer depends = new StringBuffer();
	depends.append(TARGET_INIT);
	depends.append(","); //$NON-NLS-1$
	depends.append(TARGET_ALL_PLUGINS);
	depends.append(","); //$NON-NLS-1$
	depends.append(TARGET_ALL_FRAGMENTS);
	
	script.println();
	script.printTargetDeclaration(1, TARGET_ALL_CHILDREN, depends.toString(), null, null, null);
	script.printString(1, "</target>"); //$NON-NLS-1$
}
/**
 * Target responsible for delegating target calls to plug-in's build.xml scripts.
 */
protected void generateAllPluginsTarget(AntScript script) throws CoreException {
	int tab = 1;
	List plugins = computeElements(false);
	String[][] sortedPlugins = Utils.computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]));
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			PluginModel plugin = getRegistry().getPlugin(sortedPlugins[list][i]);
			IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getFeatureRootLocation()));
			script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, null);
		}
	}
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
/**
 * Target responsible for delegating target calls to fragments's build.xml scripts.
 */
protected void generateAllFragmentsTarget(AntScript script) throws CoreException {
	int tab = 1;
	List fragments = computeElements(true);
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_FRAGMENTS, TARGET_INIT, null, null, null);
	for (Iterator iterator = fragments.iterator(); iterator.hasNext();) {
		PluginModel fragment = (PluginModel) iterator.next();
		IPath location = Utils.makeRelative(new Path(getLocation(fragment)), new Path(getFeatureRootLocation()));
		script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, null);
	}
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}





/**
 * Just ends the script.
 */
protected void generateEpilogue(AntScript script) {
	script.println();
	script.printString(0, "</project>"); //$NON-NLS-1$
}
/**
 * Defines, the XML declaration, Ant project and init target.
 */
protected void generatePrologue(AntScript script) {
	int tab = 1;
	script.printProjectDeclaration(feature.getFeatureIdentifier(), TARGET_BUILD_UPDATE_JAR, "."); //$NON-NLS-1$
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, null, null, null, null);
	script.printProperty(tab, PROPERTY_FEATURE, feature.getFeatureIdentifier());
	script.printProperty(tab, PROPERTY_FEATURE_VERSION_SUFFIX, "_" + feature.getFeatureVersion()); //$NON-NLS-1$
	script.printProperty(tab, PROPERTY_FEATURE_FULL_NAME, getPropertyFormat(PROPERTY_FEATURE) + getPropertyFormat(PROPERTY_FEATURE_VERSION_SUFFIX));
	script.printProperty(tab, PROPERTY_FEATURE_TEMP_FOLDER, BASEDIR + "/" + PROPERTY_FEATURE_TEMP_FOLDER); //$NON-NLS-1$
	script.printProperty(tab, PROPERTY_FEATURE_DESTINATION, BASEDIR);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
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
		generator.generate();
	}
}
public void setFeature(String featureID) throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null)); //$NON-NLS-1$
	this.featureID = featureID;
}
/**
 * Reads the target feature from the specified location.
 */
protected void readFeature() throws CoreException {
	String location = getFeatureRootLocation();
	if (location == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureLocation"), null)); //$NON-NLS-1$
	
	FeatureExecutableFactory factory = new FeatureExecutableFactory();
	File file = new File(location);
	try {
		feature = (Feature) factory.createFeature(file.toURL(), null);
		if (feature == null)
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), null));	 //$NON-NLS-1$
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), e)); //$NON-NLS-1$
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

protected Properties getBuildProperties(Feature feature) throws CoreException {
	VersionedIdentifier identifier = feature.getVersionedIdentifier();
	Properties result = (Properties) buildProperties.get(identifier);
	if (result == null) {
		result = readBuildProperties(getFeatureRootLocation());
		buildProperties.put(identifier, result);
	}
	return result;
}
/**
 * Delegates some target call to all-template only if the property
 * includeChildren is set.
 */
protected void generateChildrenTarget(AntScript script) {
	script.println();
	script.printTargetDeclaration(1, TARGET_CHILDREN, null, PROPERTY_INCLUDE_CHILDREN, null, null);
	script.printAntCallTask(2, TARGET_ALL_CHILDREN, null, null);
	script.printString(1, "</target>"); //$NON-NLS-1$
}

protected void generateBuildJarsTarget(AntScript script) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_JARS, TARGET_INIT, null, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_JARS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printEndTag(--tab, "target"); //$NON-NLS-1$
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
	params.clear();
	params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printEndTag(--tab, "target"); //$NON-NLS-1$
}

protected void generateRefreshTarget(AntScript script) {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_REFRESH, TARGET_INIT, PROPERTY_ECLIPSE_RUNNING, null, null);
	script.printRefreshLocalTask(tab, getPropertyFormat(PROPERTY_FEATURE), "infinite"); //$NON-NLS-1$
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_REFRESH);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printString(--tab, "</target>"); //$NON-NLS-1$
}
}