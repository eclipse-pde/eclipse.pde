/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - Initial API and implementation
 *    G&H Softwareentwicklung GmbH - internationalization implementation (bug 150933)
 *    Prosyst - create proper OSGi bundles (bug 174157)
 *    Felix Riegger (SAP AG) - consolidation of publishers for PDE formats (bug 331974)
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Generate an assemble script for a given feature and a given config. It
 * generates all the instruction to zip the listed plugins and features.
 */
public class AssembleConfigScriptGenerator extends AbstractScriptGenerator {
	protected String directory; // representing the directory where to generate the file
	protected String featureId;
	protected Config configInfo;
	protected BuildTimeFeature[] features; // the features that will be assembled
	protected BuildTimeFeature[] allFeatures; //the set of all the features that have been considered
	protected BundleDescription[] plugins;
	protected String filename;
	protected Collection rootFileProviders;
	protected String rootFolder = null;
	protected ArrayList addedByPermissions = new ArrayList(); //contains the list of files and folders that have been added to an archive by permission management

	private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$
	private static final String PROPERTY_ELEMENT_NAME = "elementName"; //$NON-NLS-1$

	private static final byte BUNDLE_TYPE = 0;
	private static final byte FEATURE_TYPE = 1;

	protected String PROPERTY_ECLIPSE_PLUGINS = "eclipse.plugins"; //$NON-NLS-1$
	protected String PROPERTY_ECLIPSE_FEATURES = "eclipse.features"; //$NON-NLS-1$
	protected boolean signJars;
	private boolean generateJnlp;

	private String archiveFormat;
	private boolean groupConfigs = false;
	private String product;
	private ProductFile productFile = null;
	protected ShapeAdvisor shapeAdvisor = null;
	private Boolean p2Bundles = null;

	public AssembleConfigScriptGenerator() {
		super();
	}

	public void initialize(String directoryName, String feature, Config configurationInformation, Collection elementList, Collection featureList, Collection allFeaturesList, Collection rootProviders) throws CoreException {
		this.directory = directoryName;
		this.featureId = feature;
		this.configInfo = configurationInformation;
		this.rootFileProviders = rootProviders != null ? rootProviders : new ArrayList(0);
		this.rootFolder = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER); //$NON-NLS-1$
		this.features = new BuildTimeFeature[featureList.size()];
		featureList.toArray(this.features);

		this.allFeatures = new BuildTimeFeature[allFeaturesList.size()];
		allFeaturesList.toArray(this.allFeatures);

		this.plugins = new BundleDescription[elementList.size()];
		this.plugins = (BundleDescription[]) elementList.toArray(this.plugins);

