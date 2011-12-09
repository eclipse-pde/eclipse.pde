/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Compuware Corporation - Sebastien Angers <sebastien.angers@compuware.com> 
 *     		- Enabled additional mirror slicingOptions in Headless PDE Build
 *     		- Enabled 'raw' attribute for mirror step in Headless PDE Build
 *     		- https://bugs.eclipse.org/338878
 *******************************************************************************/

package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.osgi.framework.Version;

public class P2ConfigScriptGenerator extends AssembleConfigScriptGenerator {
	private static final VersionRange OLD_EXECUTABLE_RANGE = new VersionRange(Version.emptyVersion, true, new Version(3, 3, 200, "v20090306-1900"), false); //$NON-NLS-1$
	private AssemblyInformation assemblyInformation = null;
	private boolean assembling = false;
	private boolean versionsList = false;

	public P2ConfigScriptGenerator(AssemblyInformation assemblyInformation, boolean assembling) {
		this.assemblyInformation = assemblyInformation;
		this.assembling = assembling;
	}

	public void initialize(String directoryName, String feature) {
		this.directory = directoryName;
		this.featureId = feature;
	}

	public void generate() {
		initializeCollections();

		try {
			String prefix = assembling ? "assemble." : "package."; //$NON-NLS-1$ //$NON-NLS-2$
			openScript(directory, prefix + featureId + ".p2.xml"); //$NON-NLS-1$
		} catch (CoreException e) {
			return;
		}
		generatePrologue();
		generateMainBegin();
		generateGatherCalls();
		generateMetadataCalls();
		generateMainEnd();

		generateGatherBinPartsTarget();
		generateCustomAssemblyTarget();
		generateSigningTarget();
		generateMetadataTarget();
		generateEpilogue();
		closeScript();
	}

	protected void initializeCollections() {
		Collection p = new LinkedHashSet();
		Collection f = new LinkedHashSet();
		Collection r = new LinkedHashSet();
		for (Iterator iterator = getConfigInfos().iterator(); iterator.hasNext();) {
			Config config = (Config) iterator.next();
			p.addAll(assemblyInformation.getPlugins(config));
			f.addAll(assemblyInformation.getFeatures(config));
			r.addAll(assemblyInformation.getRootFileProviders(config));
		}

		this.plugins = (BundleDescription[]) p.toArray(new BundleDescription[p.size()]);
		this.features = (BuildTimeFeature[]) f.toArray(new BuildTimeFeature[f.size()]);
		this.rootFileProviders = r;
	}

	protected void generatePrologue() {
		script.printProjectDeclaration("Publish p2 metadata", TARGET_MAIN, null); //$NON-NLS-1$
		ProductFile product = getProductFile();
		if (product != null) {
			String launcherName = product.getLauncherName() != null ? product.getLauncherName() : "eclipse"; //$NON-NLS-1$
			script.printProperty(PROPERTY_LAUNCHER_NAME, launcherName);
			script.printProperty(PROPERTY_LAUNCHER_PROVIDER, FEATURE_EQUINOX_EXECUTABLE);
		}
		script.printProperty(PROPERTY_P2_BUILD_REPO, "file:" + Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/buildRepo"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printProperty(PROPERTY_ASSEMBLY_TMP, Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + "/tmp"); //$NON-NLS-1$
		script.printProperty(PROPERTY_SIGN, (signJars ? Boolean.TRUE : Boolean.FALSE).toString());
		script.printAvailableTask(PROPERTY_CUSTOM_ASSEMBLY, "${builder}/customAssembly.xml", "${builder}/customAssembly.xml"); //$NON-NLS-1$ //$NON-NLS-2$

		if (productQualifier != null)
			script.printProperty(PROPERTY_P2_PRODUCT_QUALIFIER, productQualifier);

		script.printProperty(PROPERTY_P2_MIRROR_RAW, FALSE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_FILTER, ""); //$NON-NLS-1$
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_FOLLOW_ONLY_FILTERED_REQS, FALSE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_FOLLOW_STRICT, FALSE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_INCLUDE_FEATURES, TRUE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_INCLUDE_NON_GREEDY, FALSE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_INCLUDE_OPTIONAL, havePDEUIState() ? FALSE : TRUE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_LATEST_VERSION_ONLY, FALSE);
		script.printProperty(PROPERTY_P2_MIRROR_SLICING_PLATFORM_FILTER, ""); //$NON-NLS-1$

		script.println();
		generateCustomGatherMacro();
	}

