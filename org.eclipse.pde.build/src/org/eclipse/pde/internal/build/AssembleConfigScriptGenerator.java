/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     G&H Softwareentwicklung GmbH - internationalization implementation (bug 150933)
 *     Prosyst - create proper OSGi bundles (bug 174157)
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.PluginEntry;

/**
 * Generate an assemble script for a given feature and a given config. It
 * generates all the instruction to zip the listed plugins and features.
 */
public class AssembleConfigScriptGenerator extends AbstractScriptGenerator {
	protected String directory; // representing the directory where to generate the file
	protected String featureId;
	protected Config configInfo;
	protected IFeature[] features; // the features that will be assembled
	protected IFeature[] allFeatures; //the set of all the features that have been considered
	protected BundleDescription[] plugins;
	protected String filename;
	protected Collection rootFileProviders;
	protected Properties pluginsPostProcessingSteps;
	protected Properties featuresPostProcessingSteps;
	protected ArrayList addedByPermissions = new ArrayList();	//contains the list of files and folders that have been added to an archive by permission management
	
	private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$
	private static final String PROPERTY_ELEMENT_NAME = "elementName"; //$NON-NLS-1$

	private static final String UPDATEJAR = "updateJar"; //$NON-NLS-1$
	private static final String FLAT = "flat"; //$NON-NLS-1$

	private static final byte BUNDLE = 0;
	private static final byte FEATURE = 1;

	private static final String FOLDER = "folder"; //$NON-NLS-1$
	private static final String FILE = "file"; //$NON-NLS-1$
	protected String PROPERTY_ECLIPSE_PLUGINS = "eclipse.plugins"; //$NON-NLS-1$
	protected String PROPERTY_ECLIPSE_FEATURES = "eclipse.features"; //$NON-NLS-1$
	private boolean signJars;
	private boolean generateJnlp;

	private String archiveFormat;
	private boolean groupConfigs = false;
	private String product;
	private ProductFile productFile = null;

	public AssembleConfigScriptGenerator() {
		super();
	}

	public void initialize(String directoryName, String feature, Config configurationInformation, Collection elementList, Collection featureList, Collection allFeaturesList, Collection rootProviders) throws CoreException {
		this.directory = directoryName;
		this.featureId = feature;
		this.configInfo = configurationInformation;
		this.rootFileProviders = rootProviders != null ? rootProviders : new ArrayList(0);

		this.features = new IFeature[featureList.size()];
		featureList.toArray(this.features);

		this.allFeatures = new IFeature[allFeaturesList.size()];
		allFeaturesList.toArray(this.allFeatures);

		this.plugins = new BundleDescription[elementList.size()];
		this.plugins = (BundleDescription[]) elementList.toArray(this.plugins);

		openScript(directoryName, getTargetName() + ".xml"); //$NON-NLS-1$
		loadPostProcessingSteps();
	}

	private void loadProduct() {
		if (product == null || product.startsWith("${")) { //$NON-NLS-1$
			productFile = null;
			return;
		}
		String productPath = findFile(product, false);
		if (productPath == null)
			productPath = product;
		File f = new File(productPath);
		if (f.exists() && f.isFile()) {
			try {
				productFile = new ProductFile(productPath, configInfo.getOs());
			} catch (CoreException e) {
				// TODO log
			}
		} else {
			//TODO log
		}
	}

	private String computeIconsList() {
		String result = Utils.getPropertyFormat(PROPERTY_LAUNCHER_ICONS);
		if (productFile == null)
			return result;
		String[] icons = productFile.getIcons();
		for (int i = 0; i < icons.length; i++) {
			String location = findFile(icons[i], true);
			if (location != null)
				result +=  ", " + Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + location; //$NON-NLS-1$
			else {
				result += ", " + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + icons[i];  //$NON-NLS-1$
				result += ", " + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + DEFAULT_FEATURE_LOCATION + '/' + icons[i];  //$NON-NLS-1$
			}
		}
		return result;
	}