		openScript(directoryName, getTargetName() + ".xml"); //$NON-NLS-1$
		shapeAdvisor = new ShapeAdvisor();
		shapeAdvisor.setForceUpdateJars(forceUpdateJarFormat);
	}

	protected String computeIconsList() {
		return computeIconsList(configInfo.getOs());
	}

	protected String computeIconsList(String os) {
		String result = Utils.getPropertyFormat(PROPERTY_LAUNCHER_ICONS);
		if (getProductFile() == null)
			return result;
		String[] icons = os != null ? productFile.getIcons(os) : productFile.getIcons();
		for (int i = 0; i < icons.length; i++) {

			String icon = Utils.makeRelative(new Path(icons[i]), new Path(productFile.getLocation().getParent())).toOSString();

			String location = findFile(icon, true);
			if (location == null) {
				File iconFile = new File(productFile.getLocation().getParentFile(), icon);
				if (iconFile.exists())
					location = Utils.makeRelative(new Path(iconFile.getAbsolutePath()), new Path(workingDirectory)).toOSString();
			}
			if (location != null)
				result += ", " + Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + location; //$NON-NLS-1$
			else {
				result += ", " + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + icon; //$NON-NLS-1$
				result += ", " + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + DEFAULT_FEATURE_LOCATION + '/' + icon; //$NON-NLS-1$
			}
		}
		return result;
	}

	public void generate() {
		try {
			// Note that we must pass the OS information in to load product
			// so that any icon files can be calculated for the 
			// generateBrandingCalls.
			productFile = loadProduct(product, configInfo.getOs());
		} catch (CoreException e) {
			//ignore
		}
		generatePrologue();

		generateMainBegin();
		generateInitializationSteps();
		if (BuildDirector.p2Gathering) {
			generateP2Assembling();
		} else {
			generateGatherCalls();
			generateProcessingCalls();
			generateBrandingCalls();
			generateP2Steps();
		}
		generateArchivingCalls();
		generateMainEnd();

		generateEpilogue();
	}

	protected void generateGatherCalls() {
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, true, null);

		if (embeddedSource)
			script.printAntCallTask(TARGET_GATHER_SOURCES, true, null);

		printCustomAssemblyAntCall(PROPERTY_POST + TARGET_GATHER_BIN_PARTS, null);
		script.println();
	}

	protected void generateP2Assembling() {
		if (productFile != null) {
			script.printAntCallTask(TARGET_RUN_DIRECTOR, true, null);
			script.printAntCallTask(TARGET_MIRROR_PRODUCT, true, null);
		} else {
			script.printAntCallTask(TARGET_MIRROR_ARCHIVE, true, null);
		}
	}

	protected void generateMirrorProductTask() {
		Map mirrorArgs = new HashMap();
		mirrorArgs.put(PROPERTY_P2_MIRROR_METADATA_DEST, Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO));
		mirrorArgs.put(PROPERTY_P2_MIRROR_ARTIFACT_DEST, Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO));

		script.printTargetDeclaration(TARGET_MIRROR_PRODUCT, null, PROPERTY_P2_METADATA_REPO, PROPERTY_SKIP_MIRRORING, null);
		script.printAntCallTask(TARGET_MIRROR_ARCHIVE, true, mirrorArgs);
		script.printTargetEnd();
		script.println();
	}

	protected void generateMirrorTask(boolean assembling) {
		script.printTargetDeclaration(TARGET_MIRROR_ARCHIVE, null, null, PROPERTY_SKIP_MIRRORING, null);
		script.printProperty(PROPERTY_P2_APPEND, "true"); //$NON-NLS-1$
		script.printProperty(PROPERTY_P2_MIRROR_METADATA_DEST, "file:" + Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE)); //$NON-NLS-1$
		script.printProperty(PROPERTY_P2_MIRROR_ARTIFACT_DEST, "file:" + Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE)); //$NON-NLS-1$
		if (features.length + plugins.length + rootFileProviders.size() > 0 || productFile != null) {
			script.printTab();
			script.print("<p2.mirror "); //$NON-NLS-1$
			script.printAttribute("source", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$
			script.printTab();
			script.print("\t<destination "); //$NON-NLS-1$
			script.printAttribute("location", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_METADATA_DEST), true); //$NON-NLS-1$ 
			script.printAttribute("name", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO_NAME), true); //$NON-NLS-1$
			script.printAttribute("compressed", Utils.getPropertyFormat(PROPERTY_P2_COMPRESS), true); //$NON-NLS-1$
			script.printAttribute("append", assembling ? Utils.getPropertyFormat(PROPERTY_P2_APPEND) : TRUE, true); //$NON-NLS-1$
			script.printAttribute("kind", "metadata", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("/>"); //$NON-NLS-1$
			script.printTab();
			script.print("\t<destination "); //$NON-NLS-1$
			script.printAttribute("location", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_ARTIFACT_DEST), true); //$NON-NLS-1$ 
			script.printAttribute("name", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO_NAME), true); //$NON-NLS-1$
			script.printAttribute("compressed", Utils.getPropertyFormat(PROPERTY_P2_COMPRESS), true); //$NON-NLS-1$
			script.printAttribute("append", assembling ? Utils.getPropertyFormat(PROPERTY_P2_APPEND) : TRUE, true); //$NON-NLS-1$
			script.printAttribute("kind", "artifact", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("/>"); //$NON-NLS-1$
			script.printTab();

			for (int i = 0; i < plugins.length; i++) {
				BundleDescription plugin = plugins[i];
				script.printTab();
				script.print("\t<iu "); //$NON-NLS-1$
				script.printAttribute(ID, plugin.getSymbolicName(), true);
				script.printAttribute(VERSION, plugin.getVersion().toString(), true);
				script.println("/>"); //$NON-NLS-1$
			}

			for (int i = 0; i < features.length; i++) {
				BuildTimeFeature feature = features[i];
				script.printTab();
				script.print("\t<iu"); //$NON-NLS-1$
				script.printAttribute(ID, getFeatureGroupId(feature), true);
				script.printAttribute(VERSION, feature.getVersion(), true);
				script.println("/>"); //$NON-NLS-1$
			}

			for (Iterator iterator = rootFileProviders.iterator(); iterator.hasNext();) {
				BuildTimeFeature rootProvider = (BuildTimeFeature) iterator.next();
				if (!(havePDEUIState() && rootProvider.getId().equals("org.eclipse.pde.container.feature"))) { //$NON-NLS-1$
					script.printTab();
					script.print("\t<iu"); //$NON-NLS-1$
					script.printAttribute(ID, getFeatureGroupId(rootProvider), true);
					script.printAttribute(VERSION, rootProvider.getVersion(), true);
					script.println("/>"); //$NON-NLS-1$
				}
			}
			if (productFile != null) {
				script.printTab();
				script.print("\t<iu"); //$NON-NLS-1$
				script.printAttribute(ID, productFile.getId(), true);
				script.printAttribute(VERSION, getReplacedProductVersion(), true);
				script.println("/>"); //$NON-NLS-1$
			}
			//categories
			script.printTab();
			script.println("<iu query=\"property[@name='org.eclipse.equinox.p2.type.category']\" required=\"false\" />"); //$NON-NLS-1$
			script.println("</p2.mirror>"); //$NON-NLS-1$
		}
		script.printTargetEnd();
		script.println();
	}

	protected String getFeatureGroupId(BuildTimeFeature feature) {
		if (!feature.isBinary()) {
			Properties properties = getFeatureBuildProperties(feature);
			if (properties.containsKey(PROPERTY_P2_GROUP_ID))
				return properties.getProperty(PROPERTY_P2_GROUP_ID);
		}
		return feature.getId() + ".feature.group"; //$NON-NLS-1$
	}

	protected String getReplacedProductVersion() {
		String productVersion = productFile.getVersion();
		if (productVersion.endsWith(PROPERTY_QUALIFIER)) {
			Version version = new Version(productVersion);
			StringBuffer buffer = new StringBuffer();
			buffer.append(version.getMajor());
			buffer.append('.');
			buffer.append(version.getMinor());
			buffer.append('.');
			buffer.append(version.getMicro());
			buffer.append('.');
			buffer.append(Utils.getPropertyFormat(PROPERTY_P2_PRODUCT_QUALIFIER));

			productVersion = buffer.toString();
		}
		return productVersion;
	}

	protected void generateDirectorTarget(boolean assembling) {
		if (assembling) {
			script.printTargetDeclaration(TARGET_RUN_DIRECTOR_CONDITION, null, null, null, null);
			script.printTab();
			script.print("<condition"); //$NON-NLS-1$
			script.printAttribute("property", TARGET_RUN_DIRECTOR_CONDITION, true); //$NON-NLS-1$
			script.printAttribute("value", TRUE, true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$
			script.println("\t<or>"); //$NON-NLS-1$
			script.println("\t\t<isset property=\"" + PROPERTY_RUN_PACKAGER + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("\t\t<isset property=\"" + PROPERTY_SKIP_DIRECTOR + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("\t</or>"); //$NON-NLS-1$
			script.printEndTag("condition"); //$NON-NLS-1$
			script.printTargetEnd();
		}

		script.printTargetDeclaration(TARGET_RUN_DIRECTOR, assembling ? TARGET_RUN_DIRECTOR_CONDITION : null, null, assembling ? TARGET_RUN_DIRECTOR_CONDITION : PROPERTY_SKIP_DIRECTOR, null);
		Map parameters = new HashMap();
		parameters.put(PROPERTY_OS, Utils.getPropertyFormat(PROPERTY_OS));
		parameters.put(PROPERTY_WS, Utils.getPropertyFormat(PROPERTY_WS));
		parameters.put(PROPERTY_ARCH, Utils.getPropertyFormat(PROPERTY_ARCH));
		parameters.put(PROPERTY_P2_REPO, Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO));
		parameters.put(PROPERTY_P2_DIRECTOR_IU, productFile != null ? productFile.getId() : Utils.getPropertyFormat(PROPERTY_P2_ROOT_NAME));
		parameters.put(PROPERTY_P2_DIRECTOR_VERSION, productFile != null ? getReplacedProductVersion() : Utils.getPropertyFormat(PROPERTY_P2_ROOT_VERSION));
		parameters.put(PROPERTY_P2_DIRECTOR_INSTALLPATH, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		script.printAntTask(Utils.getPropertyFormat(PROPERTY_GENERIC_TARGETS), null, TARGET_RUN_DIRECTOR, null, TRUE, parameters);
		script.println();
		script.printTargetEnd();
		script.println();
	}

	private void generateProcessingCalls() {
		script.printAntCallTask(TARGET_JAR_PROCESSING, true, null);
		script.println();
	}

	private void generateArchivingCalls() {
		script.printAntCallTask(TARGET_ASSEMBLE_ARCHIVE, true, null);
	}

	protected void generateMainBegin() {
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	protected void generateMainEnd() {
		script.printAntCallTask(TARGET_CLEANUP_ASSEMBLY, true, null);
		script.printTargetEnd();
		script.println();
	}

	protected void generateCleanupAssembly(boolean assembling) {
		String condition = (assembling && BuildDirector.p2Gathering) ? PROPERTY_RUN_PACKAGER : null;
		script.printTargetDeclaration(TARGET_CLEANUP_ASSEMBLY, null, null, condition, null);
		if (!FORMAT_FOLDER.equalsIgnoreCase(archiveFormat))
			script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), null, null);
		script.printTargetEnd();
		script.println();
	}

	/**
	 * 
	 */
	protected void generateBrandingCalls() {
		script.printBrandTask(rootFolder, computeIconsList(), Utils.getPropertyFormat(PROPERTY_LAUNCHER_NAME), Utils.getPropertyFormat(PROPERTY_OS));
	}

	private void generateP2Steps() {
		if (!haveP2Bundles())
			return;
		if (rootFileProviders.size() == 0 && features.length == 0 && plugins.length == 0)
			return;
		script.printAntCallTask(TARGET_P2_METADATA, true, null);
		script.println();
	}

	protected void generateArchivingTarget(boolean assembling) {
		boolean condition = assembling && BuildDirector.p2Gathering;
		if (condition) {
			script.printTargetDeclaration(TARGET_ASSEMBLE_ARCHIVE_CONDITION, null, null, null, null);
			script.printTab();
			script.print("<condition"); //$NON-NLS-1$
			script.printAttribute("property", TARGET_ASSEMBLE_ARCHIVE_CONDITION, true); //$NON-NLS-1$
			script.printAttribute("value", TRUE, true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$
			script.println("\t<or>"); //$NON-NLS-1$
			script.println("\t\t<isset property=\"" + PROPERTY_RUN_PACKAGER + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			if (productFile != null)
				script.println("\t\t<isset property=\"" + PROPERTY_SKIP_DIRECTOR + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			else
				script.println("\t\t<isset property=\"" + PROPERTY_SKIP_MIRRORING + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("\t</or>"); //$NON-NLS-1$
			script.printEndTag("condition"); //$NON-NLS-1$
			script.printTargetEnd();
		}

		// Only run the archive target if there is something there to archive.
		// if not p2Gathering, no conditions,
		// else, if assembling condition is (runPackager || skipDirector), if packaging condition is skipDirector
		String assemblyCondition = BuildDirector.p2Gathering ? TARGET_ASSEMBLE_ARCHIVE_CONDITION : null;
		String packageCondition = BuildDirector.p2Gathering ? (productFile != null ? PROPERTY_SKIP_DIRECTOR : PROPERTY_SKIP_MIRRORING) : null;
		script.printTargetDeclaration(TARGET_ASSEMBLE_ARCHIVE, condition ? TARGET_ASSEMBLE_ARCHIVE_CONDITION : null, null, assembling ? assemblyCondition : packageCondition, null);
		Map properties = new HashMap();
		properties.put(PROPERTY_ROOT_FOLDER, rootFolder);
		printCustomAssemblyAntCall(PROPERTY_PRE + "archive", properties); //$NON-NLS-1$

		if (FORMAT_FOLDER.equalsIgnoreCase(archiveFormat)) {
			generateMoveRootFiles();
		} else if (FORMAT_ZIP.equalsIgnoreCase(archiveFormat)) {
			generateZipTarget();
		} else if (FORMAT_ANTZIP.equalsIgnoreCase(archiveFormat)) {
			generateAntZipTarget();
		} else if (FORMAT_ANTTAR.equalsIgnoreCase(archiveFormat)) {
			generateAntTarTarget();
		} else if (FORMAT_TAR.equalsIgnoreCase(archiveFormat)) {
			generateTarGZTasks(true);
		}
		script.printTargetEnd();
		script.println();
	}

	private void generateMoveRootFiles() {
		if (rootFileProviders.size() == 0 || BuildDirector.p2Gathering)
			return;

		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			Object object = iter.next();
			if (object instanceof BuildTimeFeature) {
				Properties featureProperties = getFeatureBuildProperties((BuildTimeFeature) object);
				Utils.generatePermissions(featureProperties, configInfo, PROPERTY_ECLIPSE_BASE, script);
			}
		}

		if (Platform.getOS().equals("win32")) { //$NON-NLS-1$
			FileSet[] rootFiles = new FileSet[1];
			rootFiles[0] = new FileSet(rootFolder, null, "**/**", null, null, null, null); //$NON-NLS-1$
			script.printMoveTask(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), rootFiles, false);
			script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING), null, null); //$NON-NLS-1$
		} else {
			List params = new ArrayList(3);
			params.add("-R"); //$NON-NLS-1$
			params.add("."); //$NON-NLS-1$
			params.add('\'' + Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '\'');
			String rootFileFolder = Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + configInfo.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
			script.printExecTask("cp", rootFileFolder + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER), params, null); //$NON-NLS-1$
			script.printDeleteTask(rootFileFolder, null, null);
		}
	}

	protected Properties getFeatureBuildProperties(BuildTimeFeature feature) {
		if (feature.isBinary())
			return null;
		try {
			return AbstractScriptGenerator.readProperties(new Path(feature.getRootLocation()).toOSString(), PROPERTIES_FILE, IStatus.OK);
		} catch (CoreException e) {
			return null;
		}

	}

	protected void generateGatherSourceTarget() {
		script.printTargetDeclaration(TARGET_GATHER_SOURCES, null, null, null, null);
		Map properties = new HashMap(1);
		properties.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS));

		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			String placeToGather = getLocation(plugin);

			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_SOURCES, null, null, properties);

			Properties bundleProperties = (Properties) plugin.getUserObject();
			//Source code for plugins with . on the classpath must be put in a folder in the final jar.
			if (bundleProperties.get(WITH_DOT) == Boolean.TRUE) {
				String targetLocation = Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) + '/' + ModelBuildScriptGenerator.getNormalizedName(plugin);
				String targetLocationSrc = targetLocation + "/src"; //$NON-NLS-1$

				//Find the source zip where it has been gathered and extract it in a folder  
				script.println("<unzip dest=\"" + AntScript.getEscaped(targetLocationSrc) + "\">"); //$NON-NLS-1$//$NON-NLS-2$
				script.println("\t<fileset dir=\"" + AntScript.getEscaped(targetLocation) + "\" includes=\"**/*src.zip\" casesensitive=\"false\"/>"); //$NON-NLS-1$//$NON-NLS-2$
				script.println("</unzip>"); //$NON-NLS-1$

				//	Delete the source zip where it has been gathered since we extracted it
				script.printDeleteTask(null, null, new FileSet[] {new FileSet(targetLocation, null, "**/*src.zip", null, null, null, "false")}); //$NON-NLS-1$ //$NON-NLS-2$//$NON-bNLS-3$
			}
		}

		properties = new HashMap(1);
		properties.put(PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		for (int i = 0; i < features.length; i++) {
			BuildTimeFeature feature = features[i];
			String placeToGather = feature.getRootLocation();
			script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), TARGET_GATHER_SOURCES, null, null, properties);
		}

		script.printTargetEnd();
		script.println();
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
		script.printAvailableTask(PROPERTY_JARING_MANIFEST, fileName + '/' + JarFile.MANIFEST_NAME);
		script.printConditionIsSet(PROPERTY_JARING_TASK, TARGET_JARING, PROPERTY_JARING_MANIFEST, TARGET_JARING + "_NoManifest"); //$NON-NLS-1$
		script.printAntCallTask(Utils.getPropertyFormat(PROPERTY_JARING_TASK), true, params);
		script.printTargetEnd();
		script.println();

		script.printTargetDeclaration(TARGET_JARING, null, fileExists, null, null);
		script.printJarTask(fileName + ".jar", fileName, fileName + '/' + JarFile.MANIFEST_NAME, "skip"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printDeleteTask(fileName, null, null);
		script.printTargetEnd();
		script.println();

		script.printTargetDeclaration(TARGET_JARING + "_NoManifest", null, fileExists, null, null); //$NON-NLS-1$
		script.printJarTask(fileName + ".jar", fileName, null, "merge"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printDeleteTask(fileName, null, null);
		script.printTargetEnd();
		script.printComment("End of the jarUp task"); //$NON-NLS-1$
		script.println();

		script.printComment("Beginning of the jar signing  target"); //$NON-NLS-1$
		script.printTargetDeclaration(TARGET_JARSIGNING, null, null, null, Messages.sign_Jar);
		printCustomAssemblyAntCall(PROPERTY_PRE + TARGET_JARSIGNING, null);
		if (generateJnlp)
			script.printProperty(PROPERTY_UNSIGN, "true"); //$NON-NLS-1$
		script.println("<eclipse.jarProcessor sign=\"" + Utils.getPropertyFormat(PROPERTY_SIGN) + "\" pack=\"" + Utils.getPropertyFormat(PROPERTY_PACK) + "\" unsign=\"" + Utils.getPropertyFormat(PROPERTY_UNSIGN) + "\" jar=\"" + fileName + ".jar" + "\" alias=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS) + "\" keystore=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE) + "\" storepass=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS) + "\" keypass=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_KEYPASS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ 
		script.printTargetEnd();
		script.printComment("End of the jarUp task"); //$NON-NLS-1$
		script.println();
	}

	protected void generateGZipTarget(boolean assembling) {
		//during the assemble stage, only zip if we aren't running the packager
		script.printTargetDeclaration(TARGET_GZIP_RESULTS, null, null, assembling ? PROPERTY_RUN_PACKAGER : null, null);
		script.println("<move file=\"" //$NON-NLS-1$
				+ Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + "\" tofile=\"" //$NON-NLS-1$
				+ Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '/' + Utils.getPropertyFormat(PROPERTY_COLLECTING_FOLDER) + "/tmp.tar\"/>"); //$NON-NLS-1$
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
		printLauncherJarProperty();
		script.printProperty(PROPERTY_P2_BUILD_REPO, "file:" + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/buildRepo"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_GENERIC_TARGETS, Utils.getPropertyFormat("eclipse.pdebuild.scripts") + '/' + "/genericTargets.xml"); //$NON-NLS-1$//$NON-NLS-2$
		script.printAvailableTask(PROPERTY_CUSTOM_ASSEMBLY, "${builder}/customAssembly.xml", "${builder}/customAssembly.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		if (productQualifier != null)
			script.printProperty(PROPERTY_P2_PRODUCT_QUALIFIER, productQualifier);

		if (productFile != null && productFile.getLauncherName() != null)
			script.printProperty(PROPERTY_LAUNCHER_NAME, productFile.getLauncherName());
		script.printProperty(PROPERTY_TAR_ARGS, ""); //$NON-NLS-1$
		script.println();

		generateCustomGatherMacro();
		generatePackagingTargets();
	}

	protected void printLauncherJarProperty() {
		Bundle launcherBundle = Platform.getBundle(BUNDLE_EQUINOX_LAUNCHER);
		try {
			File bundleFile = FileLocator.getBundleFile(launcherBundle);
			script.printProperty(PROPERTY_LAUNCHER_JAR, bundleFile.getAbsolutePath());
		} catch (IOException e) {
			// what can we do with this?
		}
	}

	protected void generateCustomGatherMacro() {
		List attributes = new ArrayList(5);
		attributes.add("dir"); //$NON-NLS-1$
		attributes.add("propertyName"); //$NON-NLS-1$
		attributes.add("propertyValue"); //$NON-NLS-1$
		attributes.add("subFolder"); //$NON-NLS-1$
		attributes.add(PROPERTY_PROJECT_NAME);
		script.printMacroDef(PROPERTY_CUSTOM_GATHER, attributes);

		Map params = new HashMap();
		params.put("@{propertyName}", "@{propertyValue}"); //$NON-NLS-1$//$NON-NLS-2$
		script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, "@{dir}", TARGET_GATHER_BIN_PARTS, null, null, params); //$NON-NLS-1$

		params.put(PROPERTY_PROJECT_LOCATION, "${basedir}/@{dir}"); //$NON-NLS-1$
		params.put(PROPERTY_PROJECT_NAME, "@{projectName}"); //$NON-NLS-1$
		params.put(PROPERTY_TARGET_FOLDER, "@{propertyValue}@{subFolder}"); //$NON-NLS-1$
		printCustomAssemblyAntCall(TARGET_GATHER_BIN_PARTS, params);

		script.printEndMacroDef();
		script.println();
	}

	protected void printCustomGatherCall(String fullName, String dir, String propertyName, String propertyValue, String subFolder) {
		script.println("<" + PROPERTY_CUSTOM_GATHER + " dir=\"" + dir + "\" projectName=\"" + fullName + "\" propertyName=\"" + propertyName + "\" propertyValue=\"" + propertyValue + "\" subFolder=\"" + (subFolder != null ? subFolder : "") + "\" />"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
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
		script.println();
	}

	protected void generatePostProcessingTarget() {
		script.printTargetDeclaration(TARGET_JAR_PROCESSING, null, null, null, null);
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			generatePostProcessingSteps(plugin.getSymbolicName(), plugin.getVersion().toString(), (String) shapeAdvisor.getFinalShape(plugin)[1], BUNDLE_TYPE);
		}

		for (int i = 0; i < features.length; i++) {
			BuildTimeFeature feature = features[i];
			generatePostProcessingSteps(feature.getId(), feature.getVersion(), (String) shapeAdvisor.getFinalShape(feature)[1], FEATURE_TYPE);
		}
		printCustomAssemblyAntCall(PROPERTY_POST + TARGET_JARUP, null);
		script.printTargetEnd();
		script.println();
	}

	protected void generateGatherBinPartsTarget() {
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, null, null, null, null);
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			String placeToGather = getLocation(plugin);
			printCustomGatherCall(ModelBuildScriptGenerator.getNormalizedName(plugin), Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS), null);
		}

		Set featureSet = new HashSet();
		for (int i = 0; i < features.length; i++) {
			BuildTimeFeature feature = features[i];
			String placeToGather = feature.getRootLocation();
			String featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
			printCustomGatherCall(featureFullName, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), '/' + DEFAULT_FEATURE_LOCATION);
			featureSet.add(feature);
		}

		//This will generate gather.bin.parts call to features that provides files for the root
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			BuildTimeFeature feature = (BuildTimeFeature) iter.next();
			if (featureSet.contains(feature))
				continue;
			String placeToGather = feature.getRootLocation();
			String featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
			printCustomGatherCall(featureFullName, Utils.makeRelative(new Path(placeToGather), new Path(workingDirectory)).toOSString(), PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), '/' + DEFAULT_FEATURE_LOCATION);
		}
		script.printTargetEnd();
		script.println();
	}

	private void generateSignJarCall(String name, String version, byte type) {
		if (!signJars)
			return;
		Map properties = new HashMap(2);
		properties.put(PROPERTY_SOURCE, type == BUNDLE_TYPE ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES));
		properties.put(PROPERTY_ELEMENT_NAME, name + '_' + version);
		script.printAntCallTask(TARGET_JARSIGNING, true, properties);
	}

	//generate the appropriate postProcessingCall
	private void generatePostProcessingSteps(String name, String version, String style, byte type) {
		if (ShapeAdvisor.FOLDER.equalsIgnoreCase(style))
			return;
		if (ShapeAdvisor.FILE.equalsIgnoreCase(style)) {
			generateJarUpCall(name, version, type);
			generateSignJarCall(name, version, type);
			generateJNLPCall(name, version, type);
			return;
		}
	}

	private void generateJNLPCall(String name, String version, byte type) {
		if (generateJnlp == false)
			return;
		if (type != FEATURE_TYPE)
			return;

		String dir = type == BUNDLE_TYPE ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES);
		String location = dir + '/' + name + '_' + version + ".jar"; //$NON-NLS-1$
		script.println("<eclipse.jnlpGenerator feature=\"" + AntScript.getEscaped(location) + "\"  codebase=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_CODEBASE) + "\" j2se=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_J2SE) + "\" locale=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_LOCALE) + "\" generateOfflineAllowed=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_GENOFFLINE) + "\" configInfo=\"" + Utils.getPropertyFormat(PROPERTY_JNLP_CONFIGS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	}

	private void generateJarUpCall(String name, String version, byte type) {
		Map properties = new HashMap(2);
		properties.put(PROPERTY_SOURCE, type == BUNDLE_TYPE ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS) : Utils.getPropertyFormat(PROPERTY_ECLIPSE_FEATURES));
		properties.put(PROPERTY_ELEMENT_NAME, name + '_' + version);
		script.printAntCallTask(TARGET_JARUP, true, properties);
	}

	private void generateEpilogue() {
		generateGatherBinPartsTarget();
		if (embeddedSource)
			generateGatherSourceTarget();
		generatePostProcessingTarget();
		generateArchivingTarget(true);
		generateCleanupAssembly(true);
		if (FORMAT_TAR.equalsIgnoreCase(archiveFormat))
			generateGZipTarget(true);

		generateCustomAssemblyTarget();
		generateMetadataTarget();
		generateDirectorTarget(true);
		if (BuildDirector.p2Gathering) {
			generateMirrorTask(true);
			generateMirrorProductTask();
		}

		script.printProjectEnd();
		script.close();
		script = null;
	}

	public String getTargetName() {
		String config = getTargetConfig();
		return DEFAULT_ASSEMBLE_NAME + '.' + getTargetElement() + (config.length() > 0 ? "." : "") + config; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getTargetConfig() {
		return (configInfo.equals(Config.genericConfig()) ? "" : configInfo.toStringReplacingAny(".", ANY_STRING)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getTargetElement() {
		return (featureId.equals("") ? "" : featureId); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void printCustomAssemblyAntCall(String customTarget, Map properties) {
		Map params = (properties != null) ? new HashMap(properties) : new HashMap(1);
		params.put(PROPERTY_CUSTOM_TARGET, customTarget);
		script.printAntCallTask(TARGET_CUSTOM_ASSEMBLY, true, params);
	}

	protected void generateCustomAssemblyTarget() {
		script.printTargetDeclaration(TARGET_CUSTOM_ASSEMBLY, null, PROPERTY_CUSTOM_ASSEMBLY, null, null);
		script.printAntTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_ASSEMBLY), null, Utils.getPropertyFormat(PROPERTY_CUSTOM_TARGET), null, TRUE, null);
		script.printTargetEnd();
		script.println();
	}

	private void generateMetadataTarget() {
		if (haveP2Bundles()) {
			script.printTargetDeclaration(TARGET_P2_METADATA, null, TARGET_P2_METADATA, null, null);
			script.printProperty(PROPERTY_P2_APPEND, "true"); //$NON-NLS-1$
			script.printProperty(PROPERTY_P2_COMPRESS, "false"); //$NON-NLS-1$
			script.printProperty(PROPERTY_P2_METADATA_REPO_NAME, ""); //$NON-NLS-1$
			script.printProperty(PROPERTY_P2_ARTIFACT_REPO_NAME, ""); //$NON-NLS-1$

			if (havePDEUIState()) {
				//during feature export we need to override the "mode"
				printP2GenerationModeCondition();
			}
			if (!BuildDirector.p2Gathering) {
				script.printTab();
				script.print("<p2.generator "); //$NON-NLS-1$
				script.printAttribute("source", Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), true); //$NON-NLS-1$
				script.printAttribute("append", Utils.getPropertyFormat(PROPERTY_P2_APPEND), true); //$NON-NLS-1$
				script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
				script.printAttribute("compress", Utils.getPropertyFormat(PROPERTY_P2_COMPRESS), true); //$NON-NLS-1$ 
				script.printAttribute("metadataRepository", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO), true); //$NON-NLS-1$ 
				script.printAttribute("artifactRepository", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO), true); //$NON-NLS-1$ 
				script.printAttribute("metadataRepositoryName", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO_NAME), true); //$NON-NLS-1$
				script.printAttribute("artifactRepositoryName", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO_NAME), true); //$NON-NLS-1$
				script.printAttribute("publishArtifacts", Utils.getPropertyFormat(PROPERTY_P2_PUBLISH_ARTIFACTS), true); //$NON-NLS-1$
				script.printAttribute("site", Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_SITE), true); //$NON-NLS-1$
				script.printAttribute("siteVersion", Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_VERSION), true); //$NON-NLS-1$
				script.printAttribute("p2OS", configInfo.getOs(), true); //$NON-NLS-1$
				if (!havePDEUIState() || rootFileProviders.size() > 0)
					script.printAttribute("mode", "incremental", true); //$NON-NLS-1$ //$NON-NLS-2$
				else
					script.printAttribute("mode", Utils.getPropertyFormat(PROPERTY_P2_GENERATION_MODE), true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			if (rootFileProviders.size() > 0) {
				if (productFile != null) {
					script.println();
					File modFile = productFile.getLocation();
					String modLocation = Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + DEFAULT_FEATURE_LOCATION + '/' + CONTAINER_FEATURE + "/product/" + modFile.getName(); //$NON-NLS-1$
					script.printAvailableTask(PROPERTY_P2_PRODUCT_MOD, modLocation, modLocation);
					script.printProperty(PROPERTY_P2_PRODUCT_MOD, productFile.getLocation().getPath());
				}
				script.printTab();
				script.print("<p2.generator "); //$NON-NLS-1$
				script.printAttribute("config", rootFolder, true); //$NON-NLS-1$
				script.printAttribute("append", Utils.getPropertyFormat(PROPERTY_P2_APPEND), true); //$NON-NLS-1$ 
				script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
				script.printAttribute("compress", Utils.getPropertyFormat(PROPERTY_P2_COMPRESS), true); //$NON-NLS-1$ 
				script.printAttribute("metadataRepository", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO), true); //$NON-NLS-1$ 
				script.printAttribute("artifactRepository", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO), true); //$NON-NLS-1$ 
				script.printAttribute("metadataRepositoryName", Utils.getPropertyFormat(PROPERTY_P2_METADATA_REPO_NAME), true); //$NON-NLS-1$
				script.printAttribute("artifactRepositoryName", Utils.getPropertyFormat(PROPERTY_P2_ARTIFACT_REPO_NAME), true); //$NON-NLS-1$
				script.printAttribute("launcherConfig", configInfo.toString(), true); //$NON-NLS-1$
				script.printAttribute("p2OS", configInfo.getOs(), true); //$NON-NLS-1$
				script.printAttribute("publishArtifacts", Utils.getPropertyFormat(PROPERTY_P2_PUBLISH_ARTIFACTS), true); //$NON-NLS-1$ 
				if (!havePDEUIState())
					script.printAttribute("mode", "incremental", true); //$NON-NLS-1$ //$NON-NLS-2$
				else
					script.printAttribute("mode", Utils.getPropertyFormat(PROPERTY_P2_GENERATION_MODE), true); //$NON-NLS-1$
				if (productFile != null) {
					script.printAttribute("exe", rootFolder + '/' + Utils.getPropertyFormat(PROPERTY_LAUNCHER_NAME), true); //$NON-NLS-1$
					script.printAttribute("productFile", Utils.getPropertyFormat(PROPERTY_P2_PRODUCT_MOD), true); //$NON-NLS-1$
				} else {
					script.printAttribute("root", Utils.getPropertyFormat(PROPERTY_P2_ROOT_NAME), true); //$NON-NLS-1$
					script.printAttribute("rootVersion", Utils.getPropertyFormat(PROPERTY_P2_ROOT_VERSION), true); //$NON-NLS-1$
				}
				script.println("/>"); //$NON-NLS-1$
			}

			script.printTargetEnd();
			script.println();
		}
	}

	protected void printP2GenerationModeCondition() {
		// "final" if not running packager and we are overriding, else "incremental"
		script.print("<condition"); //$NON-NLS-1$
		script.printAttribute("property", PROPERTY_P2_GENERATION_MODE, true); //$NON-NLS-1$
		script.printAttribute("value", "final", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.printAttribute("else", "incremental", false); //$NON-NLS-1$ //$NON-NLS-2$
		script.println(">"); //$NON-NLS-1$
		script.println("\t<and>"); //$NON-NLS-1$
		script.println("\t\t<not>"); //$NON-NLS-1$
		script.println("\t\t\t<isset property=\"runPackager\"/>"); //$NON-NLS-1$
		script.println("\t\t</not>"); //$NON-NLS-1$
		script.println("\t\t<isset property=\"" + PROPERTY_P2_FINAL_MODE_OVERRIDE + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("\t</and>"); //$NON-NLS-1$
		script.printEndTag("condition"); //$NON-NLS-1$
	}

	public boolean haveP2Bundles() {
		if (p2Bundles != null)
			return p2Bundles.booleanValue();

		p2Bundles = Boolean.valueOf(loadP2Class());
		return p2Bundles.booleanValue();
	}

	private void generateZipTarget() {
		final int parameterSize = 15;
		List parameters = new ArrayList(parameterSize + 1);

		if (BuildDirector.p2Gathering) {
			parameters.add(Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX));
			createZipExecCommand(parameters);
			return;
		}

		for (int i = 0; i < plugins.length; i++) {
			parameters.add(Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shapeAdvisor.getFinalShape(plugins[i])[0]);
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
			parameters.add(Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shapeAdvisor.getFinalShape(features[i])[0]);
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
		String tarArgs = assembling ? "-cvf '" : "-rvf '"; //$NON-NLS-1$//$NON-NLS-2$
		parameters.add(Utils.getPropertyFormat(PROPERTY_TAR_ARGS) + tarArgs + Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH) + "' " + ((BuildDirector.p2Gathering && productFile == null) ? "." : Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX)) + ' '); //$NON-NLS-1$ //$NON-NLS-2$
		String folder = (BuildDirector.p2Gathering && productFile == null) ? Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) : Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP);
		script.printExecTask("tar", folder, parameters, null); //$NON-NLS-1$ 

		script.printAntCallTask(TARGET_GZIP_RESULTS, true, null);

		List args = new ArrayList(2);
		args.add("-rf"); //$NON-NLS-1$
		args.add('\'' + Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + '\'');
		script.printExecTask("rm", null, args, null); //$NON-NLS-1$
	}

	//TODO this code and the generateAntTarTarget() should be refactored using a factory or something like that.
	protected void generateAntZipTarget() {
		List fileSets = new ArrayList();

		if (BuildDirector.p2Gathering) {
			//TODO permissions
			FileSet[] permissions = generatePermissions(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), true);
			String toExcludeFromArchive = Utils.getStringFromCollection(this.addedByPermissions, ","); //$NON-NLS-1$
			fileSets.add(new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), false, null, "**/**", null, toExcludeFromArchive, null, productFile != null ? Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) : null, null, null)); //$NON-NLS-1$
			fileSets.addAll(Arrays.asList(permissions));
		} else {
			for (int i = 0; i < plugins.length; i++) {
				Object[] shape = shapeAdvisor.getFinalShape(plugins[i]);
				fileSets.add(new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1] == ShapeAdvisor.FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null));
			}

			for (int i = 0; i < features.length; i++) {
				Object[] shape = shapeAdvisor.getFinalShape(features[i]);
				fileSets.add(new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1] == ShapeAdvisor.FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null));
			}

			if (rootFileProviders.size() > 0) {
				if (groupConfigs) {
					List allConfigs = getConfigInfos();
					for (Iterator iter = allConfigs.iterator(); iter.hasNext();) {
						Config elt = (Config) iter.next();
						fileSets.add(new ZipFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + elt.toStringReplacingAny(".", ANY_STRING), false, null, "**/**", null, null, null, elt.toStringReplacingAny(".", ANY_STRING), null, null)); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					}
				} else {
					FileSet[] permissions = generatePermissions(rootFolder, true);
					String toExcludeFromArchive = Utils.getStringFromCollection(this.addedByPermissions, ","); //$NON-NLS-1$
					fileSets.add(new ZipFileSet(rootFolder, false, null, "**/**", null, toExcludeFromArchive, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, null)); //$NON-NLS-1$
					fileSets.addAll(Arrays.asList(permissions));
				}
			}
		}
		if (fileSets.size() > 0) {
			FileSet[] sets = (FileSet[]) fileSets.toArray(new FileSet[fileSets.size()]);
			script.printZipTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, sets);
		}
	}

	protected Collection getArchiveRootFileProviders() {
		return rootFileProviders != null ? rootFileProviders : Collections.EMPTY_LIST;
	}

	protected FileSet[] generatePermissions(String root, boolean zip) {
		String configInfix = configInfo.toString("."); //$NON-NLS-1$
		String prefixPermissions = ROOT_PREFIX + configInfix + '.' + PERMISSIONS + '.';
		String commonPermissions = ROOT_PREFIX + PERMISSIONS + '.';
		ArrayList fileSets = new ArrayList();

		for (Iterator iter = getArchiveRootFileProviders().iterator(); iter.hasNext();) {
			Properties featureProperties = getFeatureBuildProperties((BuildTimeFeature) iter.next());
			for (Iterator iter2 = featureProperties.entrySet().iterator(); iter2.hasNext();) {
				Map.Entry permission = (Map.Entry) iter2.next();
				String instruction = (String) permission.getKey();
				String parameters = (String) permission.getValue();
				String[] values = Utils.getArrayFromString(parameters);
				for (int i = 0; i < values.length; i++) {
					boolean isFile = !values[i].endsWith("/"); //$NON-NLS-1$
					if (instruction.startsWith(prefixPermissions)) {
						addedByPermissions.add(values[i]);
						if (zip)
							fileSets.add(new ZipFileSet(root + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(prefixPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						else
							fileSets.add(new TarFileSet(root + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(prefixPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						continue;
					}
					if (instruction.startsWith(commonPermissions)) {
						addedByPermissions.add(values[i]);
						if (zip)
							fileSets.add(new ZipFileSet(root + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(commonPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						else
							fileSets.add(new TarFileSet(root + (isFile ? '/' + values[i] : ""), isFile, null, isFile ? null : values[i] + "/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) + (isFile ? '/' + values[i] : ""), null, instruction.substring(commonPermissions.length()))); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						continue;
					}
				}
			}
		}
		return (FileSet[]) fileSets.toArray(new FileSet[fileSets.size()]);
	}

	//TODO this code andn the generateAntZipTarget() should be refactored using a factory or something like that.
	private void generateAntTarTarget() {
		List fileSets = new ArrayList();

		if (BuildDirector.p2Gathering) {
			FileSet[] permissions = generatePermissions(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), false);
			String toExcludeFromArchive = Utils.getStringFromCollection(this.addedByPermissions, ","); //$NON-NLS-1$
			fileSets.add(new TarFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), false, null, "**/**", null, toExcludeFromArchive, null, productFile != null ? Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX) : null, null, null)); //$NON-NLS-1$
			fileSets.addAll(Arrays.asList(permissions));
		} else {
			//FileSet[] filesPlugins = new FileSet[plugins.length];
			for (int i = 0; i < plugins.length; i++) {
				Object[] shape = shapeAdvisor.getFinalShape(plugins[i]);
				fileSets.add(new TarFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_PLUGIN_LOCATION + '/' + (String) shape[0], shape[1] == ShapeAdvisor.FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_PLUGIN_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null));
			}

			for (int i = 0; i < features.length; i++) {
				Object[] shape = shapeAdvisor.getFinalShape(features[i]);
				fileSets.add(new TarFileSet(Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION + '/' + (String) shape[0], shape[1] == ShapeAdvisor.FILE, null, null, null, null, null, Utils.getPropertyFormat(PROPERTY_FEATURE_ARCHIVE_PREFIX) + '/' + (String) shape[0], null, null));
			}

			if (rootFileProviders.size() > 0) {
				FileSet[] permissionSets = generatePermissions(rootFolder, false);
				fileSets.add(new TarFileSet(rootFolder, false, null, "**/**", null, null, null, Utils.getPropertyFormat(PROPERTY_ARCHIVE_PREFIX), null, null)); //$NON-NLS-1$
				fileSets.add(Arrays.asList(permissionSets));
			}
		}
		if (fileSets.size() > 0) {
			FileSet[] sets = (FileSet[]) fileSets.toArray(new FileSet[fileSets.size()]);
			script.printTarTask(Utils.getPropertyFormat(PROPERTY_ARCHIVE_FULLPATH), null, false, true, sets);
		}
	}

	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}

	public void setSignJars(boolean value) {
		signJars = value;
	}

	public boolean isSigning() {
		return signJars;
	}

	public void setProduct(String value) {
		product = value;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public ProductFile getProductFile() {
		if (productFile == null && product != null) {
			try {
				// Note that we must pass the OS information in to load product
				// so that any icon files can be calculated for the 
				// generateBrandingCalls.
				productFile = loadProduct(product, configInfo != null ? configInfo.getOs() : null);
			} catch (CoreException e) {
				//ignore
			}
		}
		return productFile;
	}

	public void setArchiveFormat(String archiveFormat) {
		this.archiveFormat = archiveFormat;
	}

	public void setGroupConfigs(boolean group) {
		groupConfigs = group;
	}
}
