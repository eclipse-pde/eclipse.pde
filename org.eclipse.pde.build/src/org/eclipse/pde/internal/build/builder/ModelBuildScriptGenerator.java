/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.ant.JavacTask;
import org.eclipse.update.core.IPluginEntry;

/**
 * Generic class for generating scripts for plug-ins and fragments.
 */
public class ModelBuildScriptGenerator extends AbstractBuildScriptGenerator {
	public static final String EXPANDED_DOT = "@dot"; //$NON-NLS-1$
	public static final String DOT = "."; //$NON-NLS-1$

	/**
	 * Represents a entry that must be compiled and which is listed in the build.properties file.
	 */
	protected class CompiledEntry {
		static final byte JAR = 0;
		static final byte FOLDER = 1;
		private String name;
		private String resolvedName;
		private String[] source;
		private String[] output;
		private String[] extraClasspath;
		private byte type;

		protected CompiledEntry(String entryName, String[] entrySource, String[] entryOutput, String[] entryExtraClasspath, byte entryType) {
			this.name = entryName;
			this.source = entrySource;
			this.output = entryOutput;
			this.extraClasspath = entryExtraClasspath;
			this.type = entryType;
		}

		protected String getName(boolean resolved) {
			if (!resolved)
				return name;

			if (resolvedName == null)
				resolvedName = replaceVariables(name, true);

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

		public byte getType() {
			return type;
		}
	}

	/**
	 * PluginModel to generate script from.
	 */
	protected BundleDescription model;

	protected String fullName;
	protected String pluginZipDestination;
	protected String pluginUpdateJarDestination;

	private FeatureBuildScriptGenerator featureGenerator;

	/** constants */
	protected final String PLUGIN_DESTINATION = getPropertyFormat(PROPERTY_PLUGIN_DESTINATION);

	private Properties permissionProperties;

	private String propertiesFileName = PROPERTIES_FILE;
	private String buildScriptFileName = DEFAULT_BUILD_SCRIPT_FILENAME;
	//This list is initialized by the generateBuildJarsTarget
	private ArrayList compiledJarNames;
	private boolean dotOnTheClasspath = false;
	private boolean binaryPlugin = false;
	private boolean signJars = false;

	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		//If it is a binary plugin, then we don't generate scripts
		if (binaryPlugin)
			return;
		