	private void loadPostProcessingSteps() {
		try {
			pluginsPostProcessingSteps = readProperties(AbstractScriptGenerator.getWorkingDirectory(), DEFAULT_PLUGINS_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR, IStatus.INFO);
			featuresPostProcessingSteps = readProperties(AbstractScriptGenerator.getWorkingDirectory(), DEFAULT_FEATURES_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR, IStatus.INFO);
		} catch (CoreException e) {
			//Ignore
		}
	}

	public void generate() {
		loadProduct();
		generatePrologue();
		generateInitializationSteps();
		generateGatherBinPartsCalls();
		if (embeddedSource)
			generateGatherSourceCalls();
		generatePostProcessingSteps();
		generateBrandingCalls();
		generateArchivingSteps();
		generateEpilogue();
	}

	/**
	 * 
	 */
	private void generateBrandingCalls() {
		String install = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER); //$NON-NLS-1$
		script.printBrandTask(install, computeIconsList(), Utils.getPropertyFormat(PROPERTY_LAUNCHER_NAME), Utils.getPropertyFormat(PROPERTY_OS));
	}

	private void generateArchivingSteps() {
		if (FORMAT_FOLDER.equalsIgnoreCase(archiveFormat)) {
			generateMoveRootFiles();
			return;
		}

		if (FORMAT_ZIP.equalsIgnoreCase(archiveFormat)) {
			generateZipTarget();
			return;
		}

		if (FORMAT_ANTZIP.equalsIgnoreCase(archiveFormat)) {
			generateAntZipTarget();
			return;
		}

		if (FORMAT_ANTTAR.equalsIgnoreCase(archiveFormat)) {
			generateAntTarTarget();
			return;
		}

		if (FORMAT_TAR.equalsIgnoreCase(archiveFormat)) {
			generateTarGZTasks(true);
			return;
		}
	}

	private void generateMoveRootFiles() {
		if (rootFileProviders.size() == 0)
			return;
		
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			Properties featureProperties = null;
			try {
				featureProperties = AbstractScriptGenerator.readProperties(new Path(((IFeature) iter.next()).getURL().getFile()).removeLastSegments(1).toOSString(), PROPERTIES_FILE, IStatus.OK);
				Utils.generatePermissions(featureProperties, configInfo, PROPERTY_ECLIPSE_BASE, script);
			} catch (CoreException e) {
				//do nothing
			}
		}
		
		if (Platform.getOS().equals("win32")) { //$NON-NLS-1$
			FileSet[] rootFiles = new FileSet[1];
			rootFiles[0] = new FileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER), null, "**/**", null, null, null, null); //$NON-NLS-1$//$NON-NLS-2$	
			script.printMoveTask(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), rootFiles, false);
			script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING), null, null); //$NON-NLS-1$
		} else {
			List params = new ArrayList(3);
			params.add("-R"); //$NON-NLS-1$
			params.add("."); //$NON-NLS-1$
			params.add('\'' + Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '\'');
			String rootFileFolder = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
			script.printExecTask("cp", rootFileFolder + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER),  params, null); //$NON-NLS-1$
			script.printDeleteTask(rootFileFolder, null, null);
		}
	}

	protected void generateGatherSourceCalls() {
		Map properties = new HashMap(1);
		properties.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS));
		
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			String placeToGather = getLocation(plugin);
			
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_SOURCES, null, null, properties);
			
			Properties bundleProperties = (Properties) plugin.getUserObject();
			//Source code for plugins with . on the classpath must be put in a folder in the final jar.
			if (bundleProperties.get(WITH_DOT) == Boolean.TRUE) {		
				String targetLocation = Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) + '/' +ModelBuildScriptGenerator.getNormalizedName(plugin);
				String targetLocationSrc = targetLocation +  "/src"; //$NON-NLS-1$
				
				//Find the source zip where it has been gathered and extract it in a folder  
				script.println("<unzip dest=\"" +   script.getEscaped(targetLocationSrc) + "\">");  //$NON-NLS-1$//$NON-NLS-2$
				script.println("\t<fileset dir=\"" +  script.getEscaped(targetLocation)  + "\" includes=\"**/*src.zip\" casesensitive=\"false\"/>");  //$NON-NLS-1$//$NON-NLS-2$
				script.println("</unzip>"); //$NON-NLS-1$
				
				//	Delete the source zip where it has been gathered since we extracted it
				script.printDeleteTask(null, null, new FileSet[] {new FileSet(targetLocation, null, "**/*src.zip", null, null, null, "false")});  //$NON-NLS-1$ //$NON-NLS-2$//$NON-bNLS-3$
			} 
		}

		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_SOURCES, null, null, properties);
		}
	}

	protected void generatePackagingTargets() {
		String fileName = Utils.getPropertyFormat(PROPERTY_SOURCE) + '/' + Utils.getPropertyFormat(PROPERTY_ELEMENT_NAME);
		String fileExists = Utils.getPropertyFormat(PROPERTY_SOURCE) + '/' + Utils.getPropertyFormat(PROPERTY_ELEMENT_NAME) + "_exists"; //$NON-NLS-1$

		script.printComment("Beginning of the jarUp task"); //$NON-NLS-1$
		script.printTargetDeclaration(TARGET_JARUP, null, null, null, Messages.assemble_jarUp);
		script.printAvailableTask(fileExists, fileName);
		Map params = new HashMap(2);
		params.put(PROPERTY_SOURCE, Utils.getPropertyFormat(PROPERTY_SOURCE));
		params.put(PROPERTY_ELEMENT_NAME, Utils.getPropertyFormat(PROPERTY_ELEMENT_NAME));
		script.printAntCallTask(TARGET_JARING, true, params);
		script.printTargetEnd();

		script.printTargetDeclaration(TARGET_JARING, null, fileExists, null, null);
		script.printJarTask(fileName + ".jar", fileName, null, "merge"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printDeleteTask(fileName, null, null);

		script.printTargetEnd();
		script.printComment("End of the jarUp task"); //$NON-NLS-1$

		script.printComment("Beginning of the jar signing  target"); //$NON-NLS-1$
		script.printTargetDeclaration(TARGET_JARSIGNING, null, null, null, Messages.sign_Jar);
		if (generateJnlp)
			script.printProperty(PROPERTY_UNSIGN, "true"); //$NON-NLS-1$
		script.println("<eclipse.jarProcessor sign=\"" + Utils.getPropertyFormat(PROPERTY_SIGN) + "\" pack=\"" + Utils.getPropertyFormat(PROPERTY_PACK)+ "\" unsign=\"" + Utils.getPropertyFormat(PROPERTY_UNSIGN) +  "\" jar=\"" + fileName + ".jar" + "\" alias=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS) + "\" keystore=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE) + "\" storepass=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ 
		script.printTargetEnd();
		script.printComment("End of the jarUp task"); //$NON-NLS-1$
	}

	protected void generateGZipTarget(boolean assembling) {
		//during the assemble stage, only zip if we aren't running the packager
		script.printTargetDeclaration(TARGET_GZIP_RESULTS, null, null, assembling ? PROPERTY_RUN_PACKAGER : null, null);
		script.println("<move file=\"" //$NON-NLS-1$
				+ Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + "\" tofile=\"" //$NON-NLS-1$
				+ Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' 
				+ Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + "/tmp.tar\"/>"); //$NON-NLS-1$
		script.printGZip(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + "/tmp.tar", //$NON-NLS-1$ 
				Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH));
		script.printTargetEnd();
	}

	protected void generatePrologue() {
		script.printProjectDeclaration("Assemble " + featureId, TARGET_MAIN, null); //$NON-NLS-1$  
		script.printProperty(PROPERTY_ARCHIVE_NAME, computeArchiveName());
		script.printProperty(PROPERTY_OS, configInfo.getOs());
		script.printProperty(PROPERTY_WS, configInfo.getWs());
		script.printProperty(PROPERTY_ARCH, configInfo.getArch());
		script.printProperty(PROPERTY_SIGN, (signJars ? Boolean.TRUE : Boolean.FALSE).toString());
		script.printProperty(PROPERTY_ASSEMBLY_TMP, Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/tmp"); //$NON-NLS-1$
		script.printProperty(PROPERTY_ECLIPSE_BASE, Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER)); 
		script.printProperty(PROPERTY_ECLIPSE_PLUGINS, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION);
		script.printProperty(PROPERTY_ECLIPSE_FEATURES, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION);
		script.printProperty(PROPERTY_ARCHIVE_FULLPATH, Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + Utils.getPropertyFormat(PROPERTY_BUILD_LABEL) + '/' + Utils.getPropertyFormat(PROPERTY_ARCHIVE_NAME)); 
		if (productFile != null && productFile.getLauncherName() != null)
			script.printProperty(PROPERTY_LAUNCHER_NAME, productFile.getLauncherName()); 
		script.printProperty(PROPERTY_TAR_ARGS, ""); //$NON-NLS-1$
		generatePackagingTargets();
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateInitializationSteps() {
		if (BundleHelper.getDefault().isDebugging()) {
			script.printEchoTask("basedir : " + Utils.getPropertyFormat(PROPERTY_BASEDIR)); //$NON-NLS-1$
			script.printEchoTask("assemblyTempDir : " + Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP)); //$NON-NLS-1$
			script.printEchoTask("eclipse.base : " + Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE)); //$NON-NLS-1$
			script.printEchoTask("collectingFolder : " + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER)); //$NON-NLS-1$
			script.printEchoTask("archivePrefix : " + Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX)); //$NON-NLS-1$
		}

		script.println("<condition property=\"" + PROPERTY_PLUGIN_ARCHIVE_PREFIX + "\" value=\"" + DEFAULT_PLUGIN_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<equals arg1=\"" + Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + "\"  arg2=\"\" trim=\"true\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_PLUGIN_ARCHIVE_PREFIX, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_PLUGIN_LOCATION);

		script.println();
		script.println("<condition property=\"" + PROPERTY_FEATURE_ARCHIVE_PREFIX + "\" value=\"" + DEFAULT_FEATURE_LOCATION + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.println("\t<equals arg1=\"" + Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + "\"  arg2=\"\" trim=\"true\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		script.println("</condition>"); //$NON-NLS-1$
		script.printProperty(PROPERTY_FEATURE_ARCHIVE_PREFIX, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + '/' + DEFAULT_FEATURE_LOCATION);

		script.println();

		script.printDirName(PROPERTY_ARCHIVE_PARENT, Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH));
		script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_PARENT));
		script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP));
		script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_BUILD_LABEL));
	}

	protected void generatePostProcessingSteps() {
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			generatePostProcessingSteps(plugin.getSymbolicName(), plugin.getVersion().toString(), (String) getFinalShape(plugin)[1], BUNDLE);
		}

		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			generatePostProcessingSteps(feature.getVersionedIdentifier().getIdentifier(), feature.getVersionedIdentifier().getVersion().toString(), (String) getFinalShape(feature)[1], FEATURE);
		}
	}

	protected void generateGatherBinPartsCalls() {
		Map properties = new HashMap(1);
		properties.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS));
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			String placeToGather = getLocation(plugin);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, properties);
		}

		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (int i = 0; i < features.length; i++) {
			IFeature feature = features[i];
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, properties);
		}

		//This will generate gather.bin.parts call to features that provides files for the root
		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			IFeature feature = (IFeature) iter.next();
			String placeToGather = feature.getURL().getPath();
			int j = placeToGather.lastIndexOf(Constants.FEATURE_FILENAME_DESCRIPTOR);
			if (j != -1)
				placeToGather = placeToGather.substring(0, j);
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_BIN_PARTS, null, null, properties);
		}
	}

	private void generateSignJarCall(String name, String version, byte type) {
		if (!signJars)
			return;
		Map properties = new HashMap(2);
		properties.put(PROPERTY_SOURCE, type == BUNDLE ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES));
		properties.put(PROPERTY_ELEMENT_NAME, name + '_' + version);
		script.printAntCallTask(TARGET_JARSIGNING, true, properties);
	}

	//generate the appropriate postProcessingCall
	private void generatePostProcessingSteps(String name, String version, String style, byte type) {
		if (FOLDER.equalsIgnoreCase(style))
			return;
		if (FILE.equalsIgnoreCase(style)) {
			generateJarUpCall(name, version, type);
			generateSignJarCall(name, version, type);
			generateJNLPCall(name, version, type);
			return;
		}
	}

	private void generateJNLPCall(String name, String version, byte type) {
		if (generateJnlp == false)
			return;
		if (type != FEATURE)
			return;

		String dir = type == BUNDLE ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES);
		String location = dir + '/' + name + '_' + version + ".jar"; //$NON-NLS-1$
		script.println("<eclipse.jnlpGenerator feature=\"" +  script.getEscaped(location) + "\"  codebase=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_CODEBASE) + "\" j2se=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_J2SE) + "\" locale=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_LOCALE) + "\" generateOfflineAllowed=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_GENOFFLINE) + "\" configInfo=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_CONFIGS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	private boolean getUnpackClause(BundleDescription bundle) {
		Set entries = (Set) ((Properties)bundle.getUserObject()).get(PLUGIN_ENTRY);
		return ((PluginEntry) entries.iterator().next()).isUnpack();
	}

	protected Object[] getFinalShape(BundleDescription bundle) {
		String style = getUnpackClause(bundle) ? FLAT : UPDATEJAR;
		return getFinalShape(bundle.getSymbolicName(), bundle.getVersion().toString(), style, BUNDLE);
	}

	protected Object[] getFinalShape(IFeature feature) {
		return getFinalShape(feature.getVersionedIdentifier().getIdentifier(), feature.getVersionedIdentifier().getVersion().toString(), FLAT, FEATURE);
	}

	protected Object[] getFinalShape(String name, String version, String initialShape, byte type) {
		String style = initialShape;
		style = getShapeOverride(name, type, style);

		if (FLAT.equalsIgnoreCase(style)) {
			//do nothing
			return new Object[] {name + '_' + version, FOLDER};
		}
		if (UPDATEJAR.equalsIgnoreCase(style)) {
			return new Object[] {name + '_' + version + ".jar", FILE}; //$NON-NLS-1$
		}
		return new Object[] {name + '_' + version, FOLDER};
	}

	private String getShapeOverride(String name, byte type, String initialStyle) {
		String result = initialStyle;
		Properties currentProperties = type == BUNDLE ? pluginsPostProcessingSteps : featuresPostProcessingSteps;
		if (currentProperties.size() > 0) {
			String styleFromFile = currentProperties.getProperty(name);
			if (styleFromFile == null)
				styleFromFile = currentProperties.getProperty(DEFAULT_FINAL_SHAPE);
			result = styleFromFile;
		}
		if (forceUpdateJarFormat)
			result = UPDATEJAR;
		return result;
	}

	private void generateJarUpCall(String name, String version, byte type) {
		Map properties = new HashMap(2);
		properties.put(PROPERTY_SOURCE, type == BUNDLE ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES));
		properties.put(PROPERTY_ELEMENT_NAME, name + '_' + version);
		script.printAntCallTask(TARGET_JARUP, true, properties);
	}

	private void generateEpilogue() {
		if (!FORMAT_FOLDER.equalsIgnoreCase(archiveFormat))
			script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), null, null);
		script.printTargetEnd();
		if (FORMAT_TAR.equalsIgnoreCase(archiveFormat)) 
			generateGZipTarget(true);
		script.printProjectEnd();
		script.close();
		script = null;
	}

	public String getTargetName() {
		return DEFAULT_ASSEMBLE_NAME + (featureId.equals("") ? "" : ('.' + featureId)) + (configInfo.equals(Config.genericConfig()) ? "" : ('.' + configInfo.toStringReplacingAny(".", ANY_STRING))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
	}

	private void generateZipTarget() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);
		for (int i = 0; i < plugins.length; i++) {
			parameters.add(Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) getFinalShape(plugins[i])[0]);
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		for (int i = 0; i < features.length; i++) {
			parameters.add(Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) getFinalShape(features[i])[0]);
			if (i % parameterSize == 0) {
				createZipExecCommand(parameters);
				parameters.clear();
			}
		}
		if (!parameters.isEmpty()) {
			createZipExecCommand(parameters);
			parameters.clear();
		}

		createZipRootFileCommand();
	}

	/**
	 *  Zip the root files
	 */
	private void createZipRootFileCommand() {
		if (rootFileProviders.size() == 0)
			return;

		List parameters = new ArrayList(1);
		parameters.add("-r -q ${zipargs} '" + Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + "' . "); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("zip", Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING), parameters, null); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	private void createZipExecCommand(List parameters) {
		parameters.add(0, "-r -q " + Utils.getPropertyFormat(PROPERTY_ZIP_ARGS) + " '" + Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + '\''); //$NON-NLS-1$ //$NON-NLS-2$
		script.printExecTask("zip", Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), parameters, null); //$NON-NLS-1$ 
	}

	protected String computeArchiveName() {
		String extension = (FORMAT_TAR.equalsIgnoreCase(archiveFormat) || FORMAT_ANTTAR.equalsIgnoreCase(archiveFormat)) ? ".tar.gz" : ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
		return featureId + "-" + Utils.getPropertyFormat(PROPERTY_BUILD_ID_PARAM) + (configInfo.equals(Config.genericConfig()) ? "" : ("-" + configInfo.toStringReplacingAny(".", ANY_STRING))) + extension; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public void generateTarGZTasks(boolean assembling) {
		//This task only supports creation of archive with eclipse at the root 
		//Need to do the copy using cp because of the link
		List parameters = new ArrayList(2);
		if (rootFileProviders.size() > 0) {
			parameters.add("-r '" + Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + "' '" + Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '\''); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$  
			script.printExecTask("cp", Utils.getPropertyFormat(PROPERTY_BASEDIR), parameters, null); //$NON-NLS-1$

			parameters.clear();
			parameters.add("-rf '" + Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '\''); //$NON-NLS-1$ //$NON-NLS-2$
			script.printExecTask("rm", Utils.getPropertyFormat(PROPERTY_BASEDIR), parameters, null); //$NON-NLS-1$
		}
		parameters.clear();
		String tarArgs = assembling ? "-cvf '" : "-rvf '";  //$NON-NLS-1$//$NON-NLS-2$
		parameters.add(Utils.getPropertyFormat(PROPERTY_TAR_ARGS) + tarArgs + Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + "' " + Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + ' '); //$NON-NLS-1$
		script.printExecTask("tar", Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), parameters, null); //$NON-NLS-1$ 
		
		script.printAntCallTask(TARGET_GZIP_RESULTS, true, null );
		
		List args = new ArrayList(2);
		args.add("-rf"); //$NON-NLS-1$
		args.add('\'' + Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '\'');
		script.printExecTask("rm", null, args, null); //$NON-NLS-1$
	}

	//TODO this code andn the generateAntTarTarget() should be refactored using a factory or something like that.
	protected void generateAntZipTarget() {
		FileSet[] filesPlugins = new FileSet[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			Object[] shape = getFinalShape(plugins[i]);
			filesPlugins[i] = new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (plugins.length != 0)
			script.printZipTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesPlugins);

		FileSet[] filesFeatures = new FileSet[features.length];
		for (int i = 0; i < features.length; i++) {
			Object[] shape = getFinalShape(features[i]);
			filesFeatures[i] = new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (features.length != 0)
			script.printZipTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesFeatures);

		if (rootFileProviders.size() == 0)
			return;

		if (groupConfigs) {
			List allConfigs = getConfigInfos();
			FileSet[] rootFiles = new FileSet[allConfigs.size()];
			int i = 0;
			for (Iterator iter = allConfigs.iterator(); iter.hasNext();) {
				Config elt = (Config) iter.next();
				rootFiles[i++] = new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + elt.toStringReplacingAny(".", ANY_STRING), false, null, "**/**", null, null, null, elt.toStringReplacingAny(".", ANY_STRING), null, null); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}
			script.printZipTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, rootFiles);
		} else {
			FileSet[] permissionSets = generatePermissions(true);
			FileSet[] rootFiles = new FileSet[permissionSets.length + 1];
			String toExcludeFromArchive = Utils.getStringFromCollection(this.addedByPermissions, ","); //$NON-NLS-1$
			System.arraycopy(permissionSets, 0, rootFiles, 1, permissionSets.length);
			rootFiles[0] = new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER), false, null, "**/**", null, toExcludeFromArchive, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, null); //$NON-NLS-1$//$NON-NLS-2$
			script.printZipTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, rootFiles);
		}
	}

	protected FileSet[] generatePermissions(boolean zip) {
		String configInfix = configInfo.toString("."); //$NON-NLS-1$
		String prefixPermissions = ROOT_PREFIX + configInfix + '.' + PERMISSIONS + '.';
		String commonPermissions = ROOT_PREFIX + PERMISSIONS + '.';
		ArrayList fileSets = new ArrayList();

		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			Properties featureProperties = null;
			try {
				featureProperties = AbstractScriptGenerator.readProperties(new Path(((IFeature) iter.next()).getURL().getFile()).removeLastSegments(1).toOSString(), PROPERTIES_FILE, IStatus.OK);
			} catch (CoreException e) {
				return new FileSet[0];
			}

			for (Iterator iter2 = featureProperties.entrySet().iterator(); iter2.hasNext();) {
				Map.Entry permission = (Map.Entry) iter2.next();
				String instruction = (String) permission.getKey();
				String parameters = (String) permission.getValue();
				String[] values = Utils.getArrayFromString(parameters);
				for (int i = 0; i < values.length; i++) {
					boolean isFile = ! values[i].endsWith("/"); //$NON-NLS-1$
					String prefix = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER); //$NON-NLS-1$
					if (instruction.startsWith(prefixPermissions)) {
						addedByPermissions.add(values[i]);
						if (zip)
							fileSets.add(new ZipFileSet(prefix + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) +  (isFile ? '/' + values[i] : ""), null, instruction.substring(prefixPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						else
							fileSets.add(new TarFileSet(prefix + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(prefixPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						continue;
					}
					if (instruction.startsWith(commonPermissions)) {
						addedByPermissions.add(values[i]);
						if (zip)
							fileSets.add(new ZipFileSet(prefix + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(commonPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						else
							fileSets.add(new TarFileSet(prefix + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(commonPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						continue;
					}
				}
			}
		}
		return (FileSet[]) fileSets.toArray(new FileSet[fileSets.size()]);
	}

	//TODO this code andn the generateAntZipTarget() should be refactored using a factory or something like that.
	private void generateAntTarTarget() {
		FileSet[] filesPlugins = new FileSet[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			Object[] shape = getFinalShape(plugins[i]);
			filesPlugins[i] = new TarFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (plugins.length != 0)
			script.printTarTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesPlugins);

		FileSet[] filesFeatures = new FileSet[features.length];
		for (int i = 0; i < features.length; i++) {
			Object[] shape = getFinalShape(features[i]);
			filesFeatures[i] = new TarFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1] == FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null);
		}
		if (features.length != 0)
			script.printTarTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, filesFeatures);

		if (rootFileProviders.size() == 0)
			return;

		FileSet[] permissionSets = generatePermissions(false);
		FileSet[] rootFiles = new FileSet[permissionSets.length + 1];
		System.arraycopy(permissionSets, 0, rootFiles, 1, permissionSets.length);
		rootFiles[0] = new TarFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER), false, null, "**/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, null); //$NON-NLS-1$//$NON-NLS-2$
		script.printTarTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, rootFiles);
	}

	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}

	public void setSignJars(boolean value) {
		signJars = value;
	}

	public void setProduct(String value) {
		product = value;
	}

	public void setArchiveFormat(String archiveFormat) {
		this.archiveFormat = archiveFormat;
	}
	
	public void setGroupConfigs(boolean group) {
		groupConfigs = group;
	}
}