	protected void generateMainEnd() {
		script.printTargetEnd();
		script.println();
	}

	protected void generateMetadataCalls() {
		script.printAntCallTask(TARGET_P2_METADATA, true, null);
	}

	protected void generateGatherCalls() {
		super.generateGatherCalls();

		if (signJars)
			script.printAntCallTask(TARGET_P2_SIGN_REPO, true, null);
		script.println();
	}

	protected void generateBrandingCalls() {
		ProductFile product = getProductFile();
		if (product != null) {
			List configs = getConfigInfos();
			for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
				Config config = (Config) iterator.next();
				if (Config.genericConfig().equals(config))
					continue;
				script.printTab();
				script.print("<eclipse.brand.p2.artifacts "); //$NON-NLS-1$
				script.printAttribute("launcherName", Utils.getPropertyFormat(PROPERTY_LAUNCHER_NAME), true); //$NON-NLS-1$
				script.printAttribute("config", config.toString("."), true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("iconsList", computeIconsList(config.getOs()), true); //$NON-NLS-1$
				script.printAttribute("launcherProvider", Utils.getPropertyFormat(PROPERTY_LAUNCHER_PROVIDER), true); //$NON-NLS-1$
				script.printAttribute("productId", product.getId(), true); //$NON-NLS-1$
				script.printAttribute("productVersion", getReplacedProductVersion(), true); //$NON-NLS-1$
				script.printAttribute("repository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
				script.printAttribute("tempDirectory", Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP), true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
		}
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_ASSEMBLY_TMP) + "/p2.branding", null, null); //$NON-NLS-1$
		script.println();
	}