		if (model == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Messages.error_missingElement, null));
		}

		// If the the plugin we want to generate is a source plugin, and the feature that required the generation of this plugin is not being asked to build the source
		// we want to leave. This is particularly usefull for the case of the pde.source building (at least for now since the source of pde is not in a feature)
		if (featureGenerator != null && featureGenerator.isSourceFeatureGeneration() == false && featureGenerator.getBuildProperties().containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + model.getSymbolicName()))
			return;

		if (!AbstractScriptGenerator.isBuildingOSGi())
			checkBootAndRuntime();

		initializeVariables();
		if (BundleHelper.getDefault().isDebugging())
			System.out.println("Generating plugin " + model.getSymbolicName()); //$NON-NLS-1$

		String custom = (String) getBuildProperties().get(PROPERTY_CUSTOM);
		if (custom != null && custom.equalsIgnoreCase("true")) { //$NON-NLS-1$
			updateExistingScript();
			return;
		}

		openScript(getLocation(model), buildScriptFileName);
		try {
			generateBuildScript();
		} finally {
			closeScript();
		}
	}

	/**
	 * Check that boot and runtime are available, otherwise throws an exception because the build will fail.
	 */
	private void checkBootAndRuntime() throws CoreException {
		if (getSite(false).getRegistry().getResolvedBundle(PI_BOOT) == null) {
			IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, NLS.bind(Messages.exception_missingPlugin, PI_BOOT), null);
			throw new CoreException(status);
		}
		if (getSite(false).getRegistry().getResolvedBundle(PI_RUNTIME) == null) {
			IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, NLS.bind(Messages.exception_missingPlugin, PI_RUNTIME), null);
			throw new CoreException(status);
		}
	}

	public static String getNormalizedName(BundleDescription bundle) {
		return bundle.getSymbolicName() + '_' + bundle.getVersion();
	}
	
	private void initializeVariables() throws CoreException {
		fullName = getNormalizedName(model);
		pluginZipDestination = PLUGIN_DESTINATION + '/' + fullName + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
		pluginUpdateJarDestination = PLUGIN_DESTINATION + '/' + fullName + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
		String[] classpathInfo = getClasspathEntries(model);
		dotOnTheClasspath = specialDotProcessing(getBuildProperties(), classpathInfo);
	}

	protected static boolean findAndReplaceDot(String[] classpathInfo) {
		for (int i = 0; i < classpathInfo.length; i++) {
			if (DOT.equals(classpathInfo[i])) {
				classpathInfo[i] = EXPANDED_DOT;
				return true;
			}
		}
		return false;
	}

	public static boolean specialDotProcessing(Properties properties, String[] classpathInfo) {
		if (findAndReplaceDot(classpathInfo) || (classpathInfo.length > 0 && classpathInfo[0].equals(EXPANDED_DOT))) {
			String sourceFolder = properties.getProperty(PROPERTY_SOURCE_PREFIX + DOT);
			if (sourceFolder != null) {
				properties.setProperty(PROPERTY_SOURCE_PREFIX + EXPANDED_DOT, sourceFolder);
				properties.remove(PROPERTY_SOURCE_PREFIX + DOT);
			}
			String outputValue = properties.getProperty(PROPERTY_OUTPUT_PREFIX + DOT);
			if (outputValue != null) {
				properties.setProperty(PROPERTY_OUTPUT_PREFIX + EXPANDED_DOT, outputValue);
				properties.remove(PROPERTY_OUTPUT_PREFIX + DOT);
			}
			String buildOrder = properties.getProperty(PROPERTY_JAR_ORDER);
			if (buildOrder != null) {
				String[] order = Utils.getArrayFromString(buildOrder);
				for (int i = 0; i < order.length; i++)
					if (order[i].equals(DOT))
						order[i] = EXPANDED_DOT;
				properties.setProperty(PROPERTY_JAR_ORDER, Utils.getStringFromArray(order, ",")); //$NON-NLS-1$
			}

			String extraEntries = properties.getProperty(PROPERTY_EXTRAPATH_PREFIX + '.');
			if (extraEntries != null) {
				properties.setProperty(PROPERTY_EXTRAPATH_PREFIX + EXPANDED_DOT, extraEntries);
			}

			String includeString = properties.getProperty(PROPERTY_BIN_INCLUDES);
			if (includeString != null) {
				String[] includes = Utils.getArrayFromString(includeString);
				for (int i = 0; i < includes.length; i++)
					if (includes[i].equals(DOT))
						includes[i] = null;
				properties.setProperty(PROPERTY_BIN_INCLUDES, Utils.getStringFromArray(includes, ",")); //$NON-NLS-1$
			}
			return true;
		}
		return false;
	}

	/**
	 * Main call for generating the script.
	 * 
	 * @throws CoreException
	 */
	private void generateBuildScript() throws CoreException {
		generatePrologue();
		generateBuildUpdateJarTarget();

		if (getBuildProperties().getProperty(SOURCE_PLUGIN, null) == null) {
			generateBuildJarsTarget(model);
		} else {
			generateBuildJarsTargetForSourceGathering();
			generateEmptyBuildSourcesTarget();
		}
		generateGatherBinPartsTarget();
		generateBuildZipsTarget();
		generateGatherSourcesTarget();
		generateGatherLogTarget();
		generateCleanTarget();
		generateRefreshTarget();
		generateZipPluginTarget();
		generateEpilogue();
	}

	/**
	 * Method generateEmptyBuildSourceTarget.
	 */
	private void generateEmptyBuildSourcesTarget() {
		script.printTargetDeclaration(TARGET_BUILD_SOURCES, null, null, null, null);
		script.printTargetEnd();
	}

	/**
	 * Method generateBuildJarsTargetForSourceGathering.
	 */
	private void generateBuildJarsTargetForSourceGathering() {
		script.printTargetDeclaration(TARGET_BUILD_JARS, null, null, null, null);
		compiledJarNames = new ArrayList(0);
		IPluginEntry entry = Utils.getPluginEntry(featureGenerator.feature, model.getSymbolicName(), false)[0];
		Config configInfo;
		if (entry.getOS() == null && entry.getWS() == null && entry.getOSArch() == null)
			configInfo = Config.genericConfig();
		else
			configInfo = new Config(entry.getOS(), entry.getWS(), entry.getOSArch());

		Set pluginsToGatherSourceFrom = (Set) featureGenerator.sourceToGather.getElementEntries().get(configInfo);
		if (pluginsToGatherSourceFrom != null) {
			for (Iterator iter = pluginsToGatherSourceFrom.iterator(); iter.hasNext();) {
				BundleDescription plugin = (BundleDescription) iter.next();
				if (plugin.getSymbolicName().equals(model.getSymbolicName())) // We are not trying to gather the source from ourself since we are generated and we know we don't have source...
					continue;

				// The two steps are required, because some plugins (xerces, junit, ...) don't build their source: the source already comes zipped
				IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getLocation(model)));
				script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toOSString(), TARGET_BUILD_SOURCES, null, null, null);
				HashMap params = new HashMap(1);
				params.put(PROPERTY_DESTINATION_TEMP_FOLDER, getPropertyFormat(PROPERTY_BASEDIR) + "/src"); //$NON-NLS-1$
				script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toOSString(), TARGET_GATHER_SOURCES, null, null, params);
			}
		}
		script.printTargetEnd();
	}

	/**
	 * Add the <code>clean</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateCleanTarget() throws CoreException {
		script.println();
		Properties properties = getBuildProperties();
		CompiledEntry[] availableJars = extractEntriesToCompile(properties);
		script.printTargetDeclaration(TARGET_CLEAN, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_clean, model.getSymbolicName()));
		for (int i = 0; i < availableJars.length; i++) {
			String jarName = availableJars[i].getName(true);
			if (availableJars[i].type == CompiledEntry.JAR) {
				script.printDeleteTask(null, getJARLocation(jarName), null);
			} else {
				script.printDeleteTask(getJARLocation(jarName), null, null);
			}
			script.printDeleteTask(null, getSRCLocation(jarName), null);
		}
		script.printDeleteTask(null, pluginUpdateJarDestination, null);
		script.printDeleteTask(null, pluginZipDestination, null);
		script.printDeleteTask(getPropertyFormat(IXMLConstants.PROPERTY_TEMP_FOLDER), null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>gather.logs</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateGatherLogTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_LOGS, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
		IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
		baseDestination = baseDestination.append(fullName);
		List destinations = new ArrayList(5);
		Properties properties = getBuildProperties();
		CompiledEntry[] availableJars = extractEntriesToCompile(properties);
		for (int i = 0; i < availableJars.length; i++) {
			String name = availableJars[i].getName(true);
			IPath destination = baseDestination.append(name).removeLastSegments(1); // remove the jar name
			if (!destinations.contains(destination)) {
				script.printMkdirTask(destination.toString());
				destinations.add(destination);
			}
			script.printCopyTask(getTempJARFolderLocation(name) + ".log", destination.toString(), null, false); //$NON-NLS-1$
		}
		script.printTargetEnd();
	}

	/**
	 * 
	 * @param zipName
	 * @param source
	 */
	private void generateZipIndividualTarget(String zipName, String source) {
		script.println();
		script.printTargetDeclaration(zipName, TARGET_INIT, null, null, null);
		IPath root = new Path(getPropertyFormat(IXMLConstants.PROPERTY_BASEDIR));
		script.printZipTask(root.append(zipName).toString(), root.append(source).toString(), false, false, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>gather.sources</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateGatherSourcesTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
		IPath baseDestination = new Path(getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
		baseDestination = baseDestination.append(fullName);
		List destinations = new ArrayList(5);
		Properties properties = getBuildProperties();
		CompiledEntry[] availableJars = extractEntriesToCompile(properties);
		for (int i = 0; i < availableJars.length; i++) {
			String jar = availableJars[i].getName(true);
			IPath destination = baseDestination.append(jar).removeLastSegments(1); // remove the jar name
			if (!destinations.contains(destination)) {
				script.printMkdirTask(destination.toString());
				destinations.add(destination);
			}
			script.printCopyTask(getSRCLocation(jar), destination.toString(), null, false);
		}
		String include = (String) getBuildProperties().get(PROPERTY_SRC_INCLUDES);
		String exclude = (String) getBuildProperties().get(PROPERTY_SRC_EXCLUDES);
		if (include != null || exclude != null) {
			FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
			script.printCopyTask(null, baseDestination.toString(), new FileSet[] {fileSet}, false);
		}
		script.printTargetEnd();
	}

	private boolean containsStarDotJar(String[] strings) {
		for (int i = 0; i < strings.length; i++) {
			if (strings[i].trim().equalsIgnoreCase("*.jar")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	/**
	 * Add the <code>gather.bin.parts</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateGatherBinPartsTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
		IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
		destination = destination.append(fullName);
		String root = destination.toString();
		script.printMkdirTask(root);
		List destinations = new ArrayList(5);
		destinations.add(destination);
		String include = (String) getBuildProperties().get(PROPERTY_BIN_INCLUDES);
		String exclude = (String) getBuildProperties().get(PROPERTY_BIN_EXCLUDES);

		//Copy only the jars that has been compiled and are listed in the includes
		String[] splitIncludes = Utils.getArrayFromString(include);
		boolean allJars = containsStarDotJar(splitIncludes);
		String[] fileSetValues = new String[compiledJarNames.size()];
		int count = 0;
		for (Iterator iter = compiledJarNames.iterator(); iter.hasNext();) {
			CompiledEntry entry = (CompiledEntry) iter.next();
			String formatedName = entry.getName(false) + (entry.getType() == CompiledEntry.FOLDER ? "/" : ""); //$NON-NLS-1$//$NON-NLS-2$
			if (allJars || Utils.isStringIn(splitIncludes, formatedName)) {
				fileSetValues[count++] = formatedName;
				continue;
			}
		}
		if (count != 0) {
			FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER), null, Utils.getStringFromArray(fileSetValues, ","), null, replaceVariables(exclude, true), null, null); //$NON-NLS-1$
			script.printCopyTask(null, root, new FileSet[] {fileSet}, true);
		}
		if (dotOnTheClasspath && compiledJarNames.size() != 0) {
			FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER) + '/' + EXPANDED_DOT, null, "**", null, null, null, null); //$NON-NLS-1$
			script.printCopyTask(null, root, new FileSet[] {fileSet}, true);
		}
		//General copy of the files listed in the includes
		if (include != null || exclude != null) {
			FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASEDIR), null, replaceVariables(include, true), null, replaceVariables(exclude, true), null, null);
			script.printCopyTask(null, root, new FileSet[] {fileSet}, true);
		}
		generatePermissionProperties(root);
		genarateIdReplacementCall(destination.toString());
		script.printTargetEnd();
	}

	private void genarateIdReplacementCall(String location) throws CoreException {
		String qualifier = getBuildProperties().getProperty(PROPERTY_QUALIFIER);
		if (qualifier == null)
			return;
		script.print("<eclipse.versionReplacer path=\"" + location + "\" version=\"" + model.getVersion() + "\"/>"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	private void generatePermissionProperties(String directory) throws CoreException {
		getPermissionProperties();
		for (Iterator iter = permissionProperties.entrySet().iterator(); iter.hasNext();) {
			Map.Entry permission = (Map.Entry) iter.next();
			String instruction = (String) permission.getKey();
			String parameters = (String) permission.getValue();
			int index;
			if ((index = instruction.indexOf(PERMISSIONS)) != -1) {
				generateChmodInstruction(directory, instruction.substring(index + PERMISSIONS.length() + 1), parameters);
				continue;
			}
			if (instruction.startsWith(LINK)) {
				generateLinkInstruction(directory, parameters);
			}
		}
	}

	private void generateChmodInstruction(String dir, String rights, String files) {
		// TO CHECK We only consider rights specified with numbers
		if (rights.equals(EXECUTABLE)) {
			rights = "755"; //$NON-NLS-1$
		}
		script.printChmod(dir, rights, files);
	}

	private void generateLinkInstruction(String dir, String files) {
		String[] links = Utils.getArrayFromString(files, ","); //$NON-NLS-1$
		List arguments = new ArrayList(2);
		for (int i = 0; i < links.length; i += 2) {
			arguments.add(links[i]);
			arguments.add(links[i + 1]);
			script.printExecTask("ln -s", dir, arguments, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$
			arguments.clear();
		}
	}

	protected Properties getPermissionProperties() throws CoreException {
		if (permissionProperties == null) {
			permissionProperties = readProperties(getLocation(model), PERMISSIONS_FILE, IStatus.INFO);
		}
		return permissionProperties;
	}

	/**
	 * Add the <code>zip.plugin</code> target to the given Ant script.
	 */
	private void generateZipPluginTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_PLUGIN, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_zipPlugin, model.getSymbolicName()));
		script.printDeleteTask(getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		script.printMkdirTask(getPropertyFormat(PROPERTY_TEMP_FOLDER));
		script.printAntCallTask(TARGET_BUILD_JARS, null, null);
		script.printAntCallTask(TARGET_BUILD_SOURCES, null, null);
		Map params = new HashMap(1);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, getPropertyFormat(PROPERTY_TEMP_FOLDER) + '/'); //$NON-NLS-1$
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, null, params);
		script.printAntCallTask(TARGET_GATHER_SOURCES, null, params);
		FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_TEMP_FOLDER), null, "**/*.bin.log", null, null, null, null); //$NON-NLS-1$
		script.printDeleteTask(null, null, new FileSet[] {fileSet});
		script.printZipTask(pluginZipDestination, getPropertyFormat(PROPERTY_TEMP_FOLDER), true, false, null);
		script.printDeleteTask(getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>build.update.jar</code> target to the given Ant script.
	 */
	private void generateBuildUpdateJarTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_buildUpdateJar, model.getSymbolicName()));
		script.printDeleteTask(getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		script.printMkdirTask(getPropertyFormat(PROPERTY_TEMP_FOLDER));
		script.printAntCallTask(TARGET_BUILD_JARS, null, null);
		Map params = new HashMap(1);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, getPropertyFormat(PROPERTY_TEMP_FOLDER) + '/'); //$NON-NLS-1$
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, null, params);
		script.printZipTask(pluginUpdateJarDestination, getPropertyFormat(PROPERTY_TEMP_FOLDER) + '/' + fullName, false, false, null); //$NON-NLS-1$
		script.printDeleteTask(getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		if (signJars)
			script.println("<signjar jar=\"" + pluginUpdateJarDestination + "\" alias=\"" + getPropertyFormat("sign.alias") + "\" keystore=\"" + getPropertyFormat("sign.keystore") + "\" storepass=\"" + getPropertyFormat("sign.storepass") + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ 
		script.printTargetEnd();
	}

	/**
	 * Add the <code>refresh</code> target to the given Ant script.
	 */
	private void generateRefreshTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_REFRESH, TARGET_INIT, PROPERTY_ECLIPSE_RUNNING, null, Messages.build_plugin_refresh);
		script.printConvertPathTask(new Path(getLocation(model)).removeLastSegments(0).toOSString().replace('\\', '/'), PROPERTY_RESOURCE_PATH, false);
		script.printRefreshLocalTask(getPropertyFormat(PROPERTY_RESOURCE_PATH), "infinite"); //$NON-NLS-1$
		script.printTargetEnd();
	}

	/**
	 * End the script by closing the project element.
	 */
	private void generateEpilogue() {
		script.println();
		script.printProjectEnd();
	}

	/**
	 * Defines, the XML declaration, Ant project and targets init and initTemplate.
	 */
	private void generatePrologue() {
		script.printProjectDeclaration(model.getSymbolicName(), TARGET_BUILD_JARS, DOT); //$NON-NLS-1$
		script.println();
		script.printProperty(PROPERTY_BOOTCLASSPATH, ""); //$NON-NLS-1$
		script.printProperty(PROPERTY_BASE_WS, getPropertyFormat(PROPERTY_WS));
		script.printProperty(PROPERTY_BASE_OS, getPropertyFormat(PROPERTY_OS));
		script.printProperty(PROPERTY_BASE_ARCH, getPropertyFormat(PROPERTY_ARCH));
		script.printProperty(PROPERTY_BASE_NL, getPropertyFormat(PROPERTY_NL));

		script.printProperty(PROPERTY_JAVAC_FAIL_ON_ERROR, "false"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_VERBOSE, "true"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_SOURCE, "1.3"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_TARGET, "1.2"); //$NON-NLS-1$  
		script.printProperty(PROPERTY_JAVAC_COMPILERARG, ""); //$NON-NLS-1$  

		script.println();
		script.printTargetDeclaration(TARGET_INIT, TARGET_PROPERTIES, null, null, null);

		script.println("<condition property=\"" + PROPERTY_PLUGIN_TEMP + "\" value=\"" + getPropertyFormat(PROPERTY_BUILD_TEMP) + '/' + DEFAULT_PLUGIN_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<isset property=\"" + PROPERTY_BUILD_TEMP + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("</condition>"); //$NON-NLS-1$

		script.printProperty(PROPERTY_PLUGIN_TEMP, getPropertyFormat(PROPERTY_BASEDIR));

		script.println("<condition property=\"" + PROPERTY_BUILD_RESULT_FOLDER + "\" value=\"" + getPropertyFormat(PROPERTY_PLUGIN_TEMP) + '/' + new Path(model.getLocation()).lastSegment() + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<isset property=\"" + PROPERTY_BUILD_TEMP + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_BUILD_RESULT_FOLDER, getPropertyFormat(PROPERTY_BASEDIR));

		script.printProperty(PROPERTY_TEMP_FOLDER, getPropertyFormat(PROPERTY_BASEDIR) + '/' + PROPERTY_TEMP_FOLDER);
		script.printProperty(PROPERTY_PLUGIN_DESTINATION, getPropertyFormat(PROPERTY_BASEDIR));
		script.printTargetEnd();
		script.println();
		script.printTargetDeclaration(TARGET_PROPERTIES, null, PROPERTY_ECLIPSE_RUNNING, null, null);
		script.printProperty(PROPERTY_BUILD_COMPILER, JDT_COMPILER_ADAPTER);
		script.printTargetEnd();
	}

	/**
	 * Sets the PluginModel to generate script from.
	 * 
	 * @param model
	 * @throws CoreException
	 */
	public void setModel(BundleDescription model) throws CoreException {
		if (model == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Messages.error_missingElement, null));
		}
		this.model = model;
		if (getBuildProperties() == AbstractScriptGenerator.MissingProperties.getInstance()) {
			//if there were no build.properties, then it is a binary plugin
			binaryPlugin = true;
		} else {
			getCompiledElements().add(model.getSymbolicName());
		}
		Properties bundleProperties = (Properties) model.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			model.setUserObject(bundleProperties);
		}
		bundleProperties.put("isCompiled", binaryPlugin ? Boolean.FALSE : Boolean.TRUE);
	}

	/**
	 * Sets model to generate scripts from.
	 * 
	 * @param modelId
	 * @throws CoreException
	 */
	public void setModelId(String modelId) throws CoreException {
		BundleDescription newModel = getModel(modelId);
		if (newModel == null) {
			String message = NLS.bind(Messages.exception_missingElement, modelId);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, message, null));
		}
		setModel(newModel);
	}

	/**
	 * Add the <code>build.zips</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateBuildZipsTarget() throws CoreException {
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
		script.printTargetDeclaration(TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
		script.printTargetEnd();
	}

	/**
	 * Sets the featureGenerator.
	 * @param featureGenerator The featureGenerator to set
	 */
	public void setFeatureGenerator(FeatureBuildScriptGenerator featureGenerator) {
		this.featureGenerator = featureGenerator;
	}

	/**
	 * Add the "build.jars" target to the given Ant script using the specified plug-in model.
	 * 
	 * @param pluginModel the plug-in model to reference
	 * @throws CoreException
	 */
	private void generateBuildJarsTarget(BundleDescription pluginModel) throws CoreException {
		Properties properties = getBuildProperties();
		CompiledEntry[] availableJars = extractEntriesToCompile(properties);
		compiledJarNames = new ArrayList(availableJars.length);
		Map jars = new HashMap(availableJars.length);
		for (int i = 0; i < availableJars.length; i++)
			jars.put(availableJars[i].getName(false), availableJars[i]);

		// Put the jars in a correct compile order
		String jarOrder = (String) getBuildProperties().get(PROPERTY_JAR_ORDER);
		IClasspathComputer classpath;
		if (AbstractScriptGenerator.isBuildingOSGi())
			classpath = new ClasspathComputer3_0(this);
		else
			classpath = new ClasspathComputer2_1(this);

		if (jarOrder != null) {
			String[] order = Utils.getArrayFromString(jarOrder);
			for (int i = 0; i < order.length; i++) {
				CompiledEntry jar = (CompiledEntry) jars.get(order[i]);
				if (jar == null)
					continue;

				compiledJarNames.add(jar);
				generateCompilationTarget(classpath.getClasspath(pluginModel, jar), jar);
				generateSRCTarget(jar);
				jars.remove(order[i]);
			}
		}
		for (Iterator iterator = jars.values().iterator(); iterator.hasNext();) {
			CompiledEntry jar = (CompiledEntry) iterator.next();
			compiledJarNames.add(jar);
			generateCompilationTarget(classpath.getClasspath(pluginModel, jar), jar);
			generateSRCTarget(jar);
		}
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_JARS, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_buildJars, pluginModel.getSymbolicName()));
		for (Iterator iter = compiledJarNames.iterator(); iter.hasNext();) {
			String name = ((CompiledEntry) iter.next()).getName(false);
			script.printAvailableTask(name, replaceVariables(getJARLocation(name), true));
			script.printAntCallTask(name, null, null);
		}
		script.printTargetEnd();

		script.println();
		script.printTargetDeclaration(TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
		for (Iterator iter = compiledJarNames.iterator(); iter.hasNext();) {
			String jarName = ((CompiledEntry) iter.next()).getName(false);
			String srcName = getSRCName(jarName);
			script.printAvailableTask(srcName, getSRCLocation(jarName));
			script.printAntCallTask(srcName, null, null);
		}
		script.printTargetEnd();
	}

	/**
	 * Add the "jar" target to the given Ant script using the given classpath and
	 * jar as parameters.
	 * 
	 * @param classpath the classpath for the jar command
	 * @param entry
	 */
	private void generateCompilationTarget(List classpath, CompiledEntry entry) {
		script.println();
		String name = entry.getName(false);
		script.printTargetDeclaration(name, TARGET_INIT, null, entry.getName(true), NLS.bind(Messages.build_plugin_jar, name));
		String destdir = getTempJARFolderLocation(entry.getName(true));
		script.printDeleteTask(destdir, null, null);
		script.printMkdirTask(destdir);
		script.printComment("compile the source code"); //$NON-NLS-1$
		JavacTask javac = new JavacTask();
		javac.setClasspath(classpath);
		javac.setBootClasspath(getPropertyFormat(PROPERTY_BOOTCLASSPATH));
		javac.setDestdir(destdir);
		javac.setFailOnError(getPropertyFormat(PROPERTY_JAVAC_FAIL_ON_ERROR));
		javac.setDebug(getPropertyFormat(PROPERTY_JAVAC_DEBUG_INFO));
		javac.setVerbose(getPropertyFormat(PROPERTY_JAVAC_VERBOSE));
		javac.setIncludeAntRuntime("no"); //$NON-NLS-1$
		javac.setSource(getPropertyFormat(PROPERTY_JAVAC_SOURCE));
		javac.setTarget(getPropertyFormat(PROPERTY_JAVAC_TARGET));
		javac.setCompileArgs(getPropertyFormat(PROPERTY_JAVAC_COMPILERARG));
		String[] sources = entry.getSource();
		javac.setSrcdir(sources);
		script.print(javac);
		script.printComment("Copy necessary resources"); //$NON-NLS-1$
		FileSet[] fileSets = new FileSet[sources.length];
		for (int i = 0; i < sources.length; i++)
			fileSets[i] = new FileSet(sources[i], null, null, null, "**/*.java, **/package.htm*", null, null); //$NON-NLS-1$
		script.printCopyTask(null, destdir, fileSets, true);

		String jarLocation = getJARLocation(entry.getName(true));
		script.printMkdirTask(new Path(jarLocation).removeLastSegments(1).toString());

		if (entry.getType() == CompiledEntry.FOLDER) {
			FileSet[] binFolder = new FileSet[] {new FileSet(destdir, null, null, null, null, null, null)};
			script.printCopyTask(null, jarLocation, binFolder, true);
		} else {
			script.printJarTask(jarLocation, destdir, getEmbeddedManifestFile(entry, destdir));
		}
		script.printDeleteTask(destdir, null, null);
		script.printTargetEnd();
	}

	private String getEmbeddedManifestFile(CompiledEntry jarEntry, String destdir) {
		try {
			String manifestName = getBuildProperties().getProperty(PROPERTY_MANIFEST_PREFIX + jarEntry.getName(true));
			if (manifestName == null)
				return null;
			return destdir + '/' + manifestName;
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * 
	 * @param properties
	 * @return JAR[]
	 */
	protected CompiledEntry[] extractEntriesToCompile(Properties properties) {
		List result = new ArrayList(5);
		int prefixLength = PROPERTY_SOURCE_PREFIX.length();
		for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			if (!(key.startsWith(PROPERTY_SOURCE_PREFIX)))
				continue;
			key = key.substring(prefixLength);
			String[] source = Utils.getArrayFromString((String) entry.getValue());
			String[] output = Utils.getArrayFromString(properties.getProperty(PROPERTY_OUTPUT_PREFIX + key));
			String[] extraClasspath = Utils.getArrayFromString(properties.getProperty(PROPERTY_EXTRAPATH_PREFIX + key));
			CompiledEntry newEntry = new CompiledEntry(key, source, output, extraClasspath, key.endsWith(PROPERTY_JAR_SUFFIX) ? CompiledEntry.JAR : CompiledEntry.FOLDER);
			result.add(newEntry);
		}
		return (CompiledEntry[]) result.toArray(new CompiledEntry[result.size()]);
	}

	/**
	 * Add the "src" target to the given Ant script.
	 * 
	 * @param jar
	 */
	private void generateSRCTarget(CompiledEntry jar) {
		script.println();
		String name = jar.getName(false);
		String srcName = getSRCName(name);
		script.printTargetDeclaration(srcName, TARGET_INIT, null, srcName, null);
		String[] sources = jar.getSource();
		filterNonExistingSourceFolders(sources);
		FileSet[] fileSets = new FileSet[sources.length];
		int count = 0;
		for (int i = 0; i < sources.length; i++) {
			if (sources[i] != null)
				fileSets[count++] = new FileSet(sources[i], null, "**/*.java", null, null, null, null); //$NON-NLS-1$
		}

		String srcLocation = getSRCLocation(name);
		script.printMkdirTask(new Path(srcLocation).removeLastSegments(1).toString());

		if (count != 0)
			script.printZipTask(srcLocation, null, false, false, fileSets);

		script.printTargetEnd();
	}

	private void filterNonExistingSourceFolders(String[] sources) {
		File pluginRoot;
		pluginRoot = new File(getLocation(model));
		for (int i = 0; i < sources.length; i++) {
			File file = new File(pluginRoot, sources[i]);
			if (!file.exists()) {
				sources[i] = null;
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, EXCEPTION_SOURCE_LOCATION_MISSING, NLS.bind(Messages.warning_cannotLocateSource, file.getAbsolutePath()), null);
				BundleHelper.getDefault().getLog().log(status);
			}
		}
	}

	/**
	 * Return the name of the zip file for the source for the jar with
	 * the given name.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getSRCLocation(String jarName) {
		return getJARLocation(getSRCName(jarName));
	}

	/**
	 * Return the location for a temporary file for the jar file with
	 * the given name.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getTempJARFolderLocation(String jarName) {
		IPath destination = new Path(getPropertyFormat(PROPERTY_TEMP_FOLDER));
		destination = destination.append(jarName + ".bin"); //$NON-NLS-1$
		return destination.toString();
	}

	/**
	 * Return the full location of the jar file.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getJARLocation(String jarName) {
		return new Path(getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER)).append(jarName).toString();
	}

	protected String[] getClasspathEntries(BundleDescription lookedUpModel) throws CoreException {
		return (String[]) getSite(false).getRegistry().getExtraData().get(new Long(lookedUpModel.getBundleId()));
	}

	protected Properties getBuildProperties() throws CoreException {
		if (buildProperties == null)
			return buildProperties = readProperties(model.getLocation(), propertiesFileName, isIgnoreMissingPropertiesFile() ? IStatus.OK : IStatus.WARNING);

		return buildProperties;
	}

	/**
	 * Return the name of the zip file for the source from the given jar name.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getSRCName(String jarName) {
		if (jarName.endsWith(".jar")) { //$NON-NLS-1$
			return jarName.substring(0, jarName.length() - 4) + "src.zip"; //$NON-NLS-1$
		}
		if (jarName.equals(EXPANDED_DOT))
			return "src.zip";
		return jarName.replace('/', '.') + "src.zip"; //$NON-NLS-1$
	}

	/**
	 * If the model defines its own custom script, we do not generate a new one
	 * but we do try to update the version number.
	 */
	private void updateExistingScript() throws CoreException {
		String root = getLocation(model);
		File buildFile = new File(root, buildScriptFileName);
		try {
			updateVersion(buildFile, PROPERTY_VERSION_SUFFIX, model.getVersion().toString());
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writeScript, buildFile);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, e));
		}
		return;
	}

	/**
	 * Substitute the value of an element description variable (variables that
	 * are found in files like plugin.xml, e.g. $ws$) by an Ant property.
	 * 
	 * @param sourceString
	 * @return String
	 */
	protected String replaceVariables(String sourceString, boolean compiledElement) {
		if (sourceString == null)
			return null;

		int i = -1;
		String result = sourceString;
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_WS)) >= 0)
			result = result.substring(0, i) + "ws/" + getPropertyFormat(compiledElement ? PROPERTY_WS : PROPERTY_BASE_WS) + result.substring(i + DESCRIPTION_VARIABLE_WS.length()); //$NON-NLS-1$
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_OS)) >= 0)
			result = result.substring(0, i) + "os/" + getPropertyFormat(compiledElement ? PROPERTY_OS : PROPERTY_BASE_OS) + result.substring(i + DESCRIPTION_VARIABLE_OS.length()); //$NON-NLS-1$
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_ARCH)) >= 0)
			result = result.substring(0, i) + "arch/" + getPropertyFormat(compiledElement ? PROPERTY_ARCH : PROPERTY_BASE_ARCH) + result.substring(i + DESCRIPTION_VARIABLE_OS.length()); //$NON-NLS-1$		
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_NL)) >= 0)
			result = result.substring(0, i) + "nl/" + getPropertyFormat(compiledElement ? PROPERTY_NL : PROPERTY_BASE_NL) + result.substring(i + DESCRIPTION_VARIABLE_NL.length()); //$NON-NLS-1$
		return result;
	}

	public BundleDescription getModel() {
		return model;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(String propertyFileName) {
		this.propertiesFileName = propertyFileName;
	}

	public String getBuildScriptFileName() {
		return buildScriptFileName;
	}

	public void setBuildScriptFileName(String buildScriptFileName) {
		this.buildScriptFileName = buildScriptFileName;
	}

	/**
	 * Sets whether or not to sign any constructed jars.
	 * 
	 * @param value whether or not to sign any constructed JARs
	 */
	public void setSignJars(boolean value) {
		signJars = value;
	}

	/**
	 * Returns the model object which is associated with the given identifier.
	 * Returns <code>null</code> if the model object cannot be found.
	 * 
	 * @param modelId the identifier of the model object to lookup
	 * @return the model object or <code>null</code>
	 */
	protected BundleDescription getModel(String modelId) throws CoreException {
		return getSite(false).getRegistry().getResolvedBundle(modelId);
	}
}