	protected void generateSigningTarget() {
		script.printTargetDeclaration(TARGET_P2_SIGN_REPO, null, null, null, null);
		if (signJars && plugins.length + features.length > 0) {
			script.printTab();
			script.print("<p2.process.artifacts"); //$NON-NLS-1$
			script.printAttribute("repositoryPath", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$
			script.printTab();
			script.print("\t<sign"); //$NON-NLS-1$
			script.printAttribute("keystore", Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE), true); //$NON-NLS-1$
			script.printAttribute("storepass", Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS), true); //$NON-NLS-1$
			script.printAttribute("keypass", Utils.getPropertyFormat(PROPERTY_SIGN_KEYPASS), true); //$NON-NLS-1$
			script.printAttribute("alias", Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS), true); //$NON-NLS-1$
			script.printAttribute("unsign", Utils.getPropertyFormat(PROPERTY_UNSIGN), true); //$NON-NLS-1$
			script.print(" />\n"); //$NON-NLS-1$
			for (int i = 0; i < plugins.length; i++) {
				script.printTab();
				script.print("\t<plugin"); //$NON-NLS-1$
				script.printAttribute("id", plugins[i].getSymbolicName(), true); //$NON-NLS-1$
				script.printAttribute("version", plugins[i].getVersion().toString(), true); //$NON-NLS-1$
				script.print(" /> \n"); //$NON-NLS-1$
			}
			for (int i = 0; i < features.length; i++) {
				script.printTab();
				script.print("\t<feature"); //$NON-NLS-1$
				script.printAttribute("id", features[i].getId(), true); //$NON-NLS-1$
				script.printAttribute("version", features[i].getVersion(), true); //$NON-NLS-1$
				script.print(" />\n"); //$NON-NLS-1$
			}
			script.println("</p2.process.artifacts>"); //$NON-NLS-1$
		}
		script.printTargetEnd();
		script.println();
	}

	protected void generateMetadataTarget() {
		script.printTargetDeclaration(TARGET_P2_METADATA, null, null, null, null);
		script.printProperty(PROPERTY_P2_FLAVOR, "tooling"); //$NON-NLS-1$
		generateBrandingCalls();

		ProductFile product = getProductFile();
		if (product != null) {

			String productDir = getWorkingDirectory() + '/' + DEFAULT_FEATURE_LOCATION + '/' + CONTAINER_FEATURE + "/product"; //$NON-NLS-1$
			File productFile = product.getLocation();
			String newProduct = new File(productDir, productFile.getName()).getAbsolutePath();
			script.printCopyFileTask(productFile.getPath(), newProduct, true);

			if (!generateProductP2Inf(productFile, productDir)) {
				//if we didn't generate the file, copy over the provided one
				File parent = productFile.getParentFile();
				File p2Inf = new File(parent, "p2.inf"); //$NON-NLS-1$
				if (p2Inf.exists())
					script.printCopyTask(p2Inf.getAbsolutePath(), productDir, null, false, true);
			}

			script.printTab();
			script.print("<replace "); //$NON-NLS-1$
			script.printAttribute("file", new File(productDir, "p2.inf").getAbsolutePath(), true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("token", "@FLAVOR@", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.printAttribute("value", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
			generateCopyConfigs(product, productDir);
			generateProductReplaceTask(product, newProduct, assemblyInformation);

			script.printTab();
			script.print("<p2.publish.product"); //$NON-NLS-1$
			script.printAttribute("flavor", Utils.getPropertyFormat(PROPERTY_P2_FLAVOR), true); //$NON-NLS-1$
			script.printAttribute("repository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$ 
			script.printAttribute("productFile", newProduct, true); //$NON-NLS-1$
			script.println(">"); //$NON-NLS-1$

			URI[] metadata = getContextMetadata();
			URI[] artifacts = getContextArtifacts();
			for (int i = 0; metadata != null && i < metadata.length; i++) {
				script.printTab();
				script.print("\t<contextRepository"); //$NON-NLS-1$
				script.printAttribute("location", URIUtil.toUnencodedString(metadata[i]), true); //$NON-NLS-1$
				script.printAttribute("metadata", TRUE, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			for (int i = 0; artifacts != null && i < artifacts.length; i++) {
				script.printTab();
				script.print("\t<contextRepository"); //$NON-NLS-1$
				script.printAttribute("location", URIUtil.toUnencodedString(artifacts[i]), true); //$NON-NLS-1$
				script.printAttribute("artifact", TRUE, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}

			for (Iterator iterator = getConfigInfos().iterator(); iterator.hasNext();) {
				Config config = (Config) iterator.next();
				if (Config.genericConfig().equals(config))
					continue;

				script.printTab();
				script.print("\t<config"); //$NON-NLS-1$
				script.printAttribute("os", config.getOs(), true); //$NON-NLS-1$
				script.printAttribute("ws", config.getWs(), true); //$NON-NLS-1$
				script.printAttribute("arch", config.getArch(), true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			if (versionsList) {
				script.printTab();
				script.print("\t<advice"); //$NON-NLS-1$
				script.printAttribute("kind", "featureVersions", true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("file", getWorkingDirectory() + '/' + DEFAULT_FEATURE_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
				script.printTab();
				script.print("\t<advice"); //$NON-NLS-1$
				script.printAttribute("kind", "pluginVersions", true); //$NON-NLS-1$ //$NON-NLS-2$
				script.printAttribute("file", getWorkingDirectory() + '/' + DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX + PROPERTIES_FILE_SUFFIX, true); //$NON-NLS-1$
				script.println("/>"); //$NON-NLS-1$
			}
			script.println("</p2.publish.product>"); //$NON-NLS-1$
		}

		script.println();
		generateSynchContext();
		script.printTargetEnd();
	}

	protected void generateCopyConfigs(ProductFile product, String productDir) {
		if (!product.haveCustomConfig())
			return;
		for (Iterator iterator = getConfigInfos().iterator(); iterator.hasNext();) {
			Config config = (Config) iterator.next();
			String entry = product.getConfigIniPath(config.getOs());
			if (entry == null)
				continue;
			File entryFile = new File(entry);
			if (entryFile.exists() && entryFile.isAbsolute())
				continue;
			String path = findConfigFile(product, config.getOs());
			if (path != null) {
				//non-null path exists, but isn't necessarily absolute
				File configFile = new File(path);
				script.printCopyFileTask(configFile.getAbsolutePath(), productDir + '/' + entry, true);
			}
		}
	}

	protected void generateSynchContext() {
		ProductFile product = getProductFile();
		ArrayList binaryFeatures = null;
		if (product == null) {
			binaryFeatures = new ArrayList();
			for (int i = 0; i < features.length; i++) {
				BuildTimeFeature feature = features[i];
				if (feature.isBinary())
					binaryFeatures.add(feature);
			}
		}

		if (product == null && (binaryFeatures == null || binaryFeatures.size() == 0))
			return;

		Map args = new HashMap();
		// note that if the raw attribute (p2.mirror.raw) has not been set in the build.properties, then the default was set in #generatePrologue()
		args.put("raw", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_RAW)); //$NON-NLS-1$
		script.printStartTag("p2.mirror", args); //$NON-NLS-1$

		script.printTab();
		// note that if a slicingOption has not been set in the build.properties, then the default was set in #generatePrologue()
		script.print("\t<slicingOptions"); //$NON-NLS-1$
		script.printAttribute("includeNonGreedy", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_INCLUDE_NON_GREEDY), true); //$NON-NLS-1$
		script.printAttribute("filter", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_FILTER), true); //$NON-NLS-1$
		script.printAttribute("followOnlyFilteredRequirements", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_FOLLOW_ONLY_FILTERED_REQS), true); //$NON-NLS-1$
		script.printAttribute("followStrict", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_FOLLOW_STRICT), true); //$NON-NLS-1$
		script.printAttribute("includeFeatures", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_INCLUDE_FEATURES), true); //$NON-NLS-1$
		script.printAttribute("includeOptional", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_INCLUDE_OPTIONAL), true); //$NON-NLS-1$
		script.printAttribute("latestVersionOnly", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_LATEST_VERSION_ONLY), true); //$NON-NLS-1$
		script.printAttribute("platformFilter", Utils.getPropertyFormat(PROPERTY_P2_MIRROR_SLICING_PLATFORM_FILTER), true); //$NON-NLS-1$			
		script.println("/>"); //$NON-NLS-1$

		script.printTab();
		script.print("\t<source"); //$NON-NLS-1$
		script.printAttribute("location", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$

		URI[] context = getContextMetadata();
		for (int i = 0; context != null && i < context.length; i++) {
			script.printTab();
			script.print("\t<source"); //$NON-NLS-1$
			script.printAttribute("location", URIUtil.toUnencodedString(context[i]), true); //$NON-NLS-1$
			script.printAttribute("optional", TRUE, true); //$NON-NLS-1$
			script.printAttribute("kind", "metadata", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("/>"); //$NON-NLS-1$
		}
		URI[] artifacts = getContextArtifacts();
		for (int i = 0; artifacts != null && i < artifacts.length; i++) {
			script.printTab();
			script.print("\t<source"); //$NON-NLS-1$
			script.printAttribute("location", URIUtil.toUnencodedString(artifacts[i]), true); //$NON-NLS-1$
			script.printAttribute("optional", TRUE, true); //$NON-NLS-1$
			script.printAttribute("kind", "artifact", true); //$NON-NLS-1$ //$NON-NLS-2$
			script.println("/>"); //$NON-NLS-1$
		}

		script.printTab();
		script.print("\t<destination "); //$NON-NLS-1$
		script.printAttribute("location", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$ 
		script.printAttribute("kind", "metadata", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("/>"); //$NON-NLS-1$
		script.print("\t<destination "); //$NON-NLS-1$
		script.printAttribute("location", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$ 
		script.printAttribute("kind", "artifact", true); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("/>"); //$NON-NLS-1$
		script.printTab();

		if (product != null) {
			String version = product.getVersion();
			if (version.endsWith(PROPERTY_QUALIFIER)) {
				Version oldVersion = new Version(version);
				version = oldVersion.getMajor() + "." + oldVersion.getMinor() + "." + oldVersion.getMicro() + "." + Utils.getPropertyFormat(PROPERTY_P2_PRODUCT_QUALIFIER); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			script.print("\t<iu"); //$NON-NLS-1$
			script.printAttribute(ID, product.getId(), true);
			script.printAttribute(VERSION, version, true);
			script.println("/>"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < features.length; i++) {
				BuildTimeFeature feature = features[i];
				if (feature.isBinary()) {
					binaryFeatures.add(feature);
					script.print("\t<iu"); //$NON-NLS-1$
					script.printAttribute(ID, getFeatureGroupId(feature), true);
					script.printAttribute(VERSION, feature.getVersion(), true);
					script.println("/>"); //$NON-NLS-1$
				}
			}
		}
		script.printEndTag("p2.mirror"); //$NON-NLS-1$
	}

	private boolean generateProductP2Inf(File productFile, String root) {
		ProductGenerator generator = new ProductGenerator();
		generator.setProduct(productFile.getAbsolutePath());
		generator.setBuildSiteFactory(siteFactory);
		generator.setRoot(root);
		generator.setWorkingDirectory(getWorkingDirectory());
		generator.setAssemblyInfo(assemblyInformation);
		generator.setFeatureId(featureId);
		try {
			return generator.generateP2Info();
		} catch (CoreException e) {
			//problem with the .product file
			return false;
		}
	}

	protected void generateEpilogue() {
		script.printProjectEnd();
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
		script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, "@{dir}", TARGET_PUBLISH_BIN_PARTS, null, null, params); //$NON-NLS-1$

		params.put(PROPERTY_PROJECT_LOCATION, "${basedir}/@{dir}"); //$NON-NLS-1$
		params.put(PROPERTY_PROJECT_NAME, "@{projectName}"); //$NON-NLS-1$
		params.put(PROPERTY_TARGET_FOLDER, "@{propertyValue}@{subFolder}"); //$NON-NLS-1$
		printCustomAssemblyAntCall(TARGET_GATHER_BIN_PARTS, params);

		script.printEndMacroDef();
		script.println();
	}

	protected void generateGatherBinPartsTarget() {
		BuildTimeFeature oldExecutableFeature = null;
		ArrayList binaryFeatures = new ArrayList();
		ArrayList binaryBundles = new ArrayList();
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, null, null, null, null);
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription plugin = plugins[i];
			Path pluginLocation = new Path(plugin.getLocation());
			if (Utils.isBinary(plugin))
				binaryBundles.add(new FileSet(pluginLocation.removeLastSegments(1).toOSString(), null, pluginLocation.lastSegment(), null, null, null, null));
			else
				printCustomGatherCall(ModelBuildScriptGenerator.getNormalizedName(plugin), Utils.makeRelative(pluginLocation, new Path(workingDirectory)).toOSString(), PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_PLUGINS), null);
		}

		Set featureSet = BuildDirector.p2Gathering ? new HashSet() : null;
		for (int i = 0; i < features.length; i++) {
			BuildTimeFeature feature = features[i];
			IPath featureLocation = new Path(feature.getRootLocation());
			if (feature.isBinary()) {
				binaryFeatures.add(new FileSet(featureLocation.removeLastSegments(1).toOSString(), null, featureLocation.lastSegment(), null, null, null, null));
			} else {
				String featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
				printCustomGatherCall(featureFullName, Utils.makeRelative(featureLocation, new Path(workingDirectory)).toOSString(), PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), '/' + DEFAULT_FEATURE_LOCATION);
				featureSet.add(feature);
			}
		}

		//This will generate gather.bin.parts call to features that provides files for the root
		for (Iterator iter = rootFileProviders.iterator(); iter.hasNext();) {
			BuildTimeFeature feature = (BuildTimeFeature) iter.next();
			if (featureSet.contains(feature))
				continue;
			if (isOldExecutableFeature(feature)) {
				oldExecutableFeature = feature;
				script.printAntCallTask(TARGET_P2_COMPATIBILITY_GATHER_EXECUTABLE, true, null);
			} else {
				IPath featureLocation = new Path(feature.getRootLocation());
				String featureFullName = feature.getId() + "_" + feature.getVersion(); //$NON-NLS-1$
				printCustomGatherCall(featureFullName, Utils.makeRelative(featureLocation, new Path(workingDirectory)).toOSString(), PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE), '/' + DEFAULT_FEATURE_LOCATION);
			}
		}

		String repo = Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO);
		URI[] context = getContextMetadata();
		script.printP2PublishFeaturesAndBundles(repo, repo, (FileSet[]) binaryBundles.toArray(new FileSet[binaryBundles.size()]), (FileSet[]) binaryFeatures.toArray(new FileSet[binaryFeatures.size()]), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_SITE), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_PREFIX), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_DEFINITION), Utils.getPropertyFormat(PROPERTY_P2_CATEGORY_VERSION), context);

		script.printTargetEnd();
		script.println();

		if (oldExecutableFeature != null) {
			generateCompatibilityGatherExecutable(oldExecutableFeature);
		}
	}

	private boolean isOldExecutableFeature(BuildTimeFeature feature) {
		if (!feature.getId().equals(FEATURE_EQUINOX_EXECUTABLE))
			return false;

		if (feature.isBinary() || !OLD_EXECUTABLE_RANGE.isIncluded(new Version(feature.getVersion())))
			return false;

		Properties properties = getFeatureBuildProperties(feature);
		return properties != null && Boolean.valueOf((String) properties.get(PROPERTY_CUSTOM)).booleanValue();
	}

	private void generateCompatibilityGatherExecutable(BuildTimeFeature executableFeature) {
		IPath featureLocation = new Path(executableFeature.getRootLocation());
		String featureFullName = executableFeature.getId() + "_" + executableFeature.getVersion(); //$NON-NLS-1$

		File productDir = new File(getWorkingDirectory(), DEFAULT_FEATURE_LOCATION + '/' + CONTAINER_FEATURE + "/product"); //$NON-NLS-1$
		productDir.mkdirs();
		File overridesFile = new File(productDir, "overrides.properties"); //$NON-NLS-1$
		Properties overrides = Utils.getOldExecutableRootOverrides();
		OutputStream outputStream = null;
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(overridesFile));
			overrides.store(outputStream, "Overrides for org.eclipse.equinox.executable"); //$NON-NLS-1$
		} catch (IOException e) {
			//
		} finally {
			Utils.close(outputStream);
		}

		script.printTargetDeclaration(TARGET_P2_COMPATIBILITY_GATHER_EXECUTABLE, null, null, null, null);
		script.printTab();
		script.print("<eclipse.gatherFeature"); //$NON-NLS-1$
		script.printAttribute("metadataRepository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
		script.printAttribute("artifactRepository", Utils.getPropertyFormat(PROPERTY_P2_BUILD_REPO), true); //$NON-NLS-1$
		script.printAttribute("buildResultFolder", executableFeature.getRootLocation(), true); //$NON-NLS-1$
		script.printAttribute("baseDirectory", executableFeature.getRootLocation(), true); //$NON-NLS-1$
		script.printAttribute("overrides", overridesFile.getAbsolutePath(), true); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$

		Map params = new HashMap();
		params.put(PROPERTY_PROJECT_LOCATION, "${basedir}/" + Utils.makeRelative(featureLocation, new Path(workingDirectory)).toOSString()); //$NON-NLS-1$
		params.put(PROPERTY_FEATURE_BASE, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE));
		params.put(PROPERTY_PROJECT_NAME, featureFullName);
		params.put(PROPERTY_TARGET_FOLDER, Utils.getPropertyFormat(PROPERTY_ECLIPSE_BASE) + '/' + DEFAULT_FEATURE_LOCATION);
		printCustomAssemblyAntCall(TARGET_GATHER_BIN_PARTS, params);
		script.printTargetEnd();
		script.println();
	}

	public void setVersionsList(boolean versionsList) {
		this.versionsList = versionsList;
	}
}
